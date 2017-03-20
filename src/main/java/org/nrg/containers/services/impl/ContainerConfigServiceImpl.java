package org.nrg.containers.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.nrg.config.entities.Configuration;
import org.nrg.config.exceptions.ConfigServiceException;
import org.nrg.config.services.ConfigService;
import org.nrg.containers.model.CommandConfiguration;
import org.nrg.containers.model.CommandConfigurationInternalRepresentation;
import org.nrg.containers.services.ContainerConfigService;
import org.nrg.framework.constants.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.io.IOException;

@Service
public class ContainerConfigServiceImpl implements ContainerConfigService {
    private static final Logger log = LoggerFactory.getLogger(ContainerConfigService.class);

    public static final String DEFAULT_DOCKER_HUB_PATH = "default-docker-hub-id";
    public static final String COMMAND_CONFIG_PATH_TEMPLATE = "command-%d-wrapper-%s";
    public static final String ALL_DISABLED_PATH = "disable-all-commands";

    private final ConfigService configService;
    private final ObjectMapper mapper;

    @Autowired
    public ContainerConfigServiceImpl(final ConfigService configService,
                                      final ObjectMapper mapper) {
        this.configService = configService;
        this.mapper = mapper;
    }

    @Override
    public long getDefaultDockerHubId() {
        final Configuration defaultDockerHubConfig = configService.getConfig(TOOL_ID, DEFAULT_DOCKER_HUB_PATH);
        long id = 0L;
        if (defaultDockerHubConfig != null) {
            final String contents = defaultDockerHubConfig.getContents();
            if (StringUtils.isNotBlank(contents)) {
                try {
                    id = Long.valueOf(contents);
                } catch (NumberFormatException ignored) {
                    // ignored
                }
            }
        }
        return id;
    }

    @Override
    public void setDefaultDockerHubId(final long hubId, final String username, final String reason) {
        try {
            configService.replaceConfig(username, reason, TOOL_ID, DEFAULT_DOCKER_HUB_PATH, String.valueOf(hubId));
        } catch (ConfigServiceException e) {
            log.error("Could not save default docker hub config.", e);
        }
    }

    @Override
    public void configureForProject(final CommandConfiguration commandConfiguration, final String project, final long commandId, final String wrapperName, final boolean enable, final String username, final String reason) throws CommandConfigurationException {
        setCommandConfiguration(commandConfiguration, Scope.Project, project, commandId, wrapperName, enable, username, reason);
    }

    @Override
    public void configureForSite(final CommandConfiguration commandConfiguration, final long commandId, final String wrapperName, final boolean enable, final String username, final String reason) throws CommandConfigurationException {
        setCommandConfiguration(commandConfiguration, Scope.Site, null, commandId, wrapperName, enable, username, reason);
    }

    @Override
    @Nullable
    public CommandConfiguration getSiteConfiguration(final long commandId, final String wrapperName) {
        return getCommandConfiguration(Scope.Site, null, commandId, wrapperName);
    }

    @Override
    @Nullable
    public CommandConfiguration getProjectConfiguration(final String project, final long commandId, final String wrapperName) {
        final CommandConfiguration siteConfig = getSiteConfiguration(commandId, wrapperName);
        final CommandConfiguration projectConfig = getCommandConfiguration(Scope.Project, project, commandId, wrapperName);
        return siteConfig == null ? projectConfig : siteConfig.merge(projectConfig);
    }

    @Override
    public void deleteSiteConfiguration(final long commandId, final String wrapperName, final String username) throws CommandConfigurationException {
        deleteCommandConfiguration(Scope.Site, null, commandId, wrapperName, username);
    }

    @Override
    public void deleteProjectConfiguration(final String project, final long commandId, final String wrapperName, final String username) throws CommandConfigurationException {
        deleteCommandConfiguration(Scope.Project, project, commandId, wrapperName, username);
    }

    @Override
    public void deleteAllConfiguration(final long commandId, final String wrapperName) {
        // TODO
    }

    @Override
    public void deleteAllConfiguration(final long commandId) {
        // TODO
    }

    @Override
    public void setAllDisabledForSite(final String username, final String reason) throws ConfigServiceException {
        setAllDisabledForSite(true, username, reason);
    }

