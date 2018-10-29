package org.nrg.containers.model.command.auto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.nrg.containers.model.container.auto.Container;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public abstract class LaunchReport {
    @JsonProperty("status") public abstract String status();
    @JsonProperty("params") public abstract ImmutableMap<String, String> launchParams();
    @Nullable @JsonProperty("id") public abstract Long databaseId();
    @Nullable @JsonProperty("command-id") public abstract Long commandId();
    @Nullable @JsonProperty("wrapper-id") public abstract Long wrapperId();

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = ContainerSuccess.class, name = "container"),
            @JsonSubTypes.Type(value = ServiceSuccess.class, name = "service")
    })
    public static abstract class Success extends LaunchReport {
        protected final static String STATUS = "success";
        @JsonProperty("type") public abstract String type();
    }

    @AutoValue
    public static abstract class ContainerSuccess extends Success {
        @JsonProperty("container-id") public abstract String containerId();

        @Override
        public String type() {
            return "container";
        }

        @JsonCreator
        @SuppressWarnings("unused")
        static ContainerSuccess create(@JsonProperty("container-id") final @Nonnull String containerId,
                                       @JsonProperty("status") final String ignoredStatus,
                                       @JsonProperty("params") final Map<String, String> launchParams,
                                       @JsonProperty("id") final Long databaseId,
                                       @JsonProperty("command-id") final Long commandId,
                                       @JsonProperty("wrapper-id") final Long wrapperId) {
            return create(containerId, launchParams, databaseId, commandId, wrapperId);
        }

        public static ContainerSuccess create(final @Nonnull Container container) {
            final Long databaseId = container.databaseId() == 0L ? null : container.databaseId();
            final Long commandId = container.commandId() == 0L ? null : container.commandId();
            final Long wrapperId = container.wrapperId() == 0L ? null : container.wrapperId();
            final String containerIdCouldBeNull = container.containerId();
            final String containerId = containerIdCouldBeNull != null ? containerIdCouldBeNull : ""; // This should not be null for swarmMode=false
            return create(containerId, container.getRawInputs(), databaseId, commandId, wrapperId);
        }

        public static ContainerSuccess create(final @Nonnull String containerId,
                                              final Map<String, String> launchParams,
                                              final Long databaseId,
                                              final Long commandId,
                                              final Long wrapperId) {
            final ImmutableMap<String, String> launchParamsCopy =
                    launchParams == null ?
                            ImmutableMap.<String, String>of() :
                            ImmutableMap.copyOf(launchParams);
            return new AutoValue_LaunchReport_ContainerSuccess(STATUS, launchParamsCopy, databaseId, commandId, wrapperId, containerId);
        }
    }

    @AutoValue
    public static abstract class ServiceSuccess extends Success {
        @JsonProperty("service-id") public abstract String serviceId();

        @Override
        public String type() {
            return "service";
        }

        @JsonCreator
        @SuppressWarnings("unused")
        static ServiceSuccess create(@JsonProperty("service-id") final @Nonnull String serviceId,
                                     @JsonProperty("status") final String ignoredStatus,
                                     @JsonProperty("params") final Map<String, String> launchParams,
                                     @JsonProperty("id") final Long databaseId,
                                     @JsonProperty("command-id") final Long commandId,
                                     @JsonProperty("wrapper-id") final Long wrapperId) {
            return create(serviceId, launchParams, databaseId, commandId, wrapperId);
        }

        public static ServiceSuccess create(final @Nonnull Container container) {
            final Long databaseId = container.databaseId() == 0L ? null : container.databaseId();
            final Long commandId = container.commandId() == 0L ? null : container.commandId();
            final Long wrapperId = container.wrapperId() == 0L ? null : container.wrapperId();
            final String serviceIdCouldBeNull = container.serviceId();
            final String serviceId = serviceIdCouldBeNull != null ? serviceIdCouldBeNull : ""; // This should not be null for swarmMode=true
            return create(serviceId, container.getRawInputs(), databaseId, commandId, wrapperId);
        }

        public static ServiceSuccess create(final @Nonnull String containerId,
                                            final Map<String, String> launchParams,
                                            final Long databaseId,
                                            final Long commandId,
                                            final Long wrapperId) {
            final ImmutableMap<String, String> launchParamsCopy =
                    launchParams == null ?
                            ImmutableMap.<String, String>of() :
                            ImmutableMap.copyOf(launchParams);
            return new AutoValue_LaunchReport_ServiceSuccess(STATUS, launchParamsCopy, databaseId, commandId, wrapperId, containerId);
        }
    }

    @AutoValue
    public static abstract class Failure extends LaunchReport {
        private final static String STATUS = "failure";
        @JsonProperty("message") public abstract String message();

        @JsonCreator
        @SuppressWarnings("unused")
        static Failure create(@JsonProperty("message") final @Nonnull String message,
                              @JsonProperty("status") final String ignoredStatus,
                              @JsonProperty("params") final Map<String, String> launchParams,
                              @JsonProperty("id") final Long databaseId,
                              @JsonProperty("command-id") final Long commandId,
                              @JsonProperty("wrapper-id") final Long wrapperId) {
            return create(message, launchParams, databaseId, commandId, wrapperId);
        }

        public static Failure create(final @Nonnull String message,
                                     final Map<String, String> launchParams,
                                     final long commandId,
                                     final long wrapperId) {
            final Long commandIdCopy = commandId == 0L ? null : commandId;
            final Long wrapperIdCopy = wrapperId == 0L ? null : wrapperId;
            return create(message, launchParams, 0L, commandIdCopy, wrapperIdCopy);
        }

        public static Failure create(final @Nonnull String message,
                                     final Map<String, String> launchParams,
                                     final Long databaseId,
                                     final Long commandId,
                                     final Long wrapperId) {
            final ImmutableMap<String, String> launchParamsCopy =
                    launchParams == null ?
                            ImmutableMap.<String, String>of() :
                            ImmutableMap.copyOf(launchParams);
            return new AutoValue_LaunchReport_Failure(STATUS, launchParamsCopy, databaseId, commandId, wrapperId, message);
        }
    }

    @AutoValue
    public abstract static class BulkLaunchReport {
        @JsonProperty("successes") public abstract ImmutableList<Success> successes();
        @JsonProperty("failures") public abstract ImmutableList<Failure> failures();

        public static Builder builder() {
            return new AutoValue_LaunchReport_BulkLaunchReport.Builder();
        }

        @JsonCreator
        public static BulkLaunchReport create(@JsonProperty("successes") final List<Success> successes,
                                              @JsonProperty("failures") final List<Failure> failures) {
            return builder()
                    .successes(successes)
                    .failures(failures)
                    .build();
        }

        @AutoValue.Builder
        public static abstract class Builder {
            public abstract Builder successes(List<Success> successes);
            abstract ImmutableList.Builder<Success> successesBuilder();
            public Builder addSuccess(final @Nonnull Success success) {
                successesBuilder().add(success);
                return this;
            }
            public abstract Builder failures(List<Failure> failures);
            abstract ImmutableList.Builder<Failure> failuresBuilder();
            public Builder addFailure(final @Nonnull Failure failure) {
                failuresBuilder().add(failure);
                return this;
            }

            public Builder addReport(final @Nonnull LaunchReport report) {
                if (Success.class.isAssignableFrom(report.getClass())) {
                    return addSuccess((Success)report);
                } else {
                    return addFailure((Failure)report);
                }
            }

            public abstract BulkLaunchReport build();
        }
    }
}
