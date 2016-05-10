package org.nrg.containers.api.impl;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.nrg.containers.config.DockerControlApiTestConfig;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.model.DockerHub;
import org.nrg.containers.model.DockerImageDto;
import org.nrg.containers.model.DockerServerPrefsBean;
import org.nrg.prefs.services.NrgPreferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.isIn;
import static org.junit.Assert.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = DockerControlApiTestConfig.class)
public class DockerControlApiTest {

    static String CONTAINER_HOST;
    static String CERT_PATH ;

    private static DockerClient client;

    private static final String BUSYBOX_LATEST = "busybox:latest";
    private static final String UBUNTU_LATEST = "ubuntu:latest";
    private static final String KELSEYM_PYDICOM = "kelseym/pydicom:latest";
    private static final String BUSYBOX_ID = "307ac631f1b53c62d39e9c9e81bdbdbb50a2295c35fb0ec672d168a0b129a623";
    private static final String BUSYBOX_NAME = "busybox:1.24.2-uclibc";

    @Autowired
    private DockerControlApi controlApi;

    @Autowired
    private DockerServerPrefsBean dockerServerPrefsBean;

    @Autowired
    private NrgPreferenceService mockPrefsService;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() throws Exception {

        final String hostEnv = System.getenv("DOCKER_HOST");
        CONTAINER_HOST = hostEnv != null && !hostEnv.equals("") ?
            hostEnv.replace("tcp", "https") :
            "https://192.168.99.100:2376";

        final String tlsVerify = System.getenv("DOCKER_TLS_VERIFY");
        final String certPathEnv = System.getenv("DOCKER_CERT_PATH");
        if (tlsVerify != null && tlsVerify.equals("1")) {
            CERT_PATH = certPathEnv != null && !certPathEnv.equals("") ?
                certPathEnv : "/Users/Kelsey/.docker/machine/machines/testDocker";
        } else {
            CERT_PATH = "";
        }

        // Set up mock prefs service for all the calls that will initialize
        // the ContainerServerPrefsBean
        when(mockPrefsService.getPreferenceValue("container-server", "host"))
            .thenReturn(CONTAINER_HOST);
        when(mockPrefsService.getPreferenceValue("container-server", "certPath"))
            .thenReturn(CERT_PATH);
        doNothing().when(mockPrefsService)
            .setPreferenceValue("container-server", "host", "");
        doNothing().when(mockPrefsService)
            .setPreferenceValue("container-server", "certPath", "");
        when(mockPrefsService.hasPreference("container-server", "host"))
            .thenReturn(true);
        when(mockPrefsService.hasPreference("container-server", "certPath"))
            .thenReturn(true);

        dockerServerPrefsBean.initialize();

        client = controlApi.getClient();
    }

    @After
    public void tearDown() throws Exception {
        return;
    }

    @Test
    public void testGetServer() throws Exception {
//        final ObjectMapper mapper = new ObjectMapper();
//        final String containerServerJson =
//            "{\"host\":\""+ CONTAINER_HOST + "\", \"certPath\":\"" +
//                CERT_PATH + "\"}";
//        final ContainerServerPrefsBean expectedServer = mapper.readValue(containerServerJson, ContainerServerPrefsBean.class);

        assertEquals(dockerServerPrefsBean.toBean(), controlApi.getServer());
    }

    @Test
    public void testGetImageByName() throws Exception {
        client.pull(BUSYBOX_LATEST);
        final DockerImageDto image = controlApi.getImageByName(BUSYBOX_LATEST);
        assertThat(image.getName(), containsString(BUSYBOX_LATEST));
    }

    @Test
    public void testGetAllImages() throws Exception {
        client.pull(BUSYBOX_LATEST);
        client.pull(UBUNTU_LATEST);
        final List<DockerImageDto> images = controlApi.getAllImages();

        final List<String> imageNames = imagesToTags(images);
        assertThat(BUSYBOX_LATEST, isIn(imageNames));
        assertThat(UBUNTU_LATEST, isIn(imageNames));
    }

    private List<String> imagesToTags(final List<DockerImageDto> images) {
        final List<String> tags = Lists.newArrayList();
        for (final DockerImageDto image : images) {
            if (image.getRepoTags() != null) {
                tags.addAll(image.getRepoTags());
            }
        }
        return tags;
    }

    private List<String> imagesToNames(final List<DockerImageDto> images) {
        final Function<DockerImageDto, String> imageToName = new Function<DockerImageDto, String>() {
            @Override
            public String apply(final DockerImageDto image) {
                return image.getName();
            }
        };
        return Lists.transform(images, imageToName);
    }

    @Test
    public void testLaunchImage() throws Exception {
        final List<String> cmd = Lists.newArrayList("ls", "/data/pyscript.py");
        final List<String> vol =
            Lists.newArrayList("/Users/Kelsey/Projects/XNAT/1.7/pydicomDocker/data:/data");

        client.pull(KELSEYM_PYDICOM);
        String containerId = controlApi.launchImage(KELSEYM_PYDICOM, cmd, vol);
    }

    @Test
    public void testLaunchPythonScript() throws Exception {
       // python pyscript.py -h <hostname> -u <user> -p <password> -s <session_id>
        final List<String> cmd = Lists.newArrayList(
            "python", "/data/pyscript.py",
            "-h", "https://central.xnat.org",
            "-u", "admin",
            "-p", "admin",
            "-s", "CENTRAL_E07096"
        );
        final List<String> vol =
            Lists.newArrayList("/Users/Kelsey/Projects/XNAT/1.7/pydicomDocker/data:/data");

        client.pull(KELSEYM_PYDICOM);
        String containerId = controlApi.launchImage(KELSEYM_PYDICOM, cmd, vol);
    }

    @Test
    public void testPingServer() throws Exception {
        assertEquals("OK", controlApi.pingServer());
    }

    @Test
    public void testPingHub() throws Exception {
        final DockerHub dockerHub = DockerHub.builder()
                .url("https://index.docker.io/v1/")
                .name("Docker Hub")
                .build();

        assertEquals("OK", controlApi.pingHub(dockerHub));
    }

    @Test
    public void testPullImage() throws Exception {
        controlApi.pullImage(BUSYBOX_LATEST);

    }

    @Test
    public void testDeleteImage() throws DockerException, InterruptedException, NoServerPrefException, DockerServerException {
        client.pull(BUSYBOX_NAME);
        int beforeImageCount = client.listImages().size();
        controlApi.deleteImageById(BUSYBOX_ID);
        List<com.spotify.docker.client.messages.Image> images = client.listImages();
        int afterImageCount = images.size();
        assertEquals(beforeImageCount, afterImageCount+1);
        for(com.spotify.docker.client.messages.Image image:images){
            assertNotEquals(BUSYBOX_ID, image.id());
        }
    }

}


