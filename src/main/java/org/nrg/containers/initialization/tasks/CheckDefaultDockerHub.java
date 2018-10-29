package org.nrg.containers.initialization.tasks;

import lombok.extern.slf4j.Slf4j;
import org.nrg.containers.model.dockerhub.DockerHubBase;
import org.nrg.containers.services.DockerHubService;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xnat.initialization.tasks.AbstractInitializingTask;
import org.nrg.xnat.initialization.tasks.InitializingTaskException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class CheckDefaultDockerHub extends AbstractInitializingTask {
    private final DockerHubService dockerHubService;
    private final SiteConfigPreferences siteConfigPreferences;

    @Autowired
    public CheckDefaultDockerHub(final DockerHubService dockerHubService,
                                 final SiteConfigPreferences siteConfigPreferences) {
        this.dockerHubService = dockerHubService;
        this.siteConfigPreferences = siteConfigPreferences;
    }

    @Override
    public String getTaskName() {
        return "Check that a default docker hub is stored";
    }

    @Override
    protected void callImpl() throws InitializingTaskException {

        log.debug("Checking if any docker hubs exist in database.");
        final List<DockerHubBase.DockerHub> hubs = dockerHubService.getHubs();
        final boolean anyHubsInDb = !(hubs == null || hubs.isEmpty());
        final boolean oneHubInDb = (anyHubsInDb && hubs.size() == 1); // This is just to make the debug message nicer
        log.debug("{} hub{} exist{} in db.", anyHubsInDb ? hubs.size() : "No", oneHubInDb ? "" : "s", oneHubInDb ? "s" : "");
        if (!anyHubsInDb) {
            log.info("Initializing database with public docker hub.");
            setDefault(dockerHubService.create(DockerHubBase.DockerHub.DEFAULT));
            log.debug("All is well.");
            return;
        }

        log.debug("Checking if any hub is marked \"default\".");
        final long defaultDockerHubId = dockerHubService.getDefaultHubId();
        final boolean defaultDefined = !(defaultDockerHubId == 0L);
        if (defaultDefined) {
            DockerHubBase.DockerHub existingDefault = null;
            for (final DockerHubBase.DockerHub dockerHub : hubs) {
                if (dockerHub.id() == defaultDockerHubId) {
                    existingDefault = dockerHub;
                    break;
                }
            }

            if (existingDefault != null) {
                log.debug("Hub {}: \"{}\" is marked \"default\".", defaultDockerHubId, existingDefault.name());
            } else {
                log.debug("Hub {} is marked \"default\", but no hub exists with that id.", defaultDockerHubId);
                setFirstHubAsDefault(hubs);
            }
        } else {
            log.debug("No hub is marked \"default\".");
            setFirstHubAsDefault(hubs);
        }
    }

    private void setDefault(final DockerHubBase.DockerHub dockerHub) {
        log.info("Setting docker hub {}: \"{}\" as \"default\".", dockerHub.id(), dockerHub.name());
        dockerHubService.setDefault(dockerHub, siteConfigPreferences.getPrimaryAdminUsername(), "Setting default docker hub during site initialization.");
    }

    private void setFirstHubAsDefault(final List<DockerHubBase.DockerHub> hubs) {
        log.info("Selecting first hub in database to set as \"default\".");
        setDefault(hubs.get(0));
    }
}
