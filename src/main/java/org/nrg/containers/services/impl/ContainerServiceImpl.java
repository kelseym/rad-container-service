package org.nrg.containers.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.events.model.ContainerEvent;
import org.nrg.containers.exceptions.CommandResolutionException;
import org.nrg.containers.exceptions.ContainerException;
import org.nrg.containers.exceptions.ContainerMountResolutionException;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.helpers.ContainerFinalizeHelper;
import org.nrg.containers.model.command.auto.Command;
import org.nrg.containers.model.command.auto.Command.CommandWrapper;
import org.nrg.containers.model.command.auto.ResolvedCommand;
import org.nrg.containers.model.command.auto.ResolvedCommand.PartiallyResolvedCommand;
import org.nrg.containers.model.command.auto.ResolvedCommand.PartiallyResolvedCommandMount;
import org.nrg.containers.model.command.auto.ResolvedCommand.ResolvedCommandMount;
import org.nrg.containers.model.command.auto.ResolvedCommand.ResolvedCommandMountFiles;
import org.nrg.containers.model.container.entity.ContainerEntity;
import org.nrg.containers.model.container.entity.ContainerEntityHistory;
import org.nrg.containers.services.CommandResolutionService;
import org.nrg.containers.services.CommandService;
import org.nrg.containers.services.ContainerEntityService;
import org.nrg.containers.services.ContainerService;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.transporter.TransportService;
import org.nrg.xdat.entities.AliasToken;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.helpers.Permissions;
import org.nrg.xdat.security.helpers.Users;
import org.nrg.xdat.security.services.PermissionsServiceI;
import org.nrg.xdat.security.user.exceptions.UserInitException;
import org.nrg.xdat.security.user.exceptions.UserNotFoundException;
import org.nrg.xdat.services.AliasTokenService;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.services.archive.CatalogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.nrg.containers.model.command.entity.CommandType.DOCKER;

@Service
public class ContainerServiceImpl implements ContainerService {
    private static final Logger log = LoggerFactory.getLogger(ContainerServiceImpl.class);
    private static final Pattern exitCodePattern = Pattern.compile("kill|die|oom\\((\\d+|x)\\)");

    private final CommandService commandService;
    private final ContainerControlApi containerControlApi;
    private final ContainerEntityService containerEntityService;
    private final CommandResolutionService commandResolutionService;
    private final AliasTokenService aliasTokenService;
    private final SiteConfigPreferences siteConfigPreferences;
    private final TransportService transportService;
    private PermissionsServiceI permissionsService;
    private final CatalogService catalogService;
    private final ObjectMapper mapper;

    @Autowired
    public ContainerServiceImpl(final CommandService commandService,
                                final ContainerControlApi containerControlApi,
                                final ContainerEntityService containerEntityService,
                                final CommandResolutionService commandResolutionService,
                                final AliasTokenService aliasTokenService,
                                final SiteConfigPreferences siteConfigPreferences,
                                final TransportService transportService,
                                final CatalogService catalogService,
                                final ObjectMapper mapper) {
        this.commandService = commandService;
        this.containerControlApi = containerControlApi;
        this.containerEntityService = containerEntityService;
        this.commandResolutionService = commandResolutionService;
        this.aliasTokenService = aliasTokenService;
        this.siteConfigPreferences = siteConfigPreferences;
        this.transportService = transportService;
        this.permissionsService = null; // Will be initialized later.
        this.catalogService = catalogService;
        this.mapper = mapper;
    }

    @Override
    public PartiallyResolvedCommand partiallyResolveCommand(final long commandId, final long wrapperId, final Map<String, String> rawParamValues) {
        return null;
    }

    @Override
    public PartiallyResolvedCommand partiallyResolveCommand(final long commandId, final String wrapperName, final Map<String, String> rawParamValues) {
        return null;
    }

    @Override
    public PartiallyResolvedCommand partiallyResolveCommand(final String project, final long commandId, final long wrapperId, final Map<String, String> rawParamValues) {
        return null;
    }

    @Override
    public PartiallyResolvedCommand partiallyResolveCommand(final String project, final long commandId, final String wrapperName, final Map<String, String> rawParamValues) {
        return null;
    }

    @Override
    @Nonnull
    public PartiallyResolvedCommand resolveCommand(final long commandId,
                                                   final Map<String, String> runtimeInputValues,
                                                   final UserI userI)
            throws NotFoundException, CommandResolutionException {
        final Command command = commandService.get(commandId);
        return resolveCommand(command, runtimeInputValues, userI);

    }

