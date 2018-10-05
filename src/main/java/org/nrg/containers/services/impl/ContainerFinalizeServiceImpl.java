package org.nrg.containers.services.impl;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.nrg.action.ClientException;
import org.nrg.action.ServerException;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.exceptions.ContainerException;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoDockerServerException;
import org.nrg.containers.exceptions.UnauthorizedException;
import org.nrg.containers.model.container.auto.Container;
import org.nrg.containers.model.container.auto.Container.ContainerMount;
import org.nrg.containers.model.container.auto.Container.ContainerOutput;
import org.nrg.containers.services.ContainerFinalizeService;
import org.nrg.containers.services.ContainerService;
import org.nrg.containers.utils.ContainerUtils;
import org.nrg.transporter.TransportService;
import org.nrg.xdat.om.XnatResourcecatalog;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.helpers.Permissions;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.helpers.uri.UriParserUtils;
import org.nrg.xnat.restlet.util.XNATRestConstants;
import org.nrg.xnat.services.archive.CatalogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.method.P;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.nrg.containers.model.command.entity.CommandWrapperOutputEntity.Type.ASSESSOR;
import static org.nrg.containers.model.command.entity.CommandWrapperOutputEntity.Type.RESOURCE;

@Slf4j
@Service
public class ContainerFinalizeServiceImpl implements ContainerFinalizeService {

    private final ContainerControlApi containerControlApi;
    private final SiteConfigPreferences siteConfigPreferences;
    private final TransportService transportService;
    private final CatalogService catalogService;

    @Autowired
    public ContainerFinalizeServiceImpl(final ContainerControlApi containerControlApi,
                                        final SiteConfigPreferences siteConfigPreferences,
                                        final TransportService transportService,
                                        final CatalogService catalogService) {
        this.containerControlApi = containerControlApi;
        this.siteConfigPreferences = siteConfigPreferences;
        this.transportService = transportService;
        this.catalogService = catalogService;
    }

    @Override
    public Container finalizeContainer(final Container toFinalize, final UserI userI, final boolean isFailed, final List<Container> wrapupContainers) {
        final ContainerFinalizeHelper helper =
                new ContainerFinalizeHelper(toFinalize, userI, isFailed, wrapupContainers);
        return helper.finalizeContainer();
    }

    private class ContainerFinalizeHelper {

        private Container toFinalize;
        private UserI userI;
        // private String exitCode;
        private boolean isFailed;

        private Map<String, ContainerMount> untransportedMounts;
        private Map<String, ContainerMount> transportedMounts;

        private String prefix;

        private Map<String, Container> wrapupContainerMap;

        private Map<String, String> wrapperInputAndOutputValues;

        private ContainerFinalizeHelper(final Container toFinalize,
                                        final UserI userI,
                                        final boolean isFailed,
                                        final List<Container> wrapupContainers) {
            this.toFinalize = toFinalize;
            this.userI = userI;
            this.isFailed = isFailed;

            untransportedMounts = Maps.newHashMap();
            transportedMounts = Maps.newHashMap();

            prefix = "Container " + toFinalize.databaseId() + ": ";

            if (wrapupContainers == null || wrapupContainers.size() == 0) {
                wrapupContainerMap = Collections.emptyMap();
            } else {
                wrapupContainerMap = new HashMap<>();
                for (final Container wrapupContainer : wrapupContainers) {
                    wrapupContainerMap.put(wrapupContainer.parentSourceObjectName(), wrapupContainer);
                }
            }

            // Pre-populate the map of wrapper input and output values with the inputs.
            // Output URI values will be added as we create them here.
            wrapperInputAndOutputValues = new HashMap<>(toFinalize.getWrapperInputs());
        }

