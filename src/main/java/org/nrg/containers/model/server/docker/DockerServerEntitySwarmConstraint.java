package org.nrg.containers.model.server.docker;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@Entity
public class DockerServerEntitySwarmConstraint implements Serializable {
    private long id;
    @JsonIgnore
    private DockerServerEntity dockerServerEntity;
    private boolean userSettable;
    private String attribute;
    private String comparator;
    private List<String> values;

    public static DockerServerEntitySwarmConstraint fromPojo(final DockerServerBase.DockerServerSwarmConstraint pojo) {
        final DockerServerEntitySwarmConstraint entity = new DockerServerEntitySwarmConstraint();
        entity.update(pojo);
        return entity;
    }

    public DockerServerEntitySwarmConstraint update(final DockerServerBase.DockerServerSwarmConstraint pojo) {
        this.setId(pojo.id());
        this.setUserSettable(pojo.userSettable());
        this.setAttribute(pojo.attribute());
        this.setComparator(pojo.comparator());
        this.setValues(pojo.values());
        this.setDockerServerEntity(dockerServerEntity);
        return this;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    @ManyToOne
    public DockerServerEntity getDockerServerEntity() {
        return dockerServerEntity;
    }
    public void setDockerServerEntity(final DockerServerEntity dockerServerEntity) {
        this.dockerServerEntity = dockerServerEntity;
    }

    public boolean getUserSettable() {
        return userSettable;
    }
    public void setUserSettable(boolean userSettable) {
        this.userSettable = userSettable;
    }

    public String getAttribute() {
        return attribute;
    }
    public void setAttribute(final String attribute) {
        this.attribute = attribute;
    }

    public String getComparator() {
        return comparator;
    }
    public void setComparator(final String comparator) {
        this.comparator = comparator;
    }

    @ElementCollection
    public List<String> getValues() {
        return values;
    }
    public void setValues(final List<String> values) {
        this.values = values;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final DockerServerEntitySwarmConstraint that = (DockerServerEntitySwarmConstraint) o;
        return userSettable == that.userSettable &&
                Objects.equals(this.dockerServerEntity, that.dockerServerEntity) &&
                Objects.equals(this.attribute, that.attribute) &&
                Objects.equals(this.comparator, that.comparator) &&
                Objects.equals(this.values, that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dockerServerEntity, userSettable, attribute, comparator, values);
    }

}