    @Override
    @Nonnull
    public PartiallyResolvedCommand resolveCommand(final String xnatCommandWrapperName,
                                                   final long commandId,
                                                   final Map<String, String> runtimeInputValues,
                                                   final UserI userI)
            throws NotFoundException, CommandResolutionException {
        if (StringUtils.isBlank(xnatCommandWrapperName)) {
            return resolveCommand(commandId, runtimeInputValues, userI);
        }

        // The command that gets returned from getAndConfigure only has one wrapper
        final Command command = commandService.getAndConfigure(commandId, xnatCommandWrapperName);
        final CommandWrapper wrapper = command.xnatCommandWrappers().get(0);

        return resolveCommand(wrapper, command, runtimeInputValues, userI);
    }

    @Override
    @Nonnull
    public PartiallyResolvedCommand resolveCommand(final long xnatCommandWrapperId,
                                                   final long commandId,
                                                   final Map<String, String> runtimeInputValues,
                                                   final UserI userI)
            throws NotFoundException, CommandResolutionException {
        // The command that gets returned from getAndConfigure only has one wrapper
        final Command command = commandService.getAndConfigure(xnatCommandWrapperId);
        final CommandWrapper wrapper = command.xnatCommandWrappers().get(0);

        return resolveCommand(wrapper, command, runtimeInputValues, userI);
    }

    @Override
    @Nonnull
    public PartiallyResolvedCommand resolveCommand(final Command command,
                                                   final Map<String, String> runtimeInputValues,
                                                   final UserI userI)
            throws NotFoundException, CommandResolutionException {
        // I was not given a wrapper.
        // TODO what should I do here? Should I...
        //  1. Use the "passthrough" wrapper, no matter what
        //  2. Use the "passthrough" wrapper only if the command has no outputs
        //  3. check if the command has any wrappers, and use one if it exists
        //  4. Something else
        //
        // I guess for now I'll do 2.

        if (!command.outputs().isEmpty()) {
            throw new CommandResolutionException("Cannot resolve command without an XNAT wrapper. Command has outputs that will not be handled.");
        }

        final CommandWrapper commandWrapperToResolve = CommandWrapper.passthrough(command);

        return resolveCommand(commandWrapperToResolve, command, runtimeInputValues, userI);
    }

    @Override
    @Nonnull
    public PartiallyResolvedCommand resolveCommand(final CommandWrapper commandWrapper,
                                                   final Command command,
                                                   final Map<String, String> runtimeInputValues,
                                                   final UserI userI)
            throws NotFoundException, CommandResolutionException {
        return commandResolutionService.resolve(commandWrapper, command, runtimeInputValues, userI);
    }