        private Container finalizeContainer() {
            final Container.Builder finalizedContainerBuilder = toFinalize.toBuilder();
            finalizedContainerBuilder.logPaths(uploadLogs());

            if (!isFailed) {
                // Do not try to upload outputs if we know the container failed.
                for (final ContainerMount mountOut : toFinalize.mounts()) {
                    untransportedMounts.put(mountOut.name(), mountOut);
                }

                final OutputsAndExceptions outputsAndExceptions = uploadOutputs();
                final List<Exception> failedRequiredOutputs = outputsAndExceptions.exceptions;
                if (!failedRequiredOutputs.isEmpty()) {
                    final Container.ContainerHistory failedHistoryItem = Container.ContainerHistory.fromSystem("Failed",
                            "Failed to upload required outputs.\n" + Joiner.on("\n").join(Lists.transform(failedRequiredOutputs, new Function<Exception, String>() {
                                @Override
                                public String apply(final Exception input) {
                                    return input.getMessage();
                                }
                            })));
                    finalizedContainerBuilder.addHistoryItem(failedHistoryItem)
                            .outputs(outputsAndExceptions.outputs)
                            .status(failedHistoryItem.status())
                            .statusTime(failedHistoryItem.timeRecorded());
                } else {
                    finalizedContainerBuilder.outputs(outputsAndExceptions.outputs)  // Overwrite any existing outputs
                            .status("Complete")
                            .statusTime(new Date());
                }

                ContainerUtils.updateWorkflowStatus(toFinalize.workflowId(), PersistentWorkflowUtils.COMPLETE, userI);
            } else {
                // TODO We know the container has failed. Should we send an email?
                ContainerUtils.updateWorkflowStatus(toFinalize.workflowId(), PersistentWorkflowUtils.FAILED, userI);
                finalizedContainerBuilder.status("Failed")
                        .addHistoryItem(Container.ContainerHistory.fromSystem("Failed", ""))
                        .statusTime(new Date());
            }

            return finalizedContainerBuilder.build();
        }

        private List<String> uploadLogs() {
            log.info(prefix + "Getting logs.");
            final List<String> logPaths = Lists.newArrayList();

            final String stdoutLogStr = getStdoutLogStr();
            final String stderrLogStr = getStderrLogStr();

            if (StringUtils.isNotBlank(stdoutLogStr) || StringUtils.isNotBlank(stderrLogStr)) {

                final String archivePath = siteConfigPreferences.getArchivePath(); // TODO find a place to upload this thing. Root of the archive if sitewide, else under the archive path of the root object
                if (StringUtils.isNotBlank(archivePath)) {
                    final SimpleDateFormat formatter = new SimpleDateFormat(XNATRestConstants.PREARCHIVE_TIMESTAMP);
                    final String datestamp = formatter.format(new Date());
                    final String containerExecPath = FileUtils.AppendRootPath(archivePath, "CONTAINER_EXEC/");
                    final String destinationPath = containerExecPath + datestamp + "/LOGS/";
                    final File destination = new File(destinationPath);
                    destination.mkdirs();

                    log.info(prefix + "Saving logs to " + destinationPath);

                    if (StringUtils.isNotBlank(stdoutLogStr)) {
                        log.debug("Saving stdout");
                        final File stdoutFile = new File(destination, ContainerService.STDOUT_LOG_NAME);
                        FileUtils.OutputToFile(stdoutLogStr, stdoutFile.getAbsolutePath());
                        logPaths.add(stdoutFile.getAbsolutePath());
                    } else {
                        log.debug("Stdout was blank");
                    }

                    if (StringUtils.isNotBlank(stderrLogStr)) {
                        log.debug("Saving stderr");
                        final File stderrFile = new File(destination, ContainerService.STDERR_LOG_NAME);
                        FileUtils.OutputToFile(stderrLogStr, stderrFile.getAbsolutePath());
                        logPaths.add(stderrFile.getAbsolutePath());
                    } else {
                        log.debug("Stderr was blank");
                    }
                }
            }

            if (log.isDebugEnabled()) {
                log.debug("Adding log paths to container");
            }
            return logPaths;
        }

        private String getStderrLogStr() {
            try {
                return containerControlApi.getStderrLog(toFinalize);
            } catch (DockerServerException | NoDockerServerException e) {
                log.error(prefix + "Could not get stderr log.", e);
            }
            return null;
        }

        private String getStdoutLogStr() {
            try {
                return containerControlApi.getStdoutLog(toFinalize);
            } catch (DockerServerException | NoDockerServerException e) {
                log.error(prefix + "Could not get stdout log.", e);
            }
            return null;
        }

        private OutputsAndExceptions uploadOutputs() {
            log.info(prefix + "Uploading outputs.");

            final List<ContainerOutput> outputs = Lists.newArrayList();
            final List<Exception> exceptions = Lists.newArrayList();
            for (final ContainerOutput nonUploadedOuput: toFinalize.outputs()) {
                try {
                    outputs.add(uploadOutput(nonUploadedOuput));
                } catch (UnauthorizedException | ContainerException | RuntimeException e) {
                    log.error("Cannot upload files for command output " + nonUploadedOuput.name(), e);
                    if (nonUploadedOuput.required()) {
                        exceptions.add(e);
                    }
                    outputs.add(nonUploadedOuput);
                }
            }

            log.info(prefix + "Done uploading outputs.");
            return new OutputsAndExceptions(outputs, exceptions);
        }

