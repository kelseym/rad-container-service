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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public abstract class LaunchUi {

    @JsonProperty("meta") public abstract LaunchUiMeta meta();
    @JsonProperty("input-config") public abstract Map<String, LaunchUiInputTree> inputTrees();

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

        public static LaunchUiMeta create(final Long commandId,
                                          final String commandName,
                                          final String commandLabel,
                                          final String commandDescription,
                                          final Long wrapperId,
                                          final String wrapperName,
                                          final String wrapperLabel,
                                          final String wrapperDescription,
                                          final String imageName,
                                          final String imageType) {
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

        public static LaunchUiMeta create(final PartiallyResolvedCommand partiallyResolvedCommand) {
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
            public abstract Builder commandId(final Long commandId);
            public abstract Builder commandName(final String commandName);
            public abstract Builder commandLabel(final String commandLabel);
            public abstract Builder commandDescription(final String commandDescription);
            public abstract Builder wrapperId(final Long wrapperId);
            public abstract Builder wrapperName(final String wrapperName);
            public abstract Builder wrapperLabel(final String wrapperLabel);
            public abstract Builder wrapperDescription(final String wrapperDescription);
            public abstract Builder imageName(final String imageName);
            public abstract Builder imageType(final String imageType);

            public abstract LaunchUiMeta build();
        }
    }

    @AutoValue
    public static abstract class LaunchUiInputTree {
        @JsonProperty("meta") public abstract LaunchUiInputTreeMeta meta();
        @JsonProperty("children") public abstract Map<String, LaunchUiInputTree> children();

        public static LaunchUiInputTree create(final LaunchUiInputTreeMeta meta, final Map<String, LaunchUiInputTree> children) {
            return builder()
                    .meta(meta)
                    .children(children)
                    .build();
        }

        public static Builder builder() {
            return new AutoValue_LaunchUi_LaunchUiInputTree.Builder();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract Builder meta(final LaunchUiInputTreeMeta meta);

            public abstract Builder children(final Map<String, LaunchUiInputTree> children);

            public abstract LaunchUiInputTree build();
        }
    }

    @AutoValue
    public static abstract class LaunchUiInputTreeMeta {
        @Nullable @JsonProperty("label") public abstract String label();
        @Nullable @JsonProperty("description") public abstract String description();
        @JsonProperty("advanced") public abstract boolean advanced();
        @JsonProperty("required") public abstract boolean required();
        @JsonProperty("user-settable") public abstract boolean userSettable();

        public static LaunchUiInputTreeMeta create(final String label,
                                                   final String description,
                                                   final boolean advanced,
                                                   final boolean required,
                                                   final boolean userSettable) {
            return builder()
                    .label(label)
                    .description(description)
                    .advanced(advanced)
                    .required(required)
                    .userSettable(userSettable)
                    .build();
        }

        public static Builder builder() {
            return new AutoValue_LaunchUi_LaunchUiInputTreeMeta.Builder();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract Builder label(final String label);
            public abstract Builder description(final String description);
            public abstract Builder advanced(final boolean advanced);
            public abstract Builder required(final boolean required);
            public abstract Builder userSettable(final boolean userSettable);

            public abstract LaunchUiInputTreeMeta build();
        }
    }

    @AutoValue
    public static abstract class LaunchUiInputValueChildren {
        @JsonProperty("label") public abstract String label();
        @JsonProperty("children") public abstract Map<String, Map<String, LaunchUiInputValueChildren>> children();

        public static LaunchUiInputValueChildren create(final String label,
                                                        final Map<String, Map<String, LaunchUiInputValueChildren>> children) {
            return new AutoValue_LaunchUi_LaunchUiInputValueChildren(label, children);
        }
    }

    @AutoValue
    public static abstract class SingleLaunchUi extends LaunchUi {
        @JsonProperty("input-values") public abstract Map<String, Map<String, LaunchUiInputValueChildren>> inputValueTrees();

        public static SingleLaunchUi create(final PartiallyResolvedCommand partiallyResolvedCommand,
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
            public abstract Builder meta(LaunchUiMeta meta);
            public abstract Builder inputTrees(Map<String, LaunchUiInputTree> inputTrees);

            public abstract Builder inputValueTrees(final Map<String, Map<String, LaunchUiInputValueChildren>> inputValueTrees);

            public Builder populateInputTreeAndInputValueTreeFromResolvedInputTrees(final List<ResolvedInputTreeNode<? extends Input>> resolvedInputTrees,
                                                                                    final @Nonnull Map<String, CommandInputConfiguration> inputConfigurationMap) {
                final Map<String, LaunchUiInputTree> inputTrees = new HashMap<>();
                final Map<String, Map<String, LaunchUiInputValueChildren>> inputValueTrees = new HashMap<>();
                for (final ResolvedInputTreeNode<? extends Input> rootNode : resolvedInputTrees) {
                    final String inputName = rootNode.input().name();
                    if (log.isDebugEnabled()) {
                        log.debug("ROOT " + inputName + " - Populating input relationship tree.");
                    }
                    inputTrees.put(
                            inputName,
                            LaunchUi.convertResolvedInputTreeToLaunchUiInputTree(rootNode, inputConfigurationMap)
                    );

                    if (log.isDebugEnabled()) {
                        log.debug("ROOT " + inputName + " - Populating input value tree.");
                    }
                    inputValueTrees.put(
                            inputName,
                            LaunchUi.convertResolvedInputTreeToLaunchUiInputValueTree(rootNode)
                    );
                }
                return this
                        .inputTrees(inputTrees)
                        .inputValueTrees(inputValueTrees);
            }

            public abstract SingleLaunchUi build();
        }
    }

    @AutoValue
    public static abstract class BulkLaunchUi extends LaunchUi {
        @JsonProperty("input-values") public abstract List<Map<String, Map<String, LaunchUiInputValueChildren>>> listOfInputValueTrees();


        public static Builder builder() {
            return new AutoValue_LaunchUi_BulkLaunchUi.Builder();
        }

        @AutoValue.Builder
        public static abstract class Builder {

            public abstract Builder meta(LaunchUiMeta meta);
            public abstract Builder inputTrees(Map<String, LaunchUiInputTree> inputTrees);

            public abstract Builder listOfInputValueTrees(final List<Map<String, Map<String, LaunchUiInputValueChildren>>> listOfInputValueTrees);

            public Builder populateInputTreeAndInputValueTreeFromResolvedInputTrees(final List<List<ResolvedInputTreeNode<? extends Input>>> listOfResolvedInputTrees,
                                                                                    final @Nonnull Map<String, CommandInputConfiguration> inputConfigurationMap) {
                final Map<String, LaunchUiInputTree> inputTrees = new HashMap<>();
                final List<Map<String, Map<String, LaunchUiInputValueChildren>>> listOfInputValueTrees = new ArrayList<>();
                for (final List<ResolvedInputTreeNode<? extends Input>> resolvedInputTrees : listOfResolvedInputTrees) {
                    final Map<String, Map<String, LaunchUiInputValueChildren>> inputValueTrees = new HashMap<>();
                    for (final ResolvedInputTreeNode<? extends Input> rootNode : resolvedInputTrees) {
                        final String inputName = rootNode.input().name();
                        if (log.isDebugEnabled()) {
                            log.debug("ROOT " + inputName + " - Populating input relationship tree.");
                        }
                        inputTrees.put(
                                inputName,
                                LaunchUi.convertResolvedInputTreeToLaunchUiInputTree(rootNode, inputConfigurationMap)
                        );

                        if (log.isDebugEnabled()) {
                            log.debug("ROOT " + inputName + " - Populating input value tree.");
                        }
                        inputValueTrees.put(
                                inputName,
                                LaunchUi.convertResolvedInputTreeToLaunchUiInputValueTree(rootNode)
                        );
                    }
                    listOfInputValueTrees.add(inputValueTrees);
                }
                return this
                        .inputTrees(inputTrees)
                        .listOfInputValueTrees(listOfInputValueTrees);
            }
            public abstract BulkLaunchUi build();
        }
    }

    private static Map<String, LaunchUiInputValueChildren> convertResolvedInputTreeToLaunchUiInputValueTree(final @Nonnull ResolvedInputTreeNode<? extends Input> node) {

        final Map<String, LaunchUiInputValueChildren> thisInputsValuesAndChildren = new HashMap<>();
        for (final ResolvedInputTreeValueAndChildren valueAndChildren : node.valuesAndChildren()) {
            final ResolvedInputValue resolvedValue = valueAndChildren.resolvedValue();
            final String value = resolvedValue.value();
            String label = resolvedValue.valueLabel();

            if (log.isDebugEnabled()) {
                log.debug(node.input().name() + " - value \"" + value + "\" label \"" + label + "\"");
            }

            if (value == null) {
                log.debug("SKIPPING. value is null");
                continue;
            }

            if (label == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Setting null label equal to value \"" + value + "\"");
                }
                label = value;
            }

            final Map<String, Map<String, LaunchUiInputValueChildren>> children = new HashMap<>();
            for (final ResolvedInputTreeNode<? extends Input> child : valueAndChildren.children()) {
                if (Command.CommandInput.class.isAssignableFrom(child.input().getClass())) {
                    // This is a command input which can be derived from a wrapper input.
                    // We can safely skip it.
                    continue;
                }

                if (log.isDebugEnabled()) {
                    log.debug("Adding " + node.input().name() + " child " + child.input().name());
                }
                children.put(child.input().name(), convertResolvedInputTreeToLaunchUiInputValueTree(child));
            }

            final LaunchUiInputValueChildren thisInputsChildren = LaunchUiInputValueChildren.create(label, children);
            thisInputsValuesAndChildren.put(value, thisInputsChildren);

        }
        return thisInputsValuesAndChildren;
    }

    private static LaunchUiInputTree convertResolvedInputTreeToLaunchUiInputTree(final @Nonnull ResolvedInputTreeNode<? extends Input> node,
                                                                                 final @Nonnull Map<String, CommandInputConfiguration> inputConfigurationMap) {

        final Map<String, LaunchUiInputTree> children = new HashMap<>();
        for (final ResolvedInputTreeValueAndChildren valueAndChildren : node.valuesAndChildren()) {
            for (final ResolvedInputTreeNode<? extends Input> child : valueAndChildren.children()) {
                if (Command.CommandInput.class.isAssignableFrom(child.input().getClass())) {
                    // This is a command input which can be derived from a wrapper input.
                    // We can safely skip it.
                    continue;
                }

                if (children.containsKey(child.input().name())) {
                    // We're flattening out the resolved input tree; we don't care about the values.
                    // Since for each value we see the children again, we've likely already seen this child.
                    // So skip it.
                    continue;
                }

                if (log.isDebugEnabled()) {
                    log.debug("Adding " + node.input().name() + " child " + child.input().name());
                }
                children.put(child.input().name(), convertResolvedInputTreeToLaunchUiInputTree(child, inputConfigurationMap));
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

        return LaunchUiInputTree.builder()
                .meta(LaunchUiInputTreeMeta.builder()
                        .label(input.name()) // TODO add label to commandInput (pojo, hibernate, tests, and examples)
                        .description(input.description())
                        .required(inputIsRequired)
                        .userSettable(userSettable)
                        .advanced(advanced)
                        .build())
                .children(children)
                .build();
    }
}