    @Override
    public ContainerEntity resolveAndLaunchCommand(final long commandId,
                                                   final Map<String, String> runtimeValues,
                                                   final UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandResolutionException, ContainerException {
        return launchResolvedCommand(resolveCommand(commandId, runtimeValues, userI), userI);
    }

    @Override
    public ContainerEntity resolveAndLaunchCommand(final String xnatCommandWrapperName,
                                                   final long commandId,
                                                   final Map<String, String> runtimeValues,
                                                   final UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandResolutionException, ContainerException {
        return launchResolvedCommand(resolveCommand(xnatCommandWrapperName, commandId, runtimeValues, userI), userI);

    }

    @Override
    public ContainerEntity resolveAndLaunchCommand(final long xnatCommandWrapperId,
                                                   final long commandId,
                                                   final Map<String, String> runtimeValues,
                                                   final UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandResolutionException, ContainerException {
        return launchResolvedCommand(resolveCommand(xnatCommandWrapperId, commandId, runtimeValues, userI), userI);
    }

    @Nonnull
    private ContainerEntity launchResolvedCommand(final PartiallyResolvedCommand resolvedCommand,
                                                  final UserI userI)
            throws NoServerPrefException, DockerServerException, ContainerMountResolutionException, ContainerException, UnsupportedOperationException {
        if (resolvedCommand.type().equals(DOCKER.getName())) {
            return launchResolvedDockerCommand(resolvedCommand, userI);
        } else {
            throw new UnsupportedOperationException("Cannot launch a command of type " + resolvedCommand.type());
        }
    }

    @Override
    @Nonnull
    public ContainerEntity launchResolvedDockerCommand(final PartiallyResolvedCommand resolvedCommand,
                                                       final UserI userI)
            throws NoServerPrefException, DockerServerException, ContainerMountResolutionException, ContainerException {
        log.info("Preparing to launch resolved command.");
        final ResolvedCommand preparedToLaunch = prepareToLaunch(resolvedCommand, userI);

        log.info("Creating container from resolved command.");
        final String containerId = containerControlApi.createContainer(preparedToLaunch);

        log.info("Recording container launch.");
        final ContainerEntity containerEntity = containerEntityService.save(preparedToLaunch, containerId, userI);
        containerEntityService.addContainerHistoryItem(containerEntity, ContainerEntityHistory.fromUserAction("Created", userI.getLogin()));

        log.info("Starting container.");
        try {
            containerControlApi.startContainer(containerId);
        } catch (DockerServerException e) {
            containerEntityService.addContainerHistoryItem(containerEntity, ContainerEntityHistory.fromSystem("Did not start"));
            handleFailure(containerEntity);
            throw new ContainerException("Failed to start");
        }

        return containerEntity;
    }

    @Nonnull
    private ResolvedCommand prepareToLaunch(final PartiallyResolvedCommand partiallyResolvedCommand,
                                            final UserI userI)
            throws NoServerPrefException, ContainerMountResolutionException {
        final ResolvedCommand.Builder resolvedCommandBuilder = partiallyResolvedCommand.toResolvedCommandBuilder();

        // Add default environment variables
        final AliasToken token = aliasTokenService.issueTokenForUser(userI);
        final String processingUrl = (String)siteConfigPreferences.getProperty("processingUrl", siteConfigPreferences.getSiteUrl());
        resolvedCommandBuilder.addEnvironmentVariable("XNAT_USER", token.getAlias())
                .addEnvironmentVariable("XNAT_PASS", token.getSecret())
                .addEnvironmentVariable("XNAT_HOST", processingUrl);

        // Transport mounts
        if (!partiallyResolvedCommand.mounts().isEmpty()) {

            final String dockerHost = containerControlApi.getServer().host();
            for (final PartiallyResolvedCommandMount mount : partiallyResolvedCommand.mounts()) {
                final ResolvedCommandMount.Builder resolvedCommandMountBuilder = mount.toResolvedCommandMountBuilder();

                // First, figure out what we have.
                // Do we have source files? A source directory?
                // Can we mount a directory directly, or should we copy the contents to a build directory?
                final List<ResolvedCommandMountFiles> filesList = mount.inputFiles();
                final String buildDirectory;
                if (filesList != null && filesList.size() > 1) {
                    // We have multiple sources of files. We must copy them into one common location to mount.
                    buildDirectory = getBuildDirectory();
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Mount \"%s\" has multiple sources of files.", mount.name()));
                    }

                    // TODO figure out what to do with multiple sources of files
                    if (log.isDebugEnabled()) {
                        log.debug("TODO");
                    }
                } else if (filesList != null && filesList.size() == 1) {
                    // We have one source of files. We may need to copy, or may be able to mount directly.
                    final ResolvedCommandMountFiles files = filesList.get(0);
                    final String path = files.path();
                    final boolean hasPath = StringUtils.isNotBlank(path);

                    if (StringUtils.isNotBlank(files.rootDirectory())) {
                        // That source of files does have a directory set.

                        if (hasPath || mount.writable()) {
                            // In both of these conditions, we must copy some things to a build directory.
                            // Now we must find out what.
                            if (hasPath) {
                                // The source of files also has one or more paths set

                                buildDirectory = getBuildDirectory();
                                if (log.isDebugEnabled()) {
                                    log.debug(String.format("Mount \"%s\" has a root directory and a file. Copying the file from the root directory to build directory.", mount.name()));
                                }

                                // TODO copy the file in "path", relative to the root directory, to the build directory
                                if (log.isDebugEnabled()) {
                                    log.debug("TODO");
                                }
                            } else {
                                // The mount is set to "writable".
                                buildDirectory = getBuildDirectory();
                                if (log.isDebugEnabled()) {
                                    log.debug(String.format("Mount \"%s\" has a root directory, and is set to \"writable\". Copying all files from the root directory to build directory.", mount.name()));
                                }

                                // TODO We must copy all files out of the root directory to a build directory.
                                if (log.isDebugEnabled()) {
                                    log.debug("TODO");
                                }
                            }
                        } else {
                            // The source of files can be directly mounted
                            if (log.isDebugEnabled()) {
                                log.debug(String.format("Mount \"%s\" has a root directory, and is not set to \"writable\". The root directory can be mounted directly into the container.", mount.name()));
                            }
                            buildDirectory = files.rootDirectory();
                        }
                    } else if (hasPath) {
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("Mount \"%s\" has a file. Copying it to build directory.", mount.name()));
                        }
                        buildDirectory = getBuildDirectory();
                        // TODO copy the file to the build directory
                        if (log.isDebugEnabled()) {
                            log.debug("TODO");
                        }

                    } else {
                        final String message = String.format("Mount \"%s\" should have a file path or a directory or both but it does not.", mount.name());
                        log.error(message);
                        throw new ContainerMountResolutionException(message, mount);
                    }

                } else {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Mount \"%s\" has no input files. Ensuring mount is set to \"writable\" and creating new build directory.", mount.name()));
                    }
                    buildDirectory = getBuildDirectory();
                    if (!mount.writable()) {
                        resolvedCommandMountBuilder.writable(true);
                    }
                }

                if (log.isDebugEnabled()) {
                    log.debug(String.format("Setting mount \"%s\" xnat host path to \"%s\".", mount.name(), buildDirectory));
                }
                resolvedCommandMountBuilder.xnatHostPath(buildDirectory);

                if (log.isDebugEnabled()) {
                    log.debug(String.format("Transporting mount \"%s\".", mount.name()));
                }
                final Path pathOnDockerHost = transportService.transport(dockerHost, Paths.get(buildDirectory));

                if (log.isDebugEnabled()) {
                    log.debug(String.format("Setting mount \"%s\" container host path to \"%s\".", mount.name(), buildDirectory));
                }
                resolvedCommandMountBuilder.containerHostPath(pathOnDockerHost.toString());
                resolvedCommandBuilder.addMount(resolvedCommandMountBuilder.build());
            }
        }

        return resolvedCommandBuilder.build();
    }

    private String getBuildDirectory() {
        String buildPath = siteConfigPreferences.getBuildPath();
        final String uuid = UUID.randomUUID().toString();
        return FilenameUtils.concat(buildPath, uuid);
    }


    @Override
    @Transactional
    public void processEvent(final ContainerEvent event) {
        if (log.isDebugEnabled()) {
            log.debug("Processing container event");
        }
        final ContainerEntity execution = containerEntityService.addContainerEventToHistory(event);


        // execution will be null if either we aren't tracking the container
        // that this event is about, or if we have already recorded the event
        if (execution != null ) {

            final Matcher exitCodeMatcher =
                    exitCodePattern.matcher(event.getStatus());
            if (exitCodeMatcher.matches()) {
                log.debug("Container is dead. Finalizing.");
                final String exitCode = exitCodeMatcher.group(1);
                final String userLogin = execution.getUserId();
                try {
                    final UserI userI = Users.getUser(userLogin);
                    finalize(execution, userI, exitCode);
                } catch (UserInitException | UserNotFoundException e) {
                    log.error("Could not finalize container execution. Could not get user details for user " + userLogin, e);
                }

            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Done processing docker container event: " + event);
        }
    }

    @Override
    @Transactional
    public void finalize(final Long containerExecutionId, final UserI userI) {
        final ContainerEntity containerEntity = containerEntityService.retrieve(containerExecutionId);
        String exitCode = "x";
        for (final ContainerEntityHistory history : containerEntity.getHistory()) {
            final Matcher exitCodeMatcher = exitCodePattern.matcher(history.getStatus());
            if (exitCodeMatcher.matches()) {
                exitCode = exitCodeMatcher.group(1);
            }
        }
        finalize(containerEntity, userI, exitCode);
    }

    @Override
    @Transactional
    public void finalize(final ContainerEntity containerEntity, final UserI userI, final String exitCode) {
        if (log.isInfoEnabled()) {
            log.info(String.format("Finalizing ContainerExecution %s for container %s", containerEntity.getId(), containerEntity.getContainerId()));
        }

        ContainerFinalizeHelper.finalizeContainer(containerEntity, userI, exitCode, containerControlApi, siteConfigPreferences, transportService, getPermissionsService(), catalogService, mapper);

        if (log.isInfoEnabled()) {
            log.info(String.format("Done uploading for ContainerExecution %s. Now saving information about created outputs.", containerEntity.getId()));
        }
        containerEntityService.update(containerEntity);
        if (log.isDebugEnabled()) {
            log.debug("Done saving outputs for Container " + String.valueOf(containerEntity.getId()));
        }
    }

    @Override
    @Nonnull
    @Transactional
    public String kill(final Long containerExecutionId, final UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException {
        // TODO check user permissions. How?
        final ContainerEntity containerEntity = containerEntityService.get(containerExecutionId);

        containerEntityService.addContainerHistoryItem(containerEntity, ContainerEntityHistory.fromUserAction("Killed", userI.getLogin()));

        final String containerId = containerEntity.getContainerId();
        containerControlApi.killContainer(containerId);
        return containerId;
    }

    private void handleFailure(final ContainerEntity containerEntity) {
        // TODO handle failure
    }

    private PermissionsServiceI getPermissionsService() {
        // We need this layer of indirection, rather than wiring in the PermissionsServiceI implementation,
        // because we need to wait until after XFT/XDAT is fully initialized before getting this. See XNAT-4647.
        if (permissionsService == null) {
            permissionsService = Permissions.getPermissionsService();
        }
        return permissionsService;
    }

    @VisibleForTesting
    public void setPermissionsService(final PermissionsServiceI permissionsService) {
        this.permissionsService = permissionsService;
    }
}
