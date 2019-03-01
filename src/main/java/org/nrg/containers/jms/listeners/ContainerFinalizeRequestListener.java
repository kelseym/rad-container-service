package org.nrg.containers.jms.listeners;

import org.nrg.containers.jms.requests.ContainerFinalizeRequest;
import org.nrg.containers.jms.utils.QueueUtils;
import org.nrg.containers.model.container.auto.Container;
import org.nrg.containers.services.ContainerService;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.xapi.model.users.UserFactory;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xdat.security.user.exceptions.UserInitException;
import org.nrg.xdat.security.user.exceptions.UserNotFoundException;
import org.nrg.xft.security.UserI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
@Slf4j
@Component
public class ContainerFinalizeRequestListener {
	
	@Autowired
	ContainerService containerService;
	
	@Autowired
	UserFactory userFactory;

	@Autowired
	UserManagementServiceI userManagementServiceI;
	
	
	@JmsListener(containerFactory = "containerListenerContainerFactory", destination = "containerFinalizeRequest")
	public void onRequest( ContainerFinalizeRequest request) throws UserNotFoundException, NotFoundException ,UserInitException{
		Container container=containerService.get(request.getId());
		UserI user=userManagementServiceI.getUser(request.getUsername());
		int count=QueueUtils.count(request.getDestination());
		log.info("consuming finalizing queue, count {}, exitcode {}, issuccessfull {}, id {}, username {}, status {}",count,request.getExitCodeString(),request.isSuccessful(),request.getId(),request.getUsername(),container.status());
    	containerService.consumeFinalize(request.getExitCodeString(), request.isSuccessful(), container,user);
    }
	
}
