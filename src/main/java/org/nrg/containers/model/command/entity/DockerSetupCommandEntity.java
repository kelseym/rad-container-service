package org.nrg.containers.model.command.entity;

import org.hibernate.envers.Audited;
import org.nrg.containers.model.command.auto.Command;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

@Entity
@DiscriminatorValue("docker-setup")
public class DockerSetupCommandEntity extends CommandEntity {
    public static final CommandType type = CommandType.DOCKER_SETUP;

    public static DockerSetupCommandEntity fromPojo(final Command commandPojo) {
        return new DockerSetupCommandEntity();
    }

    @Transient
    public CommandType getType() {
        return type;
    }

    public void setType(final CommandType type) {}
}
