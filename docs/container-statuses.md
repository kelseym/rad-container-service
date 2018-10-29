

# Container Statuses

When the container service launches a container, that container goes through a life cycle. It is created, then started, it runs for some amount of time, then it is done. This document will explain how those states are tracked, both by docker (currently the only supported container host) and by the container service.

## What information do we have?

The container service tracks information about the containers it has launched in the XNAT database. This information is accessible though the `/xapi/containers` and `/xapi/containers/{id}` REST APIs. Many of the fields pertain to what was launched, like `inputs` and `command-line` and so forth. On these container objects, you can find a `status` field, and a `status-time`. These tell you the most recent known status of the container, and when it was recorded. I'll go into more detail below about where that information comes from.

In addition to these fields we also keep track of every observed change to the container's state. This can be found in a list called `history` on the container API object. These are the fields we record whenever we observe a change in container state:

* **`id`** The history objects are stored in a database table, and they have a unique identifier.
* **`status`** A string identifying the status the container is in after the status change. We will go into more detail below about the different values this can take.
* **`time-recorded`** The time when the container service recorded the change in status.
* **`external-timestamp`** If the container service received information about this status change from docker, this is the time docker reported that the status changed. It will always be earlier than the **`time-recorded`** because docker will know about the change before container service does.
* **`exitCode`** if the event indicates the container has finished, this field holds the exit code that was reported.
* **`entity-type`** is used to record what kind of thing caused the status change. The container service sets the `entity-type` to
    * `system` if the status change comes from the container service itself. This is the case when, for instance, an XNAT event is responsible for launching the container.
    * `event` if the status change information comes from docker's events API. This will be the `entity-type` for almost all status changes when *not* using swarm mode.
    * `service` if the status change information comes from docker's swarm service API. This will be the `entity-type` for almost all status changes when using swarm mode.
    * `user` if the status change comes from an explict action taken by an XNAT user. This is the `entity-type` used to record a container launch, but also if a user instructs the container service to kill a running container.
* **`entity-id`** is usually `null`. The only time it is non-null is when the `entity-type` is `user`; then the `entity-id` holds the XNAT user id of the user who took the action causing the status change.
* **`message`** is usually `null`. This is set when the `entity-type` is `system`, which allows a way to record the cause of the status change. For instance, if something goes wrong after the container is finished and the container service is uploading files, the container service will add a history item with `status=Failed` and a `message` describing what went wrong.

When new information on a container's status comes in, the container service will set the top-level `status` and `status-time` fields to the values of the most recent history item's `status` and `time-recorded` fields, respectively.

## How do we get this information?

The container service has a scheduled task within XNAT to update the status of the containers. Every ten seconds, the container service checks with docker using their events API (non-swarm) or services API (swarm). In the case of the events API, we receive all the events (status changes) that have happened since the last time we asked; we can record every change in the container's status, even if it happened when we weren't looking. In the services API, we only get the current status. If that has changed, we record it. But we cannot tell if there were intermediate changes that happened when we weren't looking.

When the status changes are retreived from docker, they are converted to container service objects and thrown onto the XNAT event bus. The container service implements a listener for these events, which records them. But the event service will provide an API enabling you to use these container status events as a trigger to take other actions. For instance, you could make an event trigger to send an email whenever a container finishes.

## What are the possible statuses?

The set of all possible `status` values is pretty large. In practice we will only ever see a subset.

This is the general flow of how status values get from docker to the container service:

1. Read the status from docker. See [How do we get this information?](#how-do-we-get-this-information) for more.
2. Throw a container event onto the event bus.
3. Catch the container event.
4. Create a container history item from the event. This involves mapping some of the status values into a friendlier set (see below). All other status values are recorded as-is from docker.
5. Save the container history item.

To know what all the possible `status` values are, let us first consult docker's API documentation what values they could return to us when we ask. The statuses below come from the swagger.yaml API documentation in the most recent docker master branch commit as of this writing, [moby/moby:cc7cda1](https://github.com/moby/moby/tree/cc7cda1968062fc6dde28c93e85a0192d58b6d90).

When we get the status of a service, we actually make a call to get the "task" that is run by the swarm master to satisfy the service spec. Currently the only kind of task that is supported is a container task, so this task object is effectively the container. The status values are defined as an enum called `TaskState` (see [swagger.yaml line 2845](https://github.com/moby/moby/blob/cc7cda1968062fc6dde28c93e85a0192d58b6d90/api/swagger.yaml#L2845)):

    new, allocated, pending, assigned, accepted, preparing, ready, starting, running, complete, shutdown, failed, rejected, remove, orphaned

The docker events API can report these `status` values for container events (see [swagger.yaml line 7051](https://github.com/moby/moby/blob/cc7cda1968062fc6dde28c93e85a0192d58b6d90/api/swagger.yaml#L7051)):

    attach, commit, copy, create, destroy, detach, die, exec_create, exec_detach, exec_start, exec_die, export, health_status, kill, oom, pause, rename, resize, restart, start, stop, top, unpause, and update

As of steps 1, 2, and 3 above, the status values of container events are drawn from this set. (As a corollary, if any custom event code handles these container events, they will be dealing with these "raw" unmapped status values.) Upon catching the event and creating the container history item, some of those status values are mapped to a friendlier set. The mapping is in this table below.

| Docker status | Container service status |
|:--------------|:-------------------------|
| complete      | Complete                 |
| created       | Created                  |
| rejected      | Failed                   |
| failed        | Failed                   |
| start         | Running                  |
| started       | Running                  |
| running       | Running                  |
| kill          | Killed                   |
| oom           | Killed (Out of Memory)   |
| starting      | Starting                 |

One more note about the possible status values. When we see the status values `kill`, `die`, or `oom` on a container, or `complete`, `shutdown`, `failed`, or `rejected` on a service, the container service code considers this event to indicate that the container is finished. It proceeds with "finalization".

During finalization the container service does things like store the container logs and upload the output files. But it also sets the final status to `Complete` or `Failed` depending on the value of the `exitCode`. If `exitCode` is zero or null, the container is considered to be a success and the status is `Complete`; otherwise it is a failure and the status is `Failed`.

## Final set of possible statuses

These are all the possible values of `status` in container history and on the container objects. It is the set of container service statuses, plus the set of service statuses, plus the set of container event statuses, minus the raw statuses we map to container service names.

    Created, Running, Complete, Failed, Killed, Killed (Out of Memory), Starting, attach, commit, copy, create, destroy, detach, die, exec_create, exec_detach, exec_start, exec_die, export, health_status, pause, rename, resize, restart, stop, top, unpause, update, new, allocated, pending, assigned, accepted, preparing, ready, shutdown, remove, orphaned

In practice, this is the subset you will see most often.

    Created, Running, Complete, Failed, die

The first four are the main life cycle of a container. The `die` event is very common, but it stored as-is from docker. Why don't we translate it into one of the friendlier container service states? The reason is that, until we finalize the container, we have not checked the `exitCode`; thus we do not know if the `die` event corresponds to a success or a failure.
