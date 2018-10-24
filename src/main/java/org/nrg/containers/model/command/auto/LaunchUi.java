package org.nrg.containers.model.command.auto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import lombok.extern.slf4j.Slf4j;
import org.nrg.containers.model.command.auto.Command.Input;
import org.nrg.containers.model.command.auto.ResolvedCommand.PartiallyResolvedCommand;
import org.nrg.containers.model.command.auto.ResolvedInputTreeNode.ResolvedInputTreeValueAndChildren;
import org.nrg.containers.model.configuration.CommandConfiguration.CommandInputConfiguration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@SuppressWarnings("NullableProblems")
public abstract class LaunchUi {

    @JsonProperty("meta") public abstract LaunchUiMeta meta();
    @JsonProperty("input-config") public abstract List<LaunchUiInputTree> inputTrees();

    @AutoValue
    public static abstract class LaunchUiMeta {
        @JsonProperty("command-id") public abstract Long commandId();
        @JsonProperty("command-name") public abstract String commandName();
        @Nullable @JsonProperty("command-label") public abstract String commandLabel();
        @Nullable @JsonProperty("command-description") public abstract String commandDescription();
        @JsonProperty("wrapper-id") public abstract Long wrapperId();
        @JsonProperty("wrapper-name") public abstract String wrapperName();
        @Nullable @JsonProperty("wrapper-label") public abstract String wrapperLabel();
        @Nullable @JsonProperty("wrapper-description") public abstract String wrapperDescription();
        @JsonProperty("image-name") public abstract String imageName();
        @JsonProperty("image-type") public abstract String imageType();

        public static LaunchUiMeta create(final @Nonnull Long commandId,
                                          final @Nonnull String commandName,
                                          final @Nullable String commandLabel,
                                          final @Nullable String commandDescription,
                                          final @Nonnull Long wrapperId,
                                          final @Nonnull String wrapperName,
                                          final @Nullable String wrapperLabel,
                                          final @Nullable String wrapperDescription,
                                          final @Nonnull String imageName,
                                          final @Nonnull String imageType) {
            return builder()
                    .commandId(commandId)
                    .commandName(commandName)
                    .commandLabel(commandLabel)
                    .commandDescription(commandDescription)
                    .wrapperId(wrapperId)
                    .wrapperName(wrapperName)
                    .wrapperLabel(wrapperLabel)
                    .wrapperDescription(wrapperDescription)
                    .imageName(imageName)
                    .imageType(imageType)
                    .build();
        }

        public static LaunchUiMeta create(final @Nonnull PartiallyResolvedCommand partiallyResolvedCommand) {
            return builder()
                    .commandId(partiallyResolvedCommand.commandId())
                    .commandName(partiallyResolvedCommand.commandName())
                    .commandLabel(partiallyResolvedCommand.commandLabel())
                    .commandDescription(partiallyResolvedCommand.commandDescription())
                    .wrapperId(partiallyResolvedCommand.wrapperId())
                    .wrapperName(partiallyResolvedCommand.wrapperName())
                    .wrapperDescription(partiallyResolvedCommand.wrapperDescription())
                    .imageName(partiallyResolvedCommand.image())
                    .imageType(partiallyResolvedCommand.type())
                    .build();
        }

        public static Builder builder() {
            return new AutoValue_LaunchUi_LaunchUiMeta.Builder();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract Builder commandId(@Nonnull Long commandId);
            public abstract Builder commandName(@Nonnull String commandName);
            public abstract Builder commandLabel(@Nullable String commandLabel);
            public abstract Builder commandDescription(@Nullable String commandDescription);
            public abstract Builder wrapperId(@Nonnull Long wrapperId);
            public abstract Builder wrapperName(@Nonnull String wrapperName);
            public abstract Builder wrapperLabel(@Nullable String wrapperLabel);
            public abstract Builder wrapperDescription(@Nullable String wrapperDescription);
            public abstract Builder imageName(@Nonnull String imageName);
            public abstract Builder imageType(@Nonnull String imageType);

            public abstract LaunchUiMeta build();
        }
    }

