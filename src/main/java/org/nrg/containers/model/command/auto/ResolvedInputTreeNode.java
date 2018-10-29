package org.nrg.containers.model.command.auto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.Lists;
import org.nrg.containers.model.command.auto.Command.Input;

import java.util.List;

@AutoValue
public abstract class ResolvedInputTreeNode<T extends Input> {
    @JsonProperty("input") public abstract T input();
    @JsonProperty("values-and-children") public abstract List<ResolvedInputTreeValueAndChildren> valuesAndChildren();

    public static ResolvedInputTreeNode<? extends Input> create(final PreresolvedInputTreeNode preresolvedInputTreeNode) {
        return create(preresolvedInputTreeNode.input(), Lists.<ResolvedInputTreeValueAndChildren>newArrayList());
    }

    @JsonCreator
    public static <X extends Input> ResolvedInputTreeNode<X> create(@JsonProperty("input") final X input,
                                                                    @JsonProperty("values-and-children") final List<ResolvedInputTreeValueAndChildren> valuesAndChildren) {
        return new AutoValue_ResolvedInputTreeNode<>(input,
                valuesAndChildren == null ? Lists.<ResolvedInputTreeValueAndChildren>newArrayList() : valuesAndChildren);
    }

    @AutoValue
    public static abstract class ResolvedInputTreeValueAndChildren {
        @JsonProperty("value") public abstract ResolvedInputValue resolvedValue();
        @JsonProperty("children") public abstract List<ResolvedInputTreeNode<? extends Input>> children();

        public static ResolvedInputTreeValueAndChildren create(final ResolvedInputValue resolvedValue) {
            return create(resolvedValue, Lists.<ResolvedInputTreeNode<? extends Input>>newArrayList());
        }

        @JsonCreator
        public static ResolvedInputTreeValueAndChildren create(@JsonProperty("value") final ResolvedInputValue resolvedValue,
                                                               @JsonProperty("children") final List<ResolvedInputTreeNode<? extends Input>> children) {
            return new AutoValue_ResolvedInputTreeNode_ResolvedInputTreeValueAndChildren(resolvedValue,
                    children == null ? Lists.<ResolvedInputTreeNode<? extends Input>>newArrayList() : children);
        }
    }
}