        private ContainerOutput uploadOutput(final ContainerOutput output) throws ContainerException, UnauthorizedException {
            log.info(prefix + "Uploading output \"{}\".", output.name());
            log.debug("{}", output);

            final String mountXnatHostPath;
            final String viaWrapupContainer = output.viaWrapupContainer();
            if (StringUtils.isBlank(viaWrapupContainer)) {
                final String mountName = output.mount();
                final ContainerMount mount = getMount(mountName);
                if (mount == null) {
                    throw new ContainerException(String.format(prefix + "Mount \"%s\" does not exist.", mountName));
                }

                log.debug(prefix + "Output files are provided by mount \"{}\": {}", mountName, mount);
                mountXnatHostPath = mount.xnatHostPath();
            } else {
                log.debug(prefix + "Output files are provided by wrapup container \"{}\".", viaWrapupContainer);
                final Container wrapupContainer = getWrapupContainer(output.name());
                if (wrapupContainer == null) {
                    throw new ContainerException(prefix + "Container output \"" + output.name() + "\" " +
                            "must be processed via wrapup container \"" + viaWrapupContainer + "\" which was not found.");
                }

                ContainerMount wrapupOutputMount = null;
                for (final ContainerMount mount : wrapupContainer.mounts()) {
                    if (mount.name().equals("output")) {
                        wrapupOutputMount = mount;
                    }
                }
                if (wrapupOutputMount == null) {
                    throw new ContainerException(prefix + "Container output \"" + output.name() + "\" " +
                            "was processed via wrapup container \"" + wrapupContainer.databaseId() + "\" which has no output mount.");
                }

                mountXnatHostPath = wrapupOutputMount.xnatHostPath();
            }

            if (StringUtils.isBlank(mountXnatHostPath)) {
                throw new ContainerException(String.format(prefix + "Cannot upload output \"%s\". The path to the files on the XNAT machine is blank.", output.name()));
            }

            final String relativeFilePath = output.path() != null ? output.path() : "";
            final String filePath = StringUtils.isBlank(relativeFilePath) ? mountXnatHostPath :
                    FilenameUtils.concat(mountXnatHostPath, relativeFilePath);
            final String globMatcher = output.glob() != null ? output.glob() : "";

            final List<File> toUpload = matchGlob(filePath, globMatcher);
            if (toUpload == null || toUpload.size() == 0) {
                if (output.required()) {
                    throw new ContainerException(String.format(prefix + "Nothing to upload for output \"%s\".", output.name()));
                }
                return output;
            }

            final String label = StringUtils.isNotBlank(output.label()) ? output.label() : output.name();

            String parentUri = getWrapperInputValue(output.handledBy());
            if (parentUri == null) {
                throw new ContainerException(String.format(prefix + "Cannot upload output \"%s\". Could not instantiate object from input \"%s\".", output.name(), output.handledBy()));
            }
            if (!parentUri.startsWith("/archive")) {
                parentUri = "/archive" + parentUri;
            }

            String createdUri = null;
            final String type = output.type();
            if (type.equals(RESOURCE.getName())) {
                if (log.isDebugEnabled()) {
                    final String template = prefix + "Inserting file resource.\n\tuser: {}\n\tparentUri: {}\n\tlabel: {}\n\ttoUpload: {}";
                    log.debug(template, userI.getLogin(), parentUri, label, toUpload);
                }

                try {
                    final URIManager.DataURIA uri = UriParserUtils.parseURI(parentUri);
                    if (!Permissions.canEdit(userI, ((URIManager.ArchiveItemURI) uri).getSecurityItem())) {
                        final String message = String.format(prefix + "User does not have permission to add resources to item with URI %s.", parentUri);
                        log.error(message);
                        throw new UnauthorizedException(message);
                    }

                    final XnatResourcecatalog resourcecatalog = catalogService.insertResources(userI, parentUri, toUpload, label, null, output.format(), null);
                    createdUri = UriParserUtils.getArchiveUri(resourcecatalog);
                    if (StringUtils.isBlank(createdUri)) {
                        createdUri = parentUri + "/resources/" + resourcecatalog.getLabel();
                    }
                } catch (ClientException e) {
                    final String message = String.format(prefix + "User does not have permission to add resources to item with URI %s.", parentUri);
                    log.error(message);
                    throw new UnauthorizedException(message);
                } catch (Exception e) {
                    throw new ContainerException(prefix + "Could not upload files to resource.", e);
                }

                try {
                    catalogService.refreshResourceCatalog(userI, createdUri);
                } catch (ServerException | ClientException e) {
                    final String message = String.format(prefix + "Could not refresh catalog for resource %s.", createdUri);
                    log.error(message, e);
                }
            } else if (type.equals(ASSESSOR.getName())) {
                /* TODO Waiting on XNAT-4556
                final CommandMount mount = getMount(output.getFiles().getMount());
                final String absoluteFilePath = FilenameUtils.concat(mount.getHostPath(), output.getFiles().getPath());
                final SAXReader reader = new SAXReader(userI);
                XFTItem item = null;
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("Reading XML file at " + absoluteFilePath);
                    }
                    item = reader.parse(new File(absoluteFilePath));

                } catch (IOException e) {
                    log.error("An error occurred reading the XML", e);
                } catch (SAXException e) {
                    log.error("An error occurred parsing the XML", e);
                }

                if (!reader.assertValid()) {
                    throw new ContainerException("XML file invalid", reader.getErrors().get(0));
                }
                if (item == null) {
                    throw new ContainerException("Could not create assessor from XML");
                }

                try {
                    if (item.instanceOf("xnat:imageAssessorData")) {
                        final XnatImageassessordata assessor = (XnatImageassessordata) BaseElement.GetGeneratedItem(item);
                        if(permissionsService.canCreate(userI, assessor)){
                            throw new ContainerException(String.format("User \"%s\" has insufficient privileges for assessors in project \"%s\".", userI.getLogin(), assessor.getProject()));
                        }

                        if(assessor.getLabel()==null){
                            assessor.setLabel(assessor.getId());
                        }

                        // I hate this
                    }
                } catch (ElementNotFoundException e) {
                    throw new ContainerException(e);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                 */
            }


            log.info(prefix + "Done uploading output \"{}\". URI of created output: {}", output.name(), createdUri);

            // We use the "fromOutputHandler" property here rather than name. The reason is that we will be looking
            // up the value later based on what users set in subsequent handers' "handled-by" properties, and the value
            // they put in that property is going to be the output handler name.
            wrapperInputAndOutputValues.put(output.fromOutputHandler(), createdUri);
            
            return output.toBuilder().created(createdUri).build();
        }

