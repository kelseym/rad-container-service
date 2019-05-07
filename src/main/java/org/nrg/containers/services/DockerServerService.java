package org.nrg.containers.services;

import org.nrg.containers.model.server.docker.DockerServerBase.DockerServer;
import org.nrg.framework.exceptions.NotFoundException;

import java.util.List;

public interface DockerServerService {
    List<DockerServer> getServers();
    DockerServer retrieveServer();
    DockerServer getServer() throws NotFoundException;
    DockerServer setServer(DockerServer dockerServer);
    void update(DockerServer dockerServer);
}
