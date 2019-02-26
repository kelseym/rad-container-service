package org.nrg.containers.model.command.entity;

import org.hibernate.envers.Audited;
import org.nrg.containers.model.command.auto.Command;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

@Entity
@DiscriminatorValue("docker-wrapup")
public class DockerWrapupCommandEntity extends CommandEntity {
    public static final CommandType type = CommandType.DOCKER_WRAPUP;

    public static DockerWrapupCommandEntity fromPojo(final Command commandPojo) {
        return new DockerWrapupCommandEntity();
    }

    @Transient
    public CommandType getType() {
        return type;
    }

    public void setType(final CommandType type) {}
}
