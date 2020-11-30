package com.docker.jocker;

import com.docker.jocker.model.*;
import com.docker.jocker.io.TarInputStreamBuilder;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.docker.jocker.model.ContainerState.StatusEnum.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.apache.commons.io.IOUtils.*;
import static org.junit.Assert.*;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class DockerClientTest {

    private static final Map<String, String> label = Collections.singletonMap("it.dockins.jocker", "test");

    DockerClient docker;
            
    @Before
    public void init() throws IOException {
        String dockerHost = System.getenv("DOCKER_HOST");
        if (dockerHost == null) dockerHost = "unix:///var/run/docker.sock";
        docker = new DockerClient(dockerHost);
    }
    
    @After
    public void tearDown() throws IOException {
        try (DockerClient docker = new DockerClient("unix:///var/run/docker.sock")) {
            ContainerSummary containers = docker.containerList(false, 10, false, new ContainersFilters().label("it.dockins.jocker=test"));
            for (ContainerSummaryInner container : containers) {
                docker.containerDelete(container.getId(), true, false, true);
            }
        }
    }

    @Test
    public void version() throws IOException {
        final SystemVersion version = docker.version();
        Assert.assertNotNull(version);
        Assert.assertNotNull(version.getApiVersion());
        System.out.println(version);
    }

    @Test
    public void info() throws IOException {
        final SystemInfo info = docker.info();
        Assert.assertNotNull(info);
        Assert.assertNotNull(info.getOperatingSystem());
        System.out.println(info);
    }

    @Test
    public void runHelloWorld() throws IOException {
        docker.imagePull("hello-world", null, null, System.out::println);
        String id = docker.containerCreate(new ContainerSpec().image("hello-world").labels(label), null).getId();
        docker.containerStart(id);
        System.out.println(id);
    }

    @Test
    public void containerStopAndRestart() throws IOException {
        final String container = createLongRunContainer();
        assertEquals(RUNNING, docker.containerInspect(container).getState().getStatus());
        docker.containerStop(container, 10);
        assertEquals(EXITED, docker.containerInspect(container).getState().getStatus());
        docker.containerRestart(container, 10);
        assertEquals(RUNNING, docker.containerInspect(container).getState().getStatus());

        System.out.println(container);
    }

    @Test
    public void containerPauseAndUnpause() throws IOException {
        final String container = createLongRunContainer();
        assertEquals(RUNNING, docker.containerInspect(container).getState().getStatus());
        docker.containerPause(container);
        assertEquals(PAUSED, docker.containerInspect(container).getState().getStatus());
        docker.containerUnpause(container);
        assertEquals(RUNNING, docker.containerInspect(container).getState().getStatus());
        System.out.println(container);
    }

    @Test
    public void containerKill() throws IOException {
        final String container = createLongRunContainer();
        docker.containerKill(container, null);
        assertEquals(EXITED, docker.containerInspect(container).getState().getStatus());
    }

    @Test
    public void containerLogs() throws IOException {
        docker.imagePull("hello-world", null, null, System.out::println);
        String container = docker.containerCreate(new ContainerSpec().image("hello-world").labels(label).tty(true), null).getId();
        docker.containerStart(container);

        InputStream in = docker.containerLogs(container, true, true, false, true, 0, null);
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        IOUtils.copy(in, bos);
        final String output = bos.toString();
        System.out.println("output: " + output);
        assertTrue(output.contains("Hello from Docker!\r\nThis message shows that your installation appears to be working correctly."));
    }

    @Test
    public void containerAttach() throws IOException {
        docker.imagePull("alpine", null, null, System.out::println);
        String container = docker.containerCreate(new ContainerSpec().image("alpine").labels(label)
                .attachStdin(true).attachStdout(true).cmd(asList("ping", "localhost")), null).getId();

        docker.containerStart(container);
        final Streams streams = docker.containerAttach(container, true, true, false, true, true, null);

        final BufferedReader reader = new BufferedReader(new InputStreamReader(streams.stdout()));
        String output = reader.readLine();
        System.out.println("output: " + output);
        assertTrue(output.startsWith("PING localhost (127.0.0.1):"));
    }

    @Test
    public void containerRename() throws IOException {
        docker.imagePull("hello-world", null, null, System.out::println);
        String container = docker.containerCreate(new ContainerSpec().image("hello-world").labels(label).tty(true), null).getId();
        docker.containerRename(container, "foo_bar");
        assertEquals("/foo_bar", docker.containerInspect(container).getName());
    }


    @Test
    public void containerPrune() throws IOException {
        docker.imagePull("hello-world", null, null, System.out::println);
        String container = docker.containerCreate(new ContainerSpec().image("hello-world").labels(label).tty(true), null).getId();
        docker.containerStart(container);
        docker.containerWait(container, WaitCondition.NEXT_EXIT);

        final ContainerPruneResponse response = docker.containerPrune(new ContainersFilters().label("it.dockins.jocker=test"));
        assertTrue(response.getContainersDeleted().contains(container));
    }

    @Test
    public void containerArchiveInfo() throws IOException {
        docker.imagePull("hello-world", null, null, System.out::println);
        String container = docker.containerCreate(new ContainerSpec().image("hello-world").labels(label), null).getId();
        final FileSystemHeaders x = docker.containerArchiveInfo(container, "/hello");
        System.out.println(x);
        assertEquals("hello", x.getName());
    }

    @Test
    public void containerArchive() throws IOException, InterruptedException {
        final String container = createLongRunContainer();
        docker.putContainerFile(container, "/tmp/", false, new File("./pom.xml"));

        final TarArchiveInputStream tar = docker.containerArchive(container, "/tmp/pom.xml");
        final TarArchiveEntry entry = tar.getNextTarEntry();
        Assert.assertNotNull(entry);
        final int size = (int) entry.getSize();
        byte[] b = new byte[size];
        read(tar, b, 0, size);
        assertNull(tar.getNextTarEntry());
        final String out = new String(b, UTF_8);
        assertTrue(out.contains("a Java client library for Docker API"));
    }

    @Test
    public void volumes() throws IOException, InterruptedException {
        Volume volume = docker.volumeCreate(new VolumeConfig().labels(label));
        String name = volume.getName();

        VolumeListResponse list = docker.volumeList(new VolumeFilters().label("it.dockins.jocker=test"));
        List<String> names = list.getVolumes().stream().map(Volume::getName).collect(Collectors.toList());
        assertTrue(names.contains(name));

        Volume inspect = docker.volumeInspect(name);
        assertEquals(volume, inspect);

        docker.volumeDelete(name);
    }

    @Test
    public void networks() throws IOException, InterruptedException {
        Network network = docker.networkCreate(new NetworkConfig().name("test").labels(label));
        String id = network.getId();

        List<Network> list = docker.networkList(new NetworkFilters().label("it.dockins.jocker=test"));
        List<String> ids = list.stream().map(Network::getId).collect(Collectors.toList());
        assertTrue(ids.contains(id));

        String container = createLongRunContainer();
        docker.networkConnect(id, container);

        Network inspect = docker.networkInspect(id);
        assertNotNull(inspect);
        NetworkContainer net = inspect.getContainers().get(container);
        assertNotNull(net);

        docker.networkDisconnect(id, container, true);

        inspect = docker.networkInspect(id);
        assertNotNull(inspect);
        assertTrue(!inspect.getContainers().containsKey(container));

        docker.networkDelete(id);
    }

    @Test
    public void imageBuild() throws IOException {
        final InputStream context = new TarInputStreamBuilder()
            .add("Dockerfile", 0700, getClass().getResourceAsStream("Dockerfile"))
            .build();

        docker.imageBuild(
                new BuildImageRequest()
                        .dockerfile("Dockerfile")
                        .tag("jocker:test"),
                null, context, System.out::println);
    }

    private String createLongRunContainer() throws IOException {
        docker.imagePull("alpine", null, null, System.out::println);
        final String container = docker.containerCreate(new ContainerSpec()
                                       .image("alpine").labels(label).cmd(asList("sleep", "10")), null).getId();
        docker.containerStart(container);
        System.out.println("container ID: " + container);
        return container;
    }


}