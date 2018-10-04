package org.nrg.containers.model.command.auto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.nrg.containers.model.command.auto.Command.Input;
import org.nrg.containers.model.command.auto.ResolvedCommand.PartiallyResolvedCommand;
import org.nrg.containers.model.command.auto.ResolvedInputTreeNode.ResolvedInputTreeValueAndChildren;
import org.nrg.containers.model.command.entity.CommandInputEntity.Type;
import org.nrg.containers.model.configuration.CommandConfiguration;
import org.nrg.containers.model.configuration.CommandConfiguration.CommandInputConfiguration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public abstract class LaunchUi {
    @JsonProperty("command-id") public abstract Long commandId();
    @JsonProperty("command-name") public abstract String commandName();
    @Nullable @JsonProperty("command-description") public abstract String commandDescription();
    @JsonProperty("wrapper-id") public abstract Long wrapperId();
    @JsonProperty("wrapper-name") public abstract String wrapperName();
    @Nullable @JsonProperty("wrapper-description") public abstract String wrapperDescription();
    @JsonProperty("image-name") public abstract String imageName();
    @JsonProperty("image-type") public abstract String imageType();

    @AutoValue
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static abstract class SingleLaunchUi extends LaunchUi {
        @JsonProperty("inputs") public abstract ImmutableMap<String, LaunchUiInput> inputs();

        @JsonCreator
        public static SingleLaunchUi create(@JsonProperty("command-id") final Long commandId,
                                            @JsonProperty("command-name") final String commandName,
                                            @JsonProperty("command-description") final String commandDescription,
                                            @JsonProperty("wrapper-id") final Long wrapperId,
                                            @JsonProperty("wrapper-name") final String wrapperName,
                                            @JsonProperty("wrapper-description") final String wrapperDescription,
                                            @JsonProperty("image-name") final String imageName,
                                            @JsonProperty("image-type") final String imageType,
                                            @JsonProperty("inputs") final Map<String, LaunchUiInput> inputs) {
            return builder()
                    .commandId(commandId)
                    .commandName(commandName)
                    .commandDescription(commandDescription)
                    .wrapperId(wrapperId)
                    .wrapperName(wrapperName)
                    .wrapperDescription(wrapperDescription)
                    .imageName(imageName)
                    .imageType(imageType)
                    .inputs(inputs)
                    .build();
        }

        public static SingleLaunchUi create(final PartiallyResolvedCommand partiallyResolvedCommand,
                                            final CommandConfiguration commandConfiguration) {
            return builder()
                    .commandId(partiallyResolvedCommand.commandId())
                    .commandName(partiallyResolvedCommand.commandName())
                    .commandDescription(partiallyResolvedCommand.commandDescription())
                    .wrapperId(partiallyResolvedCommand.wrapperId())
                    .wrapperName(partiallyResolvedCommand.wrapperName())
                    .wrapperDescription(partiallyResolvedCommand.wrapperDescription())
                    .imageName(partiallyResolvedCommand.image())
                    .imageType(partiallyResolvedCommand.type())
                    .addInputsFromInputTrees(partiallyResolvedCommand, commandConfiguration)
                    .build();
        }

        public static Builder builder() {
            return new AutoValue_LaunchUi_SingleLaunchUi.Builder();
        }

        @AutoValue.Builder
        public static abstract class Builder {

            public abstract Builder commandId(Long commandId);
            public abstract Builder commandName(String commandName);
            public abstract Builder commandDescription(String commandDescription);
            public abstract Builder wrapperId(Long wrapperId);
            public abstract Builder wrapperName(String wrapperName);
            public abstract Builder wrapperDescription(String wrapperDescription);
            public abstract Builder imageName(String imageName);
            public abstract Builder imageType(String imageType);
            public abstract Builder inputs(Map<String, LaunchUiInput> inputs);
            abstract ImmutableMap.Builder<String, LaunchUiInput> inputsBuilder();
            public Builder addInputsFromInputTrees(final PartiallyResolvedCommand partiallyResolvedCommand,
                                                   final CommandConfiguration commandConfiguration) {
                // We have to go through the resolved input trees and get their values into the flat structure needed by the UI.
                final Map<String, LaunchUiInput.Builder> inputBuilderMap = Maps.newHashMap();
                for (final ResolvedInputTreeNode<? extends Input> rootNode : partiallyResolvedCommand.resolvedInputTrees()) {
                    addNodesToInputMap(rootNode, null, null, commandConfiguration.inputs(), inputBuilderMap);
                }
                for (final Map.Entry<String, LaunchUiInput.Builder> inputBuilderEntry : inputBuilderMap.entrySet()) {
                    inputsBuilder().put(inputBuilderEntry.getKey(), inputBuilderEntry.getValue().build());
                }
                return this;
            }
            public abstract SingleLaunchUi build();
        }
    }

    @AutoValue
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static abstract class BulkLaunchUi extends LaunchUi {
        @JsonProperty("inputs") public abstract ImmutableList<ImmutableMap<String, LaunchUiInput>> inputs();

        public static BulkLaunchUi.Builder builder(final PartiallyResolvedCommand partiallyResolvedCommand,
                                                   final CommandConfiguration commandConfiguration) {
            return builder()
                    .commandId(partiallyResolvedCommand.commandId())
                    .commandName(partiallyResolvedCommand.commandName())
                    .commandDescription(partiallyResolvedCommand.commandDescription())
                    .wrapperId(partiallyResolvedCommand.wrapperId())
                    .wrapperName(partiallyResolvedCommand.wrapperName())
                    .wrapperDescription(partiallyResolvedCommand.wrapperDescription())
                    .imageName(partiallyResolvedCommand.image())
                    .imageType(partiallyResolvedCommand.type())
                    .addInputsFromInputTrees(partiallyResolvedCommand, commandConfiguration);
        }

        public static Builder builder() {
            return new AutoValue_LaunchUi_BulkLaunchUi.Builder();
        }

        @AutoValue.Builder
        public static abstract class Builder {

            public abstract Builder commandId(Long commandId);
            public abstract Builder commandName(String commandName);
            public abstract Builder commandDescription(String commandDescription);
            public abstract Builder wrapperId(Long wrapperId);
            public abstract Builder wrapperName(String wrapperName);
            public abstract Builder wrapperDescription(String wrapperDescription);
            public abstract Builder imageName(String imageName);
            public abstract Builder imageType(String imageType);
            public abstract Builder inputs(List<ImmutableMap<String, LaunchUiInput>> inputs);
            abstract ImmutableList.Builder<ImmutableMap<String, LaunchUiInput>> inputsBuilder();
            public Builder addInputsFromInputTrees(final PartiallyResolvedCommand partiallyResolvedCommand,
                                                   final CommandConfiguration commandConfiguration) {
                // We have to go through the resolved input trees and get their values into the flat structure needed by the UI.
                log.debug("Building launch UI from input trees.");
                final Map<String, LaunchUiInput.Builder> inputBuilderMap = Maps.newHashMap();
                for (final ResolvedInputTreeNode<? extends Input> rootNode : partiallyResolvedCommand.resolvedInputTrees()) {
                    addNodesToInputMap(rootNode, null, null, commandConfiguration.inputs(), inputBuilderMap);
                }

                final ImmutableMap.Builder<String, LaunchUiInput> inputBuilder = ImmutableMap.builder();
                for (final Map.Entry<String, LaunchUiInput.Builder> inputBuilderEntry : inputBuilderMap.entrySet()) {
                    inputBuilder.put(inputBuilderEntry.getKey(), inputBuilderEntry.getValue().build());
                }
                inputsBuilder().add(inputBuilder.build());
                return this;
            }
            public abstract BulkLaunchUi build();
        }
    }

    private static void addNodesToInputMap(final @Nonnull ResolvedInputTreeNode<? extends Input> node,
                                           final @Nullable String parentName,
                                           final @Nullable String parentValue,
                                           final @Nonnull Map<String, CommandInputConfiguration> inputConfigurationMap,
                                           final @Nonnull Map<String, LaunchUiInput.Builder> inputMap) {
        final Input commandInput = node.input();
        final Class<? extends Input> inputClass = commandInput.getClass();
        final String inputName = commandInput.name();
        final CommandInputConfiguration inputConfiguration =
                inputConfigurationMap.containsKey(inputName) && inputConfigurationMap.get(inputName) != null ?
                        inputConfigurationMap.get(inputName) :
                        CommandInputConfiguration.builder().build();

        final List<LaunchUiInputValue> valueList = new ArrayList<>();
        final Set<String> childNames = new HashSet<>();
        for (final ResolvedInputTreeValueAndChildren valueAndChildren : node.valuesAndChildren()) {
            final ResolvedInputValue resolvedValue = valueAndChildren.resolvedValue();
            final String value = resolvedValue.value();

            // Add all children to the map, using this node's value as their parent value
            for (final ResolvedInputTreeNode<? extends Input> child : valueAndChildren.children()) {
                childNames.add(child.input().name());
                addNodesToInputMap(child, commandInput.name(), value, inputConfigurationMap, inputMap);
            }

            // Now add this node's value to the uiInput's values
            if (value == null) {
                valueList.add(LaunchUiInputValue.createNull());
            } else {
                valueList.add(LaunchUiInputValue.create(value, resolvedValue.valueLabel()));
            }
        }
        // TODO figure this out from... I don't know from what
        final UiType uiType;
        final String inputType = commandInput.type();
        final boolean inputIsRequired = commandInput.required() || (inputConfiguration.required() != null && inputConfiguration.required());
        final boolean multipleValues = valueList.size() > 1;
        final boolean noValues = !multipleValues && (valueList.size() == 0 || valueList.get(0).equals(LaunchUiInputValue.createNull()));
        final boolean userSettable = inputConfiguration.userSettable() == null || inputConfiguration.userSettable(); // default: true
        final boolean advanced = inputConfiguration.advanced() != null && inputConfiguration.advanced(); // default: false

        final String uiTypeMsgTemplate = "%s because %s";
        final String uiTypeMsg;
        if (!userSettable) {
            uiType = UiType.STATIC;
            uiTypeMsg = String.format(uiTypeMsgTemplate, uiType.getName(), "userSettable = false");
        } else {
            // The user should be able to set this input
            if (noValues) {
                uiType = UiType.TEXT;
                uiTypeMsg = String.format(uiTypeMsgTemplate, uiType.getName(), "userSettable = true and noValues = true");
            } else if (multipleValues) {
                uiType = UiType.SELECT;
                uiTypeMsg = String.format(uiTypeMsgTemplate, uiType.getName(), "userSettable = true and multipleValues = true");
            } else {
                // We have a single value
                // If it is a boolean, number, or string, apply those types.
                if (inputType.equals(Type.BOOLEAN.getName())) {
                    uiType = UiType.BOOLEAN;
                    uiTypeMsg = String.format(uiTypeMsgTemplate, uiType.getName(), "userSettable = true and inputType = " + inputType);
                } else if (inputType.equals(Type.NUMBER.getName())) {
                    uiType = UiType.NUMBER;
                    uiTypeMsg = String.format(uiTypeMsgTemplate, uiType.getName(), "userSettable = true and inputType = " + inputType);
                } else if (inputType.equals(Type.STRING.getName())) {
                    uiType = UiType.TEXT;
                    uiTypeMsg = String.format(uiTypeMsgTemplate, uiType.getName(), "userSettable = true and inputType = " + inputType);
                } else {
                    // If the input is not one of the simple types above,
                    // it is some XNAT type. But if we only have one value, that
                    // implies either it was given to us (therefore don't change it)
                    // or it was derived (therefore don't change it).
                    uiType = UiType.STATIC;
                    uiTypeMsg = String.format(uiTypeMsgTemplate, uiType.getName(), "userSettable = true and inputType is not boolean, number, or string. This is the fallback");
                }
            }
        }
        log.debug("Input {} - parent {}={}, inputType {}, required {}, noValue {}, multipleValues {}, userSettable {}, advanced {}, uiType {}",
                inputName, parentName, parentValue, inputType, inputIsRequired, noValues, multipleValues, userSettable, advanced, uiTypeMsg);

        // Hang on a second... do we even need this input?
        // If it is a command input and it is completely determined by its parent, then just skip it.
        if (Command.CommandInput.class.isAssignableFrom(inputClass) && parentName != null && parentValue != null && !noValues && !multipleValues) {
            log.debug("SKIPPING. This is a command input with a value derived from a parent. We don't need it.");
            return;
        }

        // Get the input builder if it exists, or make a new one if it doesn't
        final LaunchUiInput.Builder uiInputBuilder;
        if (inputMap.containsKey(commandInput.name())) {
            uiInputBuilder = inputMap.get(commandInput.name());
        } else {
            uiInputBuilder = LaunchUiInput.builder()
                    .label(commandInput.name()) // TODO add label to commandInput (pojo, hibernate, tests, and examples)
                    .description(commandInput.description())
                    .required(inputIsRequired)
                    .userSettable(userSettable)
                    .advanced(advanced)
                    .parent(parentName)
                    .children(childNames);
            inputMap.put(commandInput.name(), uiInputBuilder);
        }
        uiInputBuilder.addUi(parentValue == null ? "default" : parentValue,
                LaunchUiInputValuesAndType.create(valueList, uiType));
    }

    @AutoValue
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static abstract class LaunchUiInput {
        @JsonProperty("label") public abstract String label();
        @Nullable @JsonProperty("description") public abstract String description();
        @JsonProperty("advanced") public abstract Boolean advanced();
        @JsonProperty("required") public abstract Boolean required();
        @JsonProperty("user-settable") public abstract Boolean userSettable();
        @Nullable @JsonProperty("parent") public abstract String parent();
        @JsonProperty("children") public abstract ImmutableList<String> children();
        @JsonProperty("ui") public abstract ImmutableMap<String, LaunchUiInputValuesAndType> ui();

        public static Builder builder() {
            return new AutoValue_LaunchUi_LaunchUiInput.Builder();
        }

        @AutoValue.Builder
        public static abstract class Builder {
            public abstract Builder label(String label);
            public abstract Builder description(String description);
            public abstract Builder advanced(Boolean advanced);
            public abstract Builder required(Boolean required);
            public abstract Builder userSettable(Boolean userSettable);
            public abstract Builder parent(String parent);
            public abstract Builder children(List<String> children);
            public Builder children(Set<String> children) {
                for (final String child : children) {
                    addChild(child);
                }
                return this;
            }
            abstract ImmutableList.Builder<String> childrenBuilder();
            public Builder addChild(final String childName) {
                childrenBuilder().add(childName);
                return this;
            }
            public abstract Builder ui(Map<String, LaunchUiInputValuesAndType> ui);
            abstract ImmutableMap.Builder<String, LaunchUiInputValuesAndType> uiBuilder();
            public Builder addUi(final @Nonnull String parentValue,
                                 final @Nonnull LaunchUiInputValuesAndType valuesAndType) {
                uiBuilder().put(parentValue, valuesAndType);
                return this;
            }

            public abstract LaunchUiInput build();
        }
    }

    @AutoValue
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static abstract class LaunchUiInputValuesAndType {
        @JsonProperty("values") public abstract ImmutableList<LaunchUiInputValue> values();
        @JsonProperty("type") public abstract UiType type();

        public static LaunchUiInputValuesAndType create(final List<LaunchUiInputValue> values,
                                                        final UiType type) {
            final ImmutableList<LaunchUiInputValue> valuesCopy =
                    values == null ?
                            ImmutableList.<LaunchUiInputValue>of() :
                            ImmutableList.copyOf(values);
            return new AutoValue_LaunchUi_LaunchUiInputValuesAndType(valuesCopy, type);
        }
    }

    @AutoValue
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static abstract class LaunchUiInputValue {
        private static String NULL_VALUE = "";
        private static String NULL_LABEL = "null";

        @JsonProperty("value") public abstract String value();
        @JsonProperty("label") public abstract String label();

        @JsonIgnore
        public boolean isNull() {
            return value().equals(NULL_VALUE) && label().equals(NULL_LABEL);
        }

        public static LaunchUiInputValue create(final @Nonnull String value,
                                                final String label) {
            return new AutoValue_LaunchUi_LaunchUiInputValue(
                    value, label == null ? value : label);
        }

        public static LaunchUiInputValue createNull() {
            return new AutoValue_LaunchUi_LaunchUiInputValue(NULL_VALUE, NULL_LABEL);
        }
    }

    public enum UiType {
        DEFAULT("default"),
        TEXT("text"),
        NUMBER("number"),
        BOOLEAN("boolean"),
        SELECT("select"),
        HIDDEN("hidden"),
        STATIC("static");

        public final String name;

        @JsonCreator
        UiType(final String name) {
            this.name = name;
        }

        @JsonValue
        public String getName() {
            return name;
        }
    }
}
