package org.nrg.containers.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.lang3.StringUtils;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.exceptions.NotUniqueException;
import org.nrg.containers.exceptions.UnauthorizedException;
import org.nrg.containers.model.Command;
import org.nrg.containers.model.auto.DockerImage;
import org.nrg.containers.model.DockerServer;
import org.nrg.containers.model.auto.DockerHub;
import org.nrg.containers.model.auto.DockerImageAndCommandSummary;
import org.nrg.containers.services.DockerHubService.DockerHubDeleteDefaultException;
import org.nrg.containers.services.DockerService;
import org.nrg.framework.annotations.XapiRestController;
import org.nrg.framework.exceptions.NrgServiceRuntimeException;
import org.nrg.prefs.exceptions.InvalidPreferenceName;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.rest.AbstractXapiRestController;
import org.nrg.xdat.security.services.RoleHolder;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xft.security.UserI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

import static org.nrg.containers.helpers.CommandLabelHelper.LABEL_KEY;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@XapiRestController
@RequestMapping(value = "/docker")
public class DockerRestApi extends AbstractXapiRestController {
    private static final Logger log = LoggerFactory.getLogger(DockerRestApi.class);

    private static final String ID_REGEX = "\\d+";
    private static final String NAME_REGEX = "\\d*[^\\d]+\\d*";

    private static final String JSON = MediaType.APPLICATION_JSON_UTF8_VALUE;
    private static final String TEXT = MediaType.TEXT_PLAIN_VALUE;

    private DockerService dockerService;
    private ObjectMapper mapper;

    @Autowired
    public DockerRestApi(final DockerService dockerService,
                         final ObjectMapper objectMapper,
                         final UserManagementServiceI userManagementService,
                         final RoleHolder roleHolder) {
        super(userManagementService, roleHolder);
        this.dockerService = dockerService;
        this.mapper = objectMapper;
    }

    @ApiOperation(value = "Docker server", notes = "Returns Docker server configuration values",
            response = DockerServer.class)
    @ApiResponses({
            @ApiResponse(code = 200, message = "The Docker server configuration"),
            @ApiResponse(code = 400, message = "The server has not been configured"),
            @ApiResponse(code = 500, message = "Unexpected error")})
    @RequestMapping(value = "/server", method = GET, produces = JSON)
    @ResponseBody
    public DockerServer getServer() throws NotFoundException {
        return dockerService.getServer();
    }

    @ApiOperation(value = "Set Docker server configuration",
            notes = "Save new Docker server configuration values")
    @ApiResponses({
            @ApiResponse(code = 202, message = "The Docker server configuration was saved"),
            @ApiResponse(code = 400, message = "Must set the \"host\" property in request body"),
            @ApiResponse(code = 500, message = "Unexpected error")})
    @RequestMapping(value = "/server", method = POST)
    public ResponseEntity<String> setServer(final @RequestBody DockerServer dockerServer)
            throws InvalidPreferenceName, JsonProcessingException, UnauthorizedException {
        checkCreateOrThrow();
        if (StringUtils.isBlank(dockerServer.getHost())) {
            return new ResponseEntity<>("Must set the \"host\" property in request body.",
                    HttpStatus.BAD_REQUEST);
        }

        final DockerServer server = dockerService.setServer(dockerServer);
        return new ResponseEntity<>(mapper.writeValueAsString(server), HttpStatus.ACCEPTED);
    }

    @RequestMapping(value = "/server/ping", method = GET)
    @ResponseBody
    public String pingServer()
            throws NoServerPrefException, DockerServerException, UnauthorizedException {
        checkGetOrThrow();
        return dockerService.pingServer();
    }

    @RequestMapping(value = "/hubs", method = GET)
    @ResponseBody
    public List<DockerHub> getHubs() throws UnauthorizedException {
        checkGetOrThrow();
        return dockerService.getHubs();
    }

    @RequestMapping(value = "/hubs/{id:" + ID_REGEX + "}", method = GET)
    @ResponseBody
    public DockerHub getHub(final @PathVariable long id) throws NotFoundException, UnauthorizedException {
        checkGetOrThrow();
        return dockerService.getHub(id);
    }

    @RequestMapping(value = "/hubs/{name:" + NAME_REGEX + "}", method = GET)
    @ResponseBody
    public DockerHub getHub(final @PathVariable String name) throws NotFoundException, NotUniqueException, UnauthorizedException {
        checkGetOrThrow();
        return dockerService.getHub(name);
    }

