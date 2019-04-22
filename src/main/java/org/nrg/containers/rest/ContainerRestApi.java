package org.nrg.containers.rest;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.nrg.containers.exceptions.ContainerException;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoDockerServerException;
import org.nrg.containers.model.configuration.PluginVersionCheck;
import org.nrg.containers.model.container.auto.Container;
import org.nrg.containers.services.ContainerService;
import org.nrg.framework.annotations.XapiRestController;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.xapi.exceptions.InsufficientPrivilegesException;
import org.nrg.xapi.rest.AbstractXapiRestController;
import org.nrg.xapi.rest.ProjectId;
import org.nrg.xapi.rest.XapiRequestMapping;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.security.services.RoleHolder;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xft.security.UserI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.nrg.xdat.security.helpers.AccessLevel.Admin;
import static org.nrg.xdat.security.helpers.AccessLevel.Authenticated;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Slf4j
@XapiRestController
@RequestMapping()
public class ContainerRestApi extends AbstractXapiRestController {

    private static final String JSON = MediaType.APPLICATION_JSON_UTF8_VALUE;
    private static final String TEXT = MediaType.TEXT_PLAIN_VALUE;
    private static final String ZIP = "application/zip";
    private static final String ATTACHMENT_DISPOSITION = "attachment; filename=\"%s.%s\"";

    private ContainerService containerService;

    @Autowired
    public ContainerRestApi(final ContainerService containerService,
                            final UserManagementServiceI userManagementService,
                            final RoleHolder roleHolder) {
        super(userManagementService, roleHolder);
        this.containerService = containerService;
    }

    @XapiRequestMapping(value = "/containers/version", method = GET, restrictTo = Authenticated)
    @ApiOperation(value = "Check XNAT Version compatibility.")
    @ResponseBody
    public PluginVersionCheck versionCheck() {
        return containerService.checkXnatVersion();
    }

    @XapiRequestMapping(value = "/containers", method = GET, restrictTo = Authenticated)
    @ApiOperation(value = "Get all Containers")
    @ResponseBody
    public List<Container> getAll(final @RequestParam(required = false) Boolean nonfinalized) {
        return Lists.transform(containerService.getAll(nonfinalized), new Function<Container, Container>() {
            @Override
            public Container apply(final Container input) {
                return scrubPasswordEnv(input);
            }
        });
    }

    @XapiRequestMapping(value = "/projects/{project}/containers", method = GET, restrictTo = Authenticated)
    @ApiOperation(value = "Get all Containers by project")
    @ResponseBody
    public List<Container> getAll(final @PathVariable @ProjectId String project,
                                  final @RequestParam(required = false) Boolean nonfinalized) {
        return Lists.transform(containerService.getAll(nonfinalized, project), new Function<Container, Container>() {
            @Override
            public Container apply(final Container input) {
                return scrubPasswordEnv(input);
            }
        });
    }

    @XapiRequestMapping(value = "/containers/{id}", method = GET)
    @ApiOperation(value = "Get Containers by database ID")
    @ResponseBody
    public Container get(final @PathVariable String id) throws NotFoundException {
        return scrubPasswordEnv(containerService.get(id));
    }

    @XapiRequestMapping(value = "/containers/{id}", method = DELETE)
    @ApiOperation(value = "Get Container by container server ID")
    public ResponseEntity<Void> delete(final @PathVariable String id) {
        containerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @XapiRequestMapping(value = "/containers/{id}/finalize", method = POST, produces = JSON, restrictTo = Admin)
    @ApiOperation(value = "Finalize Container")
    public void finalize(final @PathVariable String id) throws NotFoundException, ContainerException, DockerServerException, NoDockerServerException {
        final UserI userI = XDAT.getUserDetails();
        containerService.finalize(id, userI);
    }

    @XapiRequestMapping(value = "/containers/{id}/kill", method = POST)
    @ApiOperation(value = "Kill Container")
    @ResponseBody
    public String kill(final @PathVariable String id)
            throws NotFoundException, NoDockerServerException, DockerServerException {
        final UserI userI = XDAT.getUserDetails();
        return containerService.kill(id, userI);
    }

    private Container scrubPasswordEnv(final Container container) {
        final Map<String, String> scrubbedEnvironmentVariables = Maps.newHashMap();
        for (final Map.Entry<String, String> env : container.environmentVariables().entrySet()) {
            scrubbedEnvironmentVariables.put(env.getKey(),
                    env.getKey().equals("XNAT_PASS") ? "******" : env.getValue());
        }
        return container.toBuilder().environmentVariables(scrubbedEnvironmentVariables).build();
    }

    @XapiRequestMapping(value = "/containers/{containerId}/logs", method = GET)
    @ApiOperation(value = "Get Container logs",
            notes = "Return stdout and stderr logs as a zip")
    public void getLogs(final @PathVariable String containerId,
                        final HttpServletResponse response)
            throws IOException, InsufficientPrivilegesException, NoDockerServerException, DockerServerException, NotFoundException {
        final Map<String, InputStream> logStreams = containerService.getLogStreams(containerId);

        try(final ZipOutputStream zipStream = new ZipOutputStream(response.getOutputStream()) ) {
            for(final String streamName : logStreams.keySet()){
                final InputStream inputStream = logStreams.get(streamName);
                final ZipEntry entry = new ZipEntry(streamName);
                try {
                    zipStream.putNextEntry(entry);
                    writeToOuputStream(inputStream, zipStream);
                } catch (IOException e) {
                    log.error("There was a problem writing %s to the zip. " + e.getMessage(), streamName);
                }
            }

            response.setStatus(HttpStatus.OK.value());
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, getAttachmentDisposition(containerId, "zip"));
            response.setHeader(HttpHeaders.CONTENT_TYPE, ZIP);
        } catch (IOException e) {
            log.error("There was a problem opening the zip stream.", e);
        }

        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @XapiRequestMapping(value = "/containers/{containerId}/logs/{file}", method = GET)
    @ApiOperation(value = "Get Container logs", notes = "Return either stdout or stderr logs")
    @ResponseBody
    public ResponseEntity<String> getLog(final @PathVariable String containerId,
                                         final @PathVariable @ApiParam(allowableValues = "stdout, stderr") String file)
            throws NoDockerServerException, DockerServerException, NotFoundException, IOException {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, getAttachmentDisposition(containerId + "-" + file, "log"))
                .header(HttpHeaders.CONTENT_TYPE, TEXT)
                .body(doGetLog(containerId, file, null, String.class));
    }

