package org.nrg.actions.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.Entity;
import java.util.List;
import java.util.Objects;

@Entity
public class ActionContextExecution extends AbstractHibernateEntity {
    private String name;
    private String description;
    @JsonProperty("action-id") private Long actionId;
    @JsonProperty("root-id") private String rootId;
    private List<ActionInput> inputs;
    @JsonProperty("resources-staged") private List<ActionResource> resourcesStaged;
    @JsonProperty("resources-created") private List<ActionResource> resourcesCreated;
    @JsonProperty("resolved-command") private ResolvedCommand resolvedCommand;

    public ActionContextExecution() {}

    public ActionContextExecution(final ActionContextExecutionDto aceDto,
                                  final ResolvedCommand resolvedCommand) {
        if (aceDto == null) {
            return;
        }
        this.name = aceDto.getName();
        this.description = aceDto.getDescription();
        this.actionId = aceDto.getActionId();
        this.rootId = aceDto.getRootId();
        this.inputs = aceDto.getInputs();
        this.resourcesCreated = aceDto.getResourcesCreated();
        this.resourcesStaged = aceDto.getResourcesStaged();

        this.resolvedCommand = resolvedCommand;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public Long getActionId() {
        return actionId;
    }

    public void setActionId(final Long actionId) {
        this.actionId = actionId;
    }

    public String getRootId() {
        return rootId;
    }

    public void setRootId(final String rootId) {
        this.rootId = rootId;
    }

    public List<ActionInput> getInputs() {
        return inputs;
    }

    public void setInputs(final List<ActionInput> inputs) {
        this.inputs = inputs;
    }

    public List<ActionResource> getResourcesStaged() {
        return resourcesStaged;
    }

    public void setResourcesStaged(final List<ActionResource> resourcesStaged) {
        this.resourcesStaged = resourcesStaged;
    }

    public List<ActionResource> getResourcesCreated() {
        return resourcesCreated;
    }

    public void setResourcesCreated(final List<ActionResource> resourcesCreated) {
        this.resourcesCreated = resourcesCreated;
    }

    public ResolvedCommand getResolvedCommand() {
        return resolvedCommand;
    }

    public void setResolvedCommand(final ResolvedCommand resolvedCommand) {
        this.resolvedCommand = resolvedCommand;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ActionContextExecution that = (ActionContextExecution) o;
        return Objects.equals(this.actionId, that.actionId) &&
                Objects.equals(this.name, that.name) &&
                Objects.equals(this.description, that.description) &&
                Objects.equals(this.rootId, that.rootId) &&
                Objects.equals(this.inputs, that.inputs) &&
                Objects.equals(this.resourcesStaged, that.resourcesStaged) &&
                Objects.equals(this.resourcesCreated, that.resourcesCreated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actionId, name, description, rootId, inputs, resourcesStaged, resourcesCreated, resolvedCommand);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("description", description)
                .add("actionId", actionId)
                .add("rootId", rootId)
                .add("inputs", inputs)
                .add("resourcesStaged", resourcesStaged)
                .add("resourcesCreated", resourcesCreated)
                .add("resolvedCommand", resolvedCommand)
                .toString();
    }
}
