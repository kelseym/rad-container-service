package org.nrg.containers.events;

import com.google.common.collect.Lists;
import com.spotify.docker.client.exceptions.ServiceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.config.ContainersConfig;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoDockerServerException;
import org.nrg.containers.model.container.auto.Container;
import org.nrg.containers.model.server.docker.DockerServerBase.DockerServer;
import org.nrg.containers.services.ContainerService;
import org.nrg.containers.services.DockerServerService;
import org.nrg.containers.utils.ContainerUtils;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.framework.task.XnatTask;
import org.nrg.framework.task.services.XnatTaskService;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.schema.XFTManager;
import org.nrg.xnat.services.XnatAppInfo;
import org.nrg.xnat.task.AbstractXnatTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.nrg.containers.services.impl.ContainerServiceImpl;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class DockerStatusUpdater   implements Runnable {

    private ContainerControlApi controlApi;
    private DockerServerService dockerServerService;
    private ContainerService containerService;
    final XnatAppInfo xnatAppInfo;
    
    private boolean haveLoggedDockerConnectFailure = false;
    private boolean haveLoggedNoServerInDb = false;
    private boolean haveLoggedXftInitFailure = false;

    @Autowired
    @SuppressWarnings("SpringJavaAutowiringInspection")
    public DockerStatusUpdater(final ContainerControlApi controlApi,
                               final DockerServerService dockerServerService,
                               final ContainerService containerService,
                               final XnatAppInfo xnatAppInfo) {
        this.controlApi = controlApi;
        this.dockerServerService = dockerServerService;
        this.containerService = containerService;
        this.xnatAppInfo=xnatAppInfo;
    }

    @Override
    public void run() {
		if(!xnatAppInfo.isPrimaryNode()) {
			log.trace("Not the Primary node: skipping update status with docker.");
	        return;
    	}
        log.trace("Primary node: Attempting to update status with docker.");

        final String skipMessage = "Skipping attempt to update status.";

        if (!XFTManager.isInitialized()) {
            if (!haveLoggedXftInitFailure) {
                log.info("XFT is not initialized. " + skipMessage);
                haveLoggedXftInitFailure = true;
            }
            return;
        }

        // Since XFT is up, we should be able to connect to the database and read the docker server
        DockerServer dockerServer = null;
        try {
            dockerServer = dockerServerService.getServer();
        } catch (NotFoundException e) {
            // ignored
 			log.error("Docker server not found");
        }
        if (dockerServer == null) {
            if (!haveLoggedNoServerInDb) {
                log.info("No docker server has been defined (or enabled) in the database. " + skipMessage);
                haveLoggedNoServerInDb = true;
                haveLoggedXftInitFailure = false;
            }
            return;
        }

        if (!controlApi.canConnect()) {
            if (!haveLoggedDockerConnectFailure) {
                log.info("Cannot ping docker server " + dockerServer.name() + ". " + skipMessage);
                haveLoggedDockerConnectFailure = true;
                haveLoggedXftInitFailure = false;
                haveLoggedNoServerInDb = false;
            }
            return;
        }

        // Now we should be able to check the status
        final UpdateReport updateReport = dockerServer.swarmMode() ? updateServices(dockerServer) : updateContainers(dockerServer);
        if (updateReport.successful == null) {
            // This means some, but not all, of the services didn't update properly. Which ones?
            for (final UpdateReportEntry entry : updateReport.updateReports) {
                if (!entry.successful) {
                    log.error("Could not update status for {}. Message: {}", entry.id, entry.message);
                } else {
                    log.debug("Updated successfully for {}.", entry.id);
                }
            }

            // Reset failure flags
            haveLoggedDockerConnectFailure = false;
            haveLoggedXftInitFailure = false;
            haveLoggedNoServerInDb = false;
        } else if (updateReport.successful) {
            if (updateReport.updateReports.size() > 0) {
                log.debug("Updated status successfully.");
            }
            // Reset failure flags
            haveLoggedDockerConnectFailure = false;
            haveLoggedXftInitFailure = false;
            haveLoggedNoServerInDb = false;
        } else {
            log.info("Did not update status successfully.");
        }
    	log.trace("-----------------------------------------------------------------------------");
        log.trace("DOCKERSTATUSUPDATER: RUN COMPLETE");
    	log.trace("-----------------------------------------------------------------------------");
   
    }

    @Nonnull
    private UpdateReport updateContainers(final DockerServer dockerServer) {
        final Date lastEventCheckTime = dockerServer.lastEventCheckTime();
        final Date since = lastEventCheckTime == null ? new Date(0L) : lastEventCheckTime;

        final Date now = new Date();

        try {
            controlApi.throwContainerEvents(since, now);
            dockerServerService.update(dockerServer.updateEventCheckTime(now));

            return UpdateReport.singleton(UpdateReportEntry.success());
        } catch (NoDockerServerException e) {
            log.info("Cannot search for Docker container events. No Docker server defined.");
        } catch (DockerServerException e) {
            log.error("Cannot find Docker container events.", e);
        }
        return UpdateReport.singleton(UpdateReportEntry.failure());
    }

    @Nonnull
    private UpdateReport updateServices(final DockerServer dockerServer) {
        final UpdateReport report = UpdateReport.create();
        //TODO : Optimize this code so that waiting ones are handled first
        for (final Container service : containerService.retrieveNonfinalizedServices()) {
            log.debug("Getting Task info for Service {}.", service.serviceId());
            log.debug("DIAGNOSTICS:"+service.toString());
            try {
                controlApi.throwTaskEventForService(dockerServer, service);
                report.add(UpdateReportEntry.success(service.serviceId()));
            } catch (ServiceNotFoundException e) {
            	if(isFinalizing(service)){
            		log.debug("ignoring failed service retrieval for finalizing and waiting jobs");
            		continue;
            	}
            	
            	if (isWaiting(service)){
            		containerService.queuedFinalize("0",true, service, AdminUtils.getAdminUser());
            		continue;
            	}
            	
                final String msg = String.format("Service %s is in database, but not found on swarm. Setting status to \"Failed\".", service.serviceId());
                log.error(msg, e);
                report.add(UpdateReportEntry.failure(service.serviceId(), msg));

                final Container.ContainerHistory failedHistoryItem = Container.ContainerHistory.fromSystem("Failed", "Not found on swarm.");
                final Container notFound = service.toBuilder()
                        .status(failedHistoryItem.status())
                        .statusTime(failedHistoryItem.timeRecorded())
                        .build();

                containerService.update(notFound);

                ContainerUtils.updateWorkflowStatus(notFound.workflowId(), PersistentWorkflowUtils.FAILED, AdminUtils.getAdminUser());
            } catch (DockerServerException e) {
                log.error(String.format("Cannot get Tasks for Service %s.", service.serviceId()), e);
                report.add(UpdateReportEntry.failure(service.serviceId(), e.getMessage()));
            }
        }

        boolean allTrue = true;
        boolean allFalse = true;
        for (final UpdateReportEntry entry : report.updateReports) {
            allTrue = allTrue && entry.successful;
            allFalse = allFalse && !entry.successful;
        }

        // If either allTrue or allFalse is true, then allTrue will tell us if everything was successful or not.
        // If both allTrue and allFalse are false,, that means some were successful and some not. So we set overall report success to null.
        report.successful = allTrue || allFalse ? allTrue : null;

        return report;
    }
   
    public boolean isWaiting(Container service){
    	return ContainerServiceImpl.waiting.equals(service.status());
    }
    
    public boolean isFinalizing(Container service){
    	return ContainerServiceImpl.finalizing.equals(service.status());
    }

    private static class UpdateReport {
        private Boolean successful;
        private List<UpdateReportEntry> updateReports;

        private UpdateReport() {}

        public static UpdateReport create() {
            final UpdateReport report = new UpdateReport();
            report.updateReports = Lists.newArrayList();
            return report;
        }

        public static UpdateReport singleton(final UpdateReportEntry entry) {
            final UpdateReport report = new UpdateReport();
            report.updateReports = Collections.singletonList(entry);
            report.successful = entry.successful;
            return report;
        }

        public void add(final UpdateReportEntry entry) {
            updateReports.add(entry);
        }
    }

    private static class UpdateReportEntry {
        private Boolean successful;
        private String id;
        private String message;

        public static UpdateReportEntry success() {
            final UpdateReportEntry updateReportEntry = new UpdateReportEntry();
            updateReportEntry.successful = true;
            return updateReportEntry;
        }

        public static UpdateReportEntry success(final String id) {
            final UpdateReportEntry updateReportEntry = success();
            updateReportEntry.id = id;
            return updateReportEntry;
        }

        public static UpdateReportEntry failure() {
            final UpdateReportEntry updateReportEntry = new UpdateReportEntry();
            updateReportEntry.successful = false;
            return updateReportEntry;
        }

        public static UpdateReportEntry failure(final String id,
                                                final String message) {
            final UpdateReportEntry updateReportEntry = failure();
            updateReportEntry.id = id;
            updateReportEntry.message = message;
            return updateReportEntry;
        }
    }

	
}