    @RequestMapping(value = "/hubs", method = POST)
    @ResponseBody
    public ResponseEntity<DockerHub> createHub(final @RequestBody DockerHub hub,
                                               final @RequestParam(value = "default", defaultValue = "false") boolean setDefault,
                                               final @RequestParam(value = "reason", defaultValue = "User request") String reason)
            throws NrgServiceRuntimeException, UnauthorizedException {
        final UserI userI = XDAT.getUserDetails();
        checkCreateOrThrow(userI);
        if (!setDefault) {
            return new ResponseEntity<>(dockerService.createHub(hub), HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>(dockerService.createHubAndSetDefault(hub, userI.getUsername(), reason), HttpStatus.CREATED);
        }
    }

    @RequestMapping(value = "/hubs/{id:" + ID_REGEX + "}", method = POST)
    @ResponseBody
    public ResponseEntity updateHub(final @PathVariable long id,
                               final @RequestBody DockerHub hub,
                               final @RequestParam(value = "default", defaultValue = "false") boolean setDefault,
                               final @RequestParam(value = "reason", defaultValue = "User request") String reason)
            throws NrgServiceRuntimeException, UnauthorizedException {
        final UserI userI = XDAT.getUserDetails();
        checkCreateOrThrow(userI);
        final DockerHub toUpdate = id == hub.id() ? hub : DockerHub.create(id, hub.name(), hub.url(), hub.username(), hub.password(), hub.email());

        if (!setDefault) {
            dockerService.updateHub(toUpdate);
        } else {
            dockerService.updateHubAndSetDefault(hub, userI.getUsername(), reason);
        }
        return new ResponseEntity(HttpStatus.OK);
    }

    @RequestMapping(value = "/hubs/{id:" + ID_REGEX + "}", method = DELETE)
    @ResponseBody
    public ResponseEntity<String> deleteHub(final @PathVariable long id)
            throws DockerHubDeleteDefaultException, UnauthorizedException {
        checkDeleteOrThrow();
        dockerService.deleteHub(id);
        return new ResponseEntity<>("", HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/hubs/{name:" + NAME_REGEX + "}", method = DELETE)
    @ResponseBody
    public ResponseEntity<String> deleteHub(final @PathVariable String name)
            throws DockerHubDeleteDefaultException, NotUniqueException, UnauthorizedException {
        checkDeleteOrThrow();
        dockerService.deleteHub(name);
        return new ResponseEntity<>("", HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/hubs/{id:" + ID_REGEX + "}/ping", method = GET)
    @ResponseBody
    public String pingHub(final @PathVariable Long id)
            throws NoServerPrefException, DockerServerException, NotFoundException, UnauthorizedException {
        checkGetOrThrow();
        return dockerService.pingHub(id);
    }

    @RequestMapping(value = "/hubs/{name:" + NAME_REGEX + "}/ping", method = GET)
    @ResponseBody
    public String pingHub(final @PathVariable String name)
            throws NoServerPrefException, DockerServerException, NotFoundException, NotUniqueException, UnauthorizedException {
        checkGetOrThrow();
        return dockerService.pingHub(name);
    }

    @RequestMapping(value = "/hubs/{id:" + ID_REGEX + "}/pull", params = {"image"}, method = POST)
    public void pullImageFromHub(final @PathVariable long hubId,
                                 final @RequestParam(value = "image") String image,
                                 final @RequestParam(value = "save-commands", defaultValue = "true")
                                             Boolean saveCommands)
            throws DockerServerException, NotFoundException, NoServerPrefException, UnauthorizedException {
        checkCreateOrThrow();
        dockerService.pullFromHub(hubId, image, saveCommands);
    }

    @RequestMapping(value = "/pull", params = {"image"}, method = POST)
    public void pullImageFromDefaultHub(final @RequestParam(value = "image") String image,
                                        final @RequestParam(value = "save-commands", defaultValue = "true")
                                                Boolean saveCommands)
            throws DockerServerException, NotFoundException, NoServerPrefException, UnauthorizedException {
        checkCreateOrThrow();
        dockerService.pullFromHub(image, saveCommands);
    }

    @ApiOperation(value = "Get list of images.", notes = "Returns a list of all Docker images.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "A list of images on the server"),
            @ApiResponse(code = 424, message = "Admin must set up Docker server."),
            @ApiResponse(code = 500, message = "Unexpected error")})
    @RequestMapping(value = "/images", method = GET, produces = JSON)
    @ResponseBody
    public List<DockerImage> getImages()
            throws NoServerPrefException, DockerServerException, UnauthorizedException {
        checkGetOrThrow();
        return dockerService.getImages();
    }

    @ApiOperation(value = "Get summary list of images and commands.")
    @RequestMapping(value = "/image-summaries", method = GET, produces = JSON)
    @ResponseBody
    public List<DockerImageAndCommandSummary> getImageSummaries()
            throws NoServerPrefException, DockerServerException, UnauthorizedException {
        checkGetOrThrow();
        return dockerService.getImageSummaries();
    }

    @ApiOperation(value = "Get Docker image",
            notes = "Retrieve information about a Docker image from the docker server")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Found the image"),
            @ApiResponse(code = 404, message = "No docker image with given id on the server"),
            @ApiResponse(code = 424, message = "Admin must set up Docker server."),
            @ApiResponse(code = 500, message = "Unexpected error")})
    @RequestMapping(value = "/images/{id}", method = GET, produces = JSON)
    @ResponseBody
    public DockerImage getImage(final @PathVariable("id") String id)
            throws NoServerPrefException, NotFoundException, UnauthorizedException {
        checkGetOrThrow();
        return dockerService.getImage(id);
    }

    @ApiOperation(value = "Delete Docker image",
            notes = "Remove information about a Docker image")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Image was removed"),
            @ApiResponse(code = 404, message = "No docker image with given id on docker server"),
            @ApiResponse(code = 424, message = "Admin must set up Docker server."),
            @ApiResponse(code = 500, message = "Unexpected error")})
    @RequestMapping(value = "/images/{id}", method = DELETE)
    @ResponseBody
    public void deleteImage(final @PathVariable("id") String id,
                            final @RequestParam(value = "force", defaultValue = "false") Boolean force)
            throws NotFoundException, NoServerPrefException, DockerServerException, UnauthorizedException {
        checkDeleteOrThrow();
        dockerService.removeImage(id, force);
    }