    @AutoValue
    public static abstract class LaunchUiInputTree {
        @JsonProperty("name") public abstract String name();
        @Nullable @JsonProperty("label") public abstract String label();
        @Nullable @JsonProperty("description") public abstract String description();
        @JsonProperty("advanced") public abstract boolean advanced();
        @JsonProperty("required") public abstract boolean required();
        @JsonProperty("user-settable") public abstract boolean userSettable();
        @JsonProperty("input-type") public abstract String inputType();
        @JsonProperty("children") public abstract List<LaunchUiInputTree> children();

        public static LaunchUiInputTree create(final @Nonnull String name,
                                               final @Nullable String label,
                                               final @Nullable String description,
                                               final boolean advanced,
                                               final boolean required,
                                               final boolean userSettable,
                                               final @Nonnull String inputType,
                                               final @Nonnull List<LaunchUiInputTree> children) {
            return builder()
                    .name(name)
                    .label(label)
                    .description(description)
                    .advanced(advanced)
                    .required(required)
                    .userSettable(userSettable)
                    .inputType(inputType)
                    .children(children)
                    .build();
        }

        public static Builder builder() {
            return new AutoValue_LaunchUi_LaunchUiInputTree.Builder();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract Builder name(@Nonnull String name);
            public abstract Builder label(@Nullable String label);
            public abstract Builder description(@Nullable String description);
            public abstract Builder advanced(boolean advanced);
            public abstract Builder required(boolean required);
            public abstract Builder userSettable(boolean userSettable);
            public abstract Builder inputType(@Nonnull String inputType);

            public abstract Builder children(@Nonnull List<LaunchUiInputTree> children);


            public abstract LaunchUiInputTree build();
        }
    }

    @AutoValue
    public static abstract class LaunchUiValueTree {
        @JsonProperty("name") public abstract String name();
        @JsonProperty("values") public abstract List<LaunchUiValueTreeValueAndChildren> valueAndChildrenList();

        public static LaunchUiValueTree create(final @Nonnull String name,
                                               final @Nonnull List<LaunchUiValueTreeValueAndChildren> valueAndChildrenList) {
            return LaunchUiValueTree.builder()
                    .name(name)
                    .valueAndChildrenList(valueAndChildrenList)
                    .build();
        }

        public static LaunchUiValueTree create(final @Nonnull ResolvedInputTreeNode<? extends Input> resolvedInputTree) {
            return LaunchUiValueTree.builder()
                    .name(resolvedInputTree.input().name())
                    .valueAndChildrenListFromResolvedInputTreeValuesAndChildren(resolvedInputTree.valuesAndChildren())
                    .build();
        }

        public static Builder builder() {
            return new AutoValue_LaunchUi_LaunchUiValueTree.Builder();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract Builder name(@Nonnull String name);

            public abstract Builder valueAndChildrenList(@Nonnull List<LaunchUiValueTreeValueAndChildren> valueAndChildrenList);

            public Builder valueAndChildrenListFromResolvedInputTreeValuesAndChildren(final @Nonnull List<ResolvedInputTreeValueAndChildren> resolvedInputTreeValueAndChildrenList) {
                final List<LaunchUiValueTreeValueAndChildren> valueAndChildrenList = new ArrayList<>();
                for (final ResolvedInputTreeValueAndChildren resolvedInputTreeValueAndChildren : resolvedInputTreeValueAndChildrenList) {
                    final LaunchUiValueTreeValueAndChildren launchUiValueTreeValueAndChildren = LaunchUiValueTreeValueAndChildren.create(resolvedInputTreeValueAndChildren);
                    if (launchUiValueTreeValueAndChildren != null) {
                        valueAndChildrenList.add(launchUiValueTreeValueAndChildren);
                    }
                }
                return this.valueAndChildrenList(valueAndChildrenList);
            }

            public abstract LaunchUiValueTree build();
        }
    }

    @AutoValue
    public static abstract class LaunchUiValueTreeValueAndChildren {
        @JsonProperty("value") public abstract String value();
        @JsonProperty("label") public abstract String label();
        @JsonProperty("children") public abstract List<LaunchUiValueTree> children();

        public static LaunchUiValueTreeValueAndChildren create(final @Nonnull String value,
                                                               final @Nonnull String label,
                                                               final @Nonnull List<LaunchUiValueTree> children) {
            return builder()
                    .value(value)
                    .label(label)
                    .children(children)
                    .build();
        }