        private ContainerMount getMount(final String mountName) throws ContainerException {
            // If mount has been transported, we're done
            if (transportedMounts.containsKey(mountName)) {
                return transportedMounts.get(mountName);
            }

            // If mount exists but has not been transported, transport it
            if (untransportedMounts.containsKey(mountName)) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format(prefix + "Transporting mount \"%s\".", mountName));
                }
                ContainerMount mountToTransport = untransportedMounts.get(mountName);

                if (StringUtils.isBlank(mountToTransport.xnatHostPath())) {
                    final Path pathOnExecutionMachine = Paths.get(mountToTransport.containerHostPath());
                    final Path pathOnXnatMachine = transportService.transport("", pathOnExecutionMachine); // TODO this currently does nothing
                    mountToTransport = mountToTransport.toBuilder().xnatHostPath(pathOnXnatMachine.toAbsolutePath().toString()).build();
                } else {
                    // TODO add transporter method to transport from specified source path to specified destination path
                    // transporter.transport(sourceMachineName, mountToTransport.getContainerHostPath(), mountToTransport.getXnatHostPath());
                }

                transportedMounts.put(mountName, mountToTransport);
                untransportedMounts.remove(mountName);

                log.debug(prefix + "Done transporting mount.");
                return mountToTransport;
            }

            // Mount does not exist
            throw new ContainerException(String.format(prefix + "Mount \"%s\" does not exist.", mountName));
        }

        private String getWrapperInputValue(final String inputName) {
            if (log.isDebugEnabled()) {
                log.debug(String.format(prefix + "Getting URI for input \"%s\".", inputName));
            }

            if (wrapperInputAndOutputValues.containsKey(inputName)) {
                return wrapperInputAndOutputValues.get(inputName);
            }

            if (log.isDebugEnabled()) {
                log.debug(String.format(prefix + "No input or output found with name \"%s\".", inputName));
            }
            return null;
        }

        @Nullable
        private Container getWrapupContainer(final String parentSourceObjectName) {
            return wrapupContainerMap.get(parentSourceObjectName);
        }

        private List<File> matchGlob(final String rootPath, final String glob) {
            final File rootDir = new File(rootPath);
            final File[] files = rootDir.listFiles();
            return files == null ? Lists.<File>newArrayList() : Arrays.asList(files);
        }
    }

    private static class OutputsAndExceptions {
        List<ContainerOutput> outputs;
        List<Exception> exceptions;

        OutputsAndExceptions(final List<ContainerOutput> outputs,
                             final List<Exception> exceptions) {
            this.outputs = outputs;
            this.exceptions = exceptions;
        }
    }
}
