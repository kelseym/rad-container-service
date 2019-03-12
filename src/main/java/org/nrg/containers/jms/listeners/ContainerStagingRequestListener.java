package org.nrg.containers.jms.listeners;

import lombok.extern.slf4j.Slf4j;
import org.nrg.containers.jms.requests.ContainerStagingRequest;
import org.nrg.containers.services.ContainerService;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xdat.security.user.exceptions.UserInitException;
import org.nrg.xdat.security.user.exceptions.UserNotFoundException;
import org.nrg.xft.security.UserI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ContainerStagingRequestListener {
	ContainerService containerService;
	UserManagementServiceI userManagementServiceI;

	@Autowired
	public ContainerStagingRequestListener(ContainerService containerService,
								   UserManagementServiceI userManagementServiceI) {
		this.containerService = containerService;
		this.userManagementServiceI = userManagementServiceI;
	}
	
	
	@JmsListener(containerFactory = "containerListenerContainerFactory", destination = "containerStagingRequest")
	public void onRequest(ContainerStagingRequest request) throws UserNotFoundException, UserInitException{
		UserI user = userManagementServiceI.getUser(request.getUsername());
		log.debug("Consuming staging queue: wfid {}, username {}",
				request.getWorkflowid(), request.getUsername());
		containerService.consumeResolveCommandAndLaunchContainer(request.getProject(), request.getWrapperId(),
				request.getCommandId(), request.getWrapperName(), request.getInputValues(),
				user, request.getWorkflowid());
    }
	
}