    @Override
    public void setAllDisabledForSite(final Boolean allDisabled, final String username, final String reason) throws ConfigServiceException {
        configService.replaceConfig(username, reason,
                TOOL_ID, ALL_DISABLED_PATH,
                String.valueOf(allDisabled),
                Scope.Site, null);
    }

    @Override
    @Nullable
    public Boolean getAllDisabledForSite() {
        return parseAllDisabledConfig(configService.getConfig(TOOL_ID, ALL_DISABLED_PATH, Scope.Site, null));
    }

    @Override
    public void setAllDisabledForProject(final String project, final String username, final String reason) throws ConfigServiceException {
        setAllDisabledForProject(true, project, username, reason);
    }

    @Override
    public void setAllDisabledForProject(final Boolean allDisabled, final String project, final String username, final String reason) throws ConfigServiceException {
        configService.replaceConfig(username, reason,
                TOOL_ID, ALL_DISABLED_PATH,
                String.valueOf(allDisabled),
                Scope.Project, project);
    }

    @Override
    @Nullable
    public Boolean getAllDisabledForProject(final String project) {
        return parseAllDisabledConfig(configService.getConfig(TOOL_ID, ALL_DISABLED_PATH, Scope.Project, project));
    }

    @Nullable
    private Boolean parseAllDisabledConfig(final @Nullable Configuration allDisabledConfig) {
        return Boolean.parseBoolean(allDisabledConfig == null ? null : allDisabledConfig.getContents());
    }

    @Override
    public void enableForSite(final long commandId, final String wrapperName, final String username, final String reason) throws CommandConfigurationException {
        setCommandEnabled(true, Scope.Site, null, commandId, wrapperName, username, reason);
    }

    @Override
    public void disableForSite(final long commandId, final String wrapperName, final String username, final String reason) throws CommandConfigurationException {
        setCommandEnabled(false, Scope.Site, null, commandId, wrapperName, username, reason);
    }

    @Override
    @Nullable
    public Boolean isEnabledForSite(final long commandId, final String wrapperName) {
        return getCommandIsEnabled(Scope.Site, null, commandId, wrapperName);
    }

    @Override
    public void enableForProject(final String project, final long commandId, final String wrapperName, final String username, final String reason) throws CommandConfigurationException {
        setCommandEnabled(true, Scope.Project, project, commandId, wrapperName, username, reason);
    }

    @Override
    public void disableForProject(final String project, final long commandId, final String wrapperName, final String username, final String reason) throws CommandConfigurationException {
        setCommandEnabled(false, Scope.Project, project, commandId, wrapperName, username, reason);
    }

    @Override
    @Nullable
    public Boolean isEnabledForProject(final String project, final long commandId, final String wrapperName) {
        return getCommandIsEnabled(Scope.Project, project, commandId, wrapperName);
    }

    private void setCommandConfiguration(final CommandConfiguration commandConfiguration, final Scope scope, final String project, final long commandId, final String wrapperName, final boolean enable, final String username, final String reason) throws CommandConfigurationException {
        final CommandConfigurationInternalRepresentation alreadyExists = getCommandConfigurationInternalRepresentation(scope, project, commandId, wrapperName);
        // If the "enable" param is true, we enable the configuration.
        // Otherwise, we leave it alone if the configuration already exists, or set it to null.
        // We will never set "enabled=false" here.
        final Boolean enabledStatusToSet = enable ? Boolean.TRUE : (alreadyExists == null ? null : alreadyExists.enabled());
        setCommandConfigurationInternalRepresentation(
                CommandConfigurationInternalRepresentation.create(enabledStatusToSet, commandConfiguration),
                scope, project, commandId, wrapperName, username, reason);
    }

    private void setCommandEnabled(final Boolean enabled, final Scope scope, final String project, final long commandId, final String wrapperName, final String username, final String reason) throws CommandConfigurationException {
        final CommandConfigurationInternalRepresentation alreadyExists = getCommandConfigurationInternalRepresentation(scope, project, commandId, wrapperName);
        setCommandConfigurationInternalRepresentation(CommandConfigurationInternalRepresentation.create(enabled, alreadyExists == null ? null : alreadyExists.configuration()),
                scope, project, commandId, wrapperName, username, reason);
    }