    @XapiRequestMapping(value = "/containers/{containerId}/logSince/{file}", method = GET)
    @ApiOperation(value = "Get Container logs", notes = "Return either stdout or stderr logs")
    @ResponseBody
    public ResponseEntity<Map> pollLog(final @PathVariable String containerId,
                                          final @PathVariable @ApiParam(allowableValues = "stdout, stderr") String file,
                                          final @RequestParam(required = false) Long since)
            throws NoDockerServerException, DockerServerException, NotFoundException, IOException {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, JSON)
                .body(doGetLog(containerId, file, since, Map.class));
    }

    private <T> T doGetLog(String containerId, String file, Long since, Class<T> type)
            throws NotFoundException, IOException {

        final UserI user = XDAT.getUserDetails();
        Integer sinceInt = null;
        try {
            sinceInt = since == null ? null : Math.toIntExact(since);
        } catch (ArithmeticException e) {
            log.error("Unable to convert since parameter to integer", e);
        }

        long queryTime = System.currentTimeMillis() / 1000L;
        final InputStream logStream = containerService.getLogStream(containerId, file, true, sinceInt);

        String logContent;
        long lastTime = -1;
        if (logStream == null) {
            logContent = "";
        } else {
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            writeToOuputStream(logStream, byteArrayOutputStream);
            logContent = byteArrayOutputStream.toString(StandardCharsets.UTF_8.name());
            if (!(logStream instanceof FileInputStream) &&
                    !containerService.isFailedOrComplete(containerService.get(containerId), user)) {
                // If it's not a file and the container/service is still active (aka still potentially logging),
                // determine what to pass for "since" querying based on timestamps
                String[] lines = logContent.replaceAll("(\\n)+$", "").split("\\n");
                String lastLine;
                try {
                    if (lines.length == 0 ||
                            StringUtils.isBlank(lastLine = lines[lines.length-1].replaceAll(" .*",""))) {
                        throw new ParseException(null, 0);
                    }
                    lastTime = Instant.parse(lastLine).plus(1L, ChronoUnit.SECONDS).getEpochSecond();
                } catch (ParseException e) {
                    lastTime = since == null ? queryTime : since;
                }
                // Strip the timestamps
                logContent = logContent.replaceAll("(^|\\n)\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{9}Z ","\n");
            }
        }
        return type.cast( (type == String.class) ? logContent : logJsonMap(logContent, lastTime) );
    }

    private Map<String, Object> logJsonMap(String content, Long lastTime) {
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("content", content);
        jsonMap.put("timestamp", lastTime);
        return jsonMap;
    }

    private void writeToOuputStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }
    }

    private static String getAttachmentDisposition(final String name, final String extension) {
        return String.format(ATTACHMENT_DISPOSITION, name, extension);
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    @ExceptionHandler(value = {NotFoundException.class})
    public String handleNotFound(final Exception e) {
        return e.getMessage();
    }

    @ResponseStatus(value = HttpStatus.FAILED_DEPENDENCY)
    @ExceptionHandler(value = {NoDockerServerException.class})
    public String handleFailedDependency() {
        return "Set up Docker server before using this REST endpoint.";
    }

    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = {DockerServerException.class, ContainerException.class})
    public String handleDockerServerException(final Exception e) {
        return e.getMessage();
    }
}
