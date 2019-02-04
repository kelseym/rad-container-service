package org.nrg.containers.initialization.tasks;

import lombok.extern.slf4j.Slf4j;
import org.nrg.containers.model.configuration.PluginVersionCheck;
import org.nrg.containers.services.ContainerService;
import org.nrg.xnat.initialization.tasks.AbstractInitializingTask;
import org.nrg.xnat.initialization.tasks.InitializingTaskException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CheckContainerServiceVersion extends AbstractInitializingTask {
    private final ContainerService containerService;

    @Autowired
    public CheckContainerServiceVersion(final ContainerService containerService) {
        this.containerService = containerService;
    }

    @Override
    public String getTaskName() {
        return "Check XNAT version compatibility with this plugin.";
    }

    @Override
    protected void callImpl() throws InitializingTaskException {

        log.debug("Checking Container Service plugin / XNAT compatibility.");
        PluginVersionCheck versionCheck = containerService.checkXnatVersion();
        if(versionCheck != null && versionCheck.compatible() == false){
            log.error(versionCheck.message());
            log.error("XNAT version: " + versionCheck.xnatVersionDetected());
        } else if(log.isDebugEnabled() && versionCheck != null){
            if(versionCheck.compatible()) log.debug("Version compatibility check: Passed");
        }

    }

}