    private void setCommandConfigurationInternalRepresentation(final CommandConfigurationInternalRepresentation commandConfigurationInternalRepresentation,
                                                               final Scope scope, final String project, final long commandId, final String wrapperName, final String username, final String reason) throws CommandConfigurationException {
        if (scope.equals(Scope.Project) && StringUtils.isBlank(project)) {
            // TODO error: project can't be blank
        }

        if (commandId == 0L) {
            // TODO error
        }

        String contents = "";
        try {
            contents = mapper.writeValueAsString(commandConfigurationInternalRepresentation);
        } catch (JsonProcessingException e) {
            final String message = String.format("Could not save configuration for command id %d, wrapper name \"%s\".", commandId, wrapperName);
            log.error(message);
            throw new CommandConfigurationException(message, e);
        }

        final String path = String.format(COMMAND_CONFIG_PATH_TEMPLATE, commandId, wrapperName != null ? wrapperName : "");
        try {
            configService.replaceConfig(username, reason, TOOL_ID, path, contents, scope, project);
        } catch (ConfigServiceException e) {
            final String message = String.format("Could not save configuration for command id %d, wrapper name \"%s\".", commandId, wrapperName);
            log.error(message);
            throw new CommandConfigurationException(message, e);
        }
    }

    @Nullable
    private CommandConfiguration getCommandConfiguration(final Scope scope, final String project, final long commandId, final String wrapperName) {
        final CommandConfigurationInternalRepresentation commandConfigurationInternalRepresentation = getCommandConfigurationInternalRepresentation(scope, project, commandId, wrapperName);
        return commandConfigurationInternalRepresentation == null ? null : commandConfigurationInternalRepresentation.configuration();
    }

    @Nullable
    private Boolean getCommandIsEnabled(final Scope scope, final String project, final long commandId, final String wrapperName) {
        final CommandConfigurationInternalRepresentation commandConfigurationInternalRepresentation = getCommandConfigurationInternalRepresentation(scope, project, commandId, wrapperName);
        return commandConfigurationInternalRepresentation == null ? null : commandConfigurationInternalRepresentation.enabled();
    }

    @Nullable
    private CommandConfigurationInternalRepresentation getCommandConfigurationInternalRepresentation(final Scope scope, final String project, final long commandId, final String wrapperName) {
        if (scope.equals(Scope.Project) && StringUtils.isBlank(project)) {
            // TODO error: project can't be blank
        }

        if (commandId == 0L) {
            // TODO error
        }

        final String path = String.format(COMMAND_CONFIG_PATH_TEMPLATE, commandId, wrapperName != null ? wrapperName : "");
        final Configuration configuration = configService.getConfig(TOOL_ID, path, scope, project);
        if (configuration == null) {
            return null;
        }

        final String configurationJson = configuration.getContents();
        if (StringUtils.isBlank(configurationJson)) {
            return null;
        }

        try {
            return mapper.readValue(configurationJson, CommandConfigurationInternalRepresentation.class);
        } catch (IOException e) {
            final String message = String.format("Could not deserialize Command Configuration for %s, command id %s, wrapper \"%s\".",
                    scope.equals(Scope.Site) ? "site" : "project " + project,
                    commandId,
                    wrapperName);
            log.error(message, e);
            //throw new CommandConfigurationException(message, e);
        }
        return null;
    }

    private void deleteCommandConfiguration(final Scope scope, final String project, final long commandId, final String wrapperName, final String username) throws CommandConfigurationException {
        if (scope.equals(Scope.Project) && StringUtils.isBlank(project)) {
            // TODO error: project can't be blank
        }

        if (commandId == 0L) {
            // TODO error
        }

        final CommandConfigurationInternalRepresentation commandConfigurationInternalRepresentation = getCommandConfigurationInternalRepresentation(scope, project, commandId, wrapperName);
        if (commandConfigurationInternalRepresentation == null) {
            return;
        }
        if (commandConfigurationInternalRepresentation.enabled() == null) {
            final String path = String.format(COMMAND_CONFIG_PATH_TEMPLATE, commandId, wrapperName != null ? wrapperName : "");
            configService.delete(configService.getConfig(TOOL_ID, path, scope, project));
            return;
        }

        setCommandConfigurationInternalRepresentation(CommandConfigurationInternalRepresentation.create(commandConfigurationInternalRepresentation.enabled(), null),
                scope, project, commandId, wrapperName, username, "Deleting command configuration");
    }
}
