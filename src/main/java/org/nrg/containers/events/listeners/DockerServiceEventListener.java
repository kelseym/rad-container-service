package org.nrg.containers.events.listeners;

import lombok.extern.slf4j.Slf4j;
import org.nrg.containers.events.model.ServiceTaskEvent;
import org.nrg.containers.services.ContainerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.fn.Consumer;

import java.util.HashSet;

import static reactor.bus.selector.Selectors.type;

@Slf4j
@Component
public class DockerServiceEventListener implements Consumer<Event<ServiceTaskEvent>> {
    private ContainerService containerService;
    private HashSet<Long> currentlyProcessing = new HashSet<>();

    @Autowired
    public DockerServiceEventListener(final EventBus eventBus) {
        eventBus.on(type(ServiceTaskEvent.class), this);
    }

    private synchronized boolean addToQueue(Long serviceDbId) {
        return currentlyProcessing.add(serviceDbId);
    }
    private synchronized void removeFromQueue(Long serviceDbId) {
        currentlyProcessing.remove(serviceDbId);
    }

    @Override
    public void accept(final Event<ServiceTaskEvent> serviceTaskEventEvent) {
        final ServiceTaskEvent event = serviceTaskEventEvent.getData();
        Long serviceDbId = event.service().databaseId();
        if (!addToQueue(serviceDbId)) {
            log.debug("Skipping event {} because service still being processed from last event", event);
            return;
        }
        try {
            containerService.processEvent(event);
        } catch (Throwable e) {
            log.error("There was a problem handling the docker event.", e);
        }
        removeFromQueue(serviceDbId);
    }

    @Autowired
    public void setContainerService(final ContainerService containerService) {
        this.containerService = containerService;
    }
}
