package org.nrg.containers.model.container.auto;

import com.google.auto.value.AutoValue;
import com.spotify.docker.client.messages.swarm.ContainerStatus;
import com.spotify.docker.client.messages.swarm.Task;
import com.spotify.docker.client.messages.swarm.TaskStatus;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Pattern;

@AutoValue
public abstract class ServiceTask {
    private static final Pattern successStatusPattern = Pattern.compile(TaskStatus.TASK_STATE_COMPLETE);
    private static final Pattern exitStatusPattern = Pattern.compile(
            StringUtils.join(
                    Arrays.asList(TaskStatus.TASK_STATE_FAILED, TaskStatus.TASK_STATE_COMPLETE,
                            TaskStatus.TASK_STATE_REJECTED, TaskStatus.TASK_STATE_SHUTDOWN), '|'));
    private static final Pattern hasNotStartedPattern = Pattern.compile(
            StringUtils.join(
                    Arrays.asList(TaskStatus.TASK_STATE_NEW, TaskStatus.TASK_STATE_ALLOCATED, TaskStatus.TASK_STATE_PENDING,
                            TaskStatus.TASK_STATE_ASSIGNED, TaskStatus.TASK_STATE_ACCEPTED, TaskStatus.TASK_STATE_PREPARING,
                            TaskStatus.TASK_STATE_READY, TaskStatus.TASK_STATE_STARTING), '|'));

    public abstract String serviceId();
    public abstract String taskId();
    @Nullable public abstract String nodeId();
    public abstract Boolean badState();
    public abstract String status();
    @Nullable public abstract Date statusTime();
    @Nullable public abstract String containerId();
    @Nullable public abstract String message();
    @Nullable public abstract String err();
    @Nullable public abstract Long exitCode();

    public static ServiceTask create(final @Nonnull Task task, final String serviceId) {
        final ContainerStatus containerStatus = task.status().containerStatus();
        Long exitCode = containerStatus == null ? null : containerStatus.exitCode();
        // Bad state occurs when node is terminated while service still trying to run there
        // Criteria:    current state = [not an exit status] AND either desired state = shutdown OR exit code = -1
        //              OR current state = shutdown and exit code = 137
        String curState = task.status().state();
        boolean badState = (!isExitStatus(curState) &&
                (task.desiredState().equals(TaskStatus.TASK_STATE_SHUTDOWN) || (exitCode != null && exitCode < 0))) ||
                curState.equals(TaskStatus.TASK_STATE_SHUTDOWN) && exitCode != null && exitCode.equals(137L);
        return ServiceTask.builder()
                .serviceId(serviceId)
                .taskId(task.id())
                .nodeId(task.nodeId())
                .status(task.status().state())
                .badState(badState)
                .statusTime(task.status().timestamp())
                .message(task.status().message())
                .err(task.status().err())
                .exitCode(exitCode)
                .containerId(containerStatus == null ? null : containerStatus.containerId())
                .build();
    }

    public boolean isExitStatus() {
        final String status = status();
        return isExitStatus(status);
    }

    public static boolean isExitStatus(String status){
        return status != null && exitStatusPattern.matcher(status).matches();
    }
    
    public static boolean isSuccessfulStatus(String status){
        return status != null && successStatusPattern.matcher(status).matches();
    }
    
    public boolean isSuccessfulStatus(){
        final String status = status();
        return isSuccessfulStatus(status);
    }

    public boolean hasNotStarted() {
        final String status = status();
        return status == null || hasNotStartedPattern.matcher(status).matches();
    }

    public static Builder builder() {
        return new AutoValue_ServiceTask.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder serviceId(final String serviceId);
        public abstract Builder taskId(final String taskId);
        public abstract Builder containerId(final String containerId);
        public abstract Builder nodeId(final String nodeId);
        public abstract Builder status(final String status);
        public abstract Builder badState(final Boolean badState);
        public abstract Builder statusTime(final Date statusTime);
        public abstract Builder message(final String message);
        public abstract Builder err(final String err);
        public abstract Builder exitCode(final Long exitCode);

        public abstract ServiceTask build();
    }
}