        public static LaunchUiValueTreeValueAndChildren create(final @Nonnull ResolvedInputTreeValueAndChildren resolvedInputTreeValueAndChildren) {
            final ResolvedInputValue resolvedInputValue = resolvedInputTreeValueAndChildren.resolvedValue();
            final String value = resolvedInputValue.value();
            if (value == null) {
                return null;
            }
            final String valueLabel = resolvedInputValue.valueLabel();
            return builder()
                    .value(value)
                    .label(valueLabel == null ? value : valueLabel)
                    .childrenFromResolvedInputTrees(resolvedInputTreeValueAndChildren.children())
                    .build();
        }

        public static Builder builder() {
            return new AutoValue_LaunchUi_LaunchUiValueTreeValueAndChildren.Builder();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract Builder value(@Nonnull String value);
            public abstract Builder label(@Nullable String label);

            public abstract Builder children(@Nonnull List<LaunchUiValueTree> children);

            public Builder childrenFromResolvedInputTrees(@Nonnull List<ResolvedInputTreeNode<? extends Input>> resolvedInputTrees) {
                final List<LaunchUiValueTree> children = new ArrayList<>();
                for (final ResolvedInputTreeNode<? extends Input> resolvedInputTree : resolvedInputTrees) {
                    children.add(LaunchUiValueTree.create(resolvedInputTree));
                }
                return this.children(children);
            }

            public abstract LaunchUiValueTreeValueAndChildren build();
        }
    }

    @AutoValue
    public static abstract class SingleLaunchUi extends LaunchUi {
        @JsonProperty("input-values") public abstract List<LaunchUiValueTree> valueTrees();

        public static SingleLaunchUi create(final @Nonnull PartiallyResolvedCommand partiallyResolvedCommand,
                                            final @Nonnull Map<String, CommandInputConfiguration> inputConfigurationMap) {
            return builder()
                    .meta(LaunchUiMeta.create(partiallyResolvedCommand))
                    .populateInputTreeAndInputValueTreeFromResolvedInputTrees(partiallyResolvedCommand.resolvedInputTrees(), inputConfigurationMap)
                    .build();
        }

        public static Builder builder() {
            return new AutoValue_LaunchUi_SingleLaunchUi.Builder();
        }

        @AutoValue.Builder
        public static abstract class Builder {
            public abstract Builder meta(@Nonnull LaunchUiMeta meta);
            public abstract Builder inputTrees(@Nonnull List<LaunchUiInputTree> inputTrees);

            public abstract Builder valueTrees(@Nonnull List<LaunchUiValueTree> valueTrees);

            public Builder populateInputTreeAndInputValueTreeFromResolvedInputTrees(final @Nonnull List<ResolvedInputTreeNode<? extends Input>> resolvedInputTrees,
                                                                                    final @Nonnull Map<String, CommandInputConfiguration> inputConfigurationMap) {
                final List<LaunchUiInputTree> inputTrees = new ArrayList<>();
                final List<LaunchUiValueTree> valueTrees = new ArrayList<>();
                for (final ResolvedInputTreeNode<? extends Input> rootNode : resolvedInputTrees) {
                    final String inputName = rootNode.input().name();
                    if (log.isDebugEnabled()) {
                        log.debug("ROOT " + inputName + " - Populating input relationship tree.");
                    }
                    inputTrees.add(convertResolvedInputTreeToLaunchUiInputTree(rootNode, inputConfigurationMap));

                    if (log.isDebugEnabled()) {
                        log.debug("ROOT " + inputName + " - Populating input value tree.");
                    }
                    valueTrees.add(LaunchUiValueTree.create(rootNode));
                }
                return this
                        .inputTrees(inputTrees)
                        .valueTrees(valueTrees);
            }

            public abstract SingleLaunchUi build();
        }
    }

    @AutoValue
    public static abstract class BulkLaunchUi extends LaunchUi {
        @JsonProperty("input-values") public abstract List<List<LaunchUiValueTree>> listOfValueTrees();


        public static Builder builder() {
            return new AutoValue_LaunchUi_BulkLaunchUi.Builder();
        }

        @AutoValue.Builder
        public static abstract class Builder {