    @ApiOperation(value = "Save Commands from labels",
            notes = "Read labels from Docker image. If any labels contain key " +
                    LABEL_KEY + ", parse value as list of Commands.")
//    @ApiResponses({
//            @ApiResponse(code = 200, message = "Image was removed"),
//            @ApiResponse(code = 404, message = "No docker image with given id on docker server"),
//            @ApiResponse(code = 424, message = "Admin must set up Docker server."),
//            @ApiResponse(code = 500, message = "Unexpected error")})
    @RequestMapping(value = "/images/save", params = "image", method = POST)
    @ResponseBody
    public List<Command> saveFromLabels(final @RequestParam("image") String imageId)
            throws NotFoundException, NoServerPrefException, DockerServerException, UnauthorizedException {
        checkCreateOrThrow();
        return dockerService.saveFromImageLabels(imageId);
    }

    private void checkGetOrThrow() throws UnauthorizedException {
        checkGetOrThrow(XDAT.getUserDetails());
    }

    private void checkGetOrThrow(final UserI userI) throws UnauthorizedException {
        // Let everyone read everything?
    }

    private void checkDeleteOrThrow() throws UnauthorizedException {
        checkDeleteOrThrow(XDAT.getUserDetails());
    }

    private void checkDeleteOrThrow(final UserI userI) throws UnauthorizedException {
        if (!isAdmin(userI)) {
            throw new UnauthorizedException(String.format("User %s cannot delete.", userI == null ? "" : userI.getLogin()));
        }
    }

    private void checkCreateOrThrow() throws UnauthorizedException {
        checkCreateOrThrow(XDAT.getUserDetails());
    }

    private void checkCreateOrThrow(final UserI userI) throws UnauthorizedException {
        if (!isAdmin(userI)) {
            throw new UnauthorizedException(String.format("User %s cannot create.", userI == null ? "" : userI.getLogin()));
        }
    }

    private boolean isAdmin(final UserI userI) {
        return getRoleHolder().isSiteAdmin(userI);
    }

    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = {InvalidPreferenceName.class})
    public String handleInvalidPreferenceName(final Exception e) {
        return e.getMessage();
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    @ExceptionHandler(value = {NotFoundException.class})
    public String handleNotFound(final Exception e) {
        return e.getMessage();
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = {NotUniqueException.class})
    public String handleNotUnique(final Exception e) {
        return e.getMessage();
    }

    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = {DockerServerException.class})
    public String handleDockerServerError(final Exception e) {
        return "The Docker server returned an error:\n" + e.getMessage();
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = {NrgServiceRuntimeException.class})
    public String handleBadRequest() {
        return "Body was not a valid Docker Hub.";
    }

    @ResponseStatus(value = HttpStatus.FAILED_DEPENDENCY)
    @ExceptionHandler(value = {NoServerPrefException.class})
    public String handleFailedDependency() {
        return "Set up Docker server before using this REST endpoint.";
    }

    @ResponseStatus(value = HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(value = {UnauthorizedException.class})
    public String handleUnauthorized(final Exception e) {
        return "Unauthorized.\n" + e.getMessage();
    }

    @ResponseStatus(value = HttpStatus.CONFLICT)
    @ExceptionHandler(value = {DockerHubDeleteDefaultException.class})
    public String handleHubDelete(final DockerHubDeleteDefaultException e) {
        return e.getMessage();
    }


}