            public abstract Builder meta(@Nonnull LaunchUiMeta meta);
            public abstract Builder inputTrees(@Nonnull List<LaunchUiInputTree> inputTrees);

            public abstract Builder listOfValueTrees(@Nonnull List<List<LaunchUiValueTree>> listOfValueTrees);

            public Builder populateInputTreeAndInputValueTreeFromResolvedInputTrees(final @Nonnull List<List<ResolvedInputTreeNode<? extends Input>>> listOfResolvedInputTrees,
                                                                                    final @Nonnull Map<String, CommandInputConfiguration> inputConfigurationMap) {
                final List<LaunchUiInputTree> inputTrees = new ArrayList<>();
                final List<List<LaunchUiValueTree>> listOfValueTrees = new ArrayList<>();
                for (final List<ResolvedInputTreeNode<? extends Input>> resolvedInputTrees : listOfResolvedInputTrees) {
                    final List<LaunchUiValueTree> valueTrees = new ArrayList<>();
                    for (final ResolvedInputTreeNode<? extends Input> rootNode : resolvedInputTrees) {
                        final String inputName = rootNode.input().name();
                        if (log.isDebugEnabled()) {
                            log.debug("ROOT " + inputName + " - Populating input relationship tree.");
                        }
                        inputTrees.add(convertResolvedInputTreeToLaunchUiInputTree(rootNode, inputConfigurationMap));

                        if (log.isDebugEnabled()) {
                            log.debug("ROOT " + inputName + " - Populating input value tree.");
                        }
                        valueTrees.add(LaunchUiValueTree.create(rootNode));
                    }
                    listOfValueTrees.add(valueTrees);
                }
                return this
                        .inputTrees(inputTrees)
                        .listOfValueTrees(listOfValueTrees);
            }
            public abstract BulkLaunchUi build();
        }
    }

    private static LaunchUiInputTree convertResolvedInputTreeToLaunchUiInputTree(final @Nonnull ResolvedInputTreeNode<? extends Input> node,
                                                                                 final @Nonnull Map<String, CommandInputConfiguration> inputConfigurationMap) {

        final List<LaunchUiInputTree> children = new ArrayList<>();
        final Set<String> alreadyAddedChildNames = new HashSet<>();
        for (final ResolvedInputTreeValueAndChildren valueAndChildren : node.valuesAndChildren()) {
            for (final ResolvedInputTreeNode<? extends Input> child : valueAndChildren.children()) {
                if (Command.CommandInput.class.isAssignableFrom(child.input().getClass())) {
                    // This is a command input which can be derived from a wrapper input.
                    // We can safely skip it.
                    continue;
                }

                if (alreadyAddedChildNames.contains(child.input().name())) {
                    // We're flattening out the resolved input tree; we don't care about the values.
                    // Since for each value we see the children again, we've likely already seen this child.
                    // So skip it.
                    continue;
                }

                if (log.isDebugEnabled()) {
                    log.debug("Adding " + node.input().name() + " child " + child.input().name());
                }
                alreadyAddedChildNames.add(child.input().name());
                children.add(convertResolvedInputTreeToLaunchUiInputTree(child, inputConfigurationMap));
            }

        }

        final Input input = node.input();
        final CommandInputConfiguration inputConfiguration =
                inputConfigurationMap.containsKey(input.name()) && inputConfigurationMap.get(input.name()) != null ?
                        inputConfigurationMap.get(input.name()) :
                        CommandInputConfiguration.builder().build();

        final Boolean requiredConfig = inputConfiguration.required();
        final boolean inputIsRequired = input.required() || (requiredConfig != null && requiredConfig);
        final Boolean settableConfig = inputConfiguration.userSettable();
        final boolean userSettable = settableConfig == null || settableConfig; // default: true
        final Boolean advancedConfig = inputConfiguration.advanced();
        final boolean advanced = advancedConfig != null && advancedConfig; // default: false

        final String inputType = "";    // TODO Look up old code to figure out input type

        return LaunchUiInputTree.builder()
                .name(input.name())
                .label(input.name()) // TODO add label to commandInput (pojo, hibernate, tests, and examples)
                .description(input.description())
                .required(inputIsRequired)
                .userSettable(userSettable)
                .advanced(advanced)
                .inputType(inputType)
                .children(children)
                .build();
    }
}
