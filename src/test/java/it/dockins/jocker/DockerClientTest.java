package it.dockins.jocker;


import it.dockins.jocker.model.ContainerInspectResponseState;
import it.dockins.jocker.model.ContainerSpec;
import it.dockins.jocker.model.ContainerSummary;
import it.dockins.jocker.model.ContainerSummaryInner;
import it.dockins.jocker.model.ContainersFilters;
import it.dockins.jocker.model.ExecConfig;
import it.dockins.jocker.model.Streams;
import it.dockins.jocker.model.SystemInfo;
import it.dockins.jocker.model.SystemVersionResponse;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static it.dockins.jocker.model.ContainerInspectResponseState.StatusEnum.EXITED;
import static it.dockins.jocker.model.ContainerInspectResponseState.StatusEnum.PAUSED;
import static it.dockins.jocker.model.ContainerInspectResponseState.StatusEnum.RUNNING;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class DockerClientTest {

    private static final Map<String, String> label = Collections.singletonMap("it.dockins.jocker", "test");

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
        try (DockerClient docker = new DockerClient("unix:///var/run/docker.sock")){
            final SystemVersionResponse version = docker.version();
            Assert.assertNotNull(version);
            Assert.assertNotNull(version.getApiVersion());
            System.out.println(version);
        }
    }

    @Test
    public void info() throws IOException {
        try (DockerClient docker = new DockerClient("unix:///var/run/docker.sock")) {
            final SystemInfo info = docker.info();
            Assert.assertNotNull(info);
            Assert.assertNotNull(info.getOperatingSystem());
            System.out.println(info);
        }
    }

    @Test
    public void runHelloWorld() throws IOException {
        try (DockerClient docker = new DockerClient("unix:///var/run/docker.sock")) {
            docker.imagePull("hello-world", null, null, System.out::println);
            String id = docker.containerCreate(new ContainerSpec().image("hello-world").labels(label), null).getId();
            docker.containerStart(id);
            System.out.println(id);
        }
    }

    @Test
    public void stopAndRestart() throws IOException {
        try (DockerClient docker = new DockerClient("unix:///var/run/docker.sock")) {
            final String container = createLongRunContainer(docker);
            Assert.assertEquals(RUNNING, docker.containerInspect(container).getState().getStatus());
            docker.containerStop(container, 10);
            Assert.assertEquals(EXITED, docker.containerInspect(container).getState().getStatus());
            docker.containerRestart(container, 10);
            Assert.assertEquals(RUNNING, docker.containerInspect(container).getState().getStatus());

            System.out.println(container);
        }
    }

    @Test
    public void pauseAndUnpause() throws IOException {
        try (DockerClient docker = new DockerClient("unix:///var/run/docker.sock")) {
            final String container = createLongRunContainer(docker);
            Assert.assertEquals(RUNNING, docker.containerInspect(container).getState().getStatus());
            docker.containerPause(container);
            Assert.assertEquals(PAUSED, docker.containerInspect(container).getState().getStatus());
            docker.containerUnpause(container);
            Assert.assertEquals(RUNNING, docker.containerInspect(container).getState().getStatus());
            System.out.println(container);
        }
    }

    @Test
    public void kill() throws IOException {
        try (DockerClient docker = new DockerClient("unix:///var/run/docker.sock")) {
            final String container = createLongRunContainer(docker);
            docker.containerKill(container, null);
            Assert.assertEquals(EXITED, docker.containerInspect(container).getState().getStatus());
        }
    }

    @Test
    public void logs() throws IOException {
        try (DockerClient docker = new DockerClient("unix:///var/run/docker.sock")) {
            docker.imagePull("hello-world", null, null, System.out::println);
            String container = docker.containerCreate(new ContainerSpec().image("hello-world").labels(label).tty(true), null).getId();
            docker.containerStart(container);

            InputStream in = docker.containerLogs(container, true, true, false, true, 0, null);
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            IOUtils.copy(in, bos);
            final String output = bos.toString();
            System.out.println("output: " + output);
            Assert.assertTrue(output.contains("Hello from Docker!\r\nThis message shows that your installation appears to be working correctly."));
        }
    }


    @Test
    public void copyFileInContainer() throws IOException, InterruptedException {
        try (DockerClient docker = new DockerClient("unix:///var/run/docker.sock")) {
            final String container = createLongRunContainer(docker);
            docker.putContainerFile(container, "/tmp/", false, new File("./pom.xml"));
            final String exec = docker.containerExec(container, new ExecConfig().cmd(Arrays.asList("ls", "/tmp/pom.xml")).attachStdout(true));
            System.out.println("exec ID: " + exec);
            final Streams streams = docker.execStart(exec, false, false);
            final ByteArrayOutputStream output = new ByteArrayOutputStream();
            IOUtils.copy(streams.stdout(), output);
            System.out.println("output: " + output);
            Assert.assertFalse(new String(output.toByteArray()).contains("No such file or directory"));
        }
    }

    private String createLongRunContainer(DockerClient docker) throws IOException {
        docker.imagePull("alpine", null, null, System.out::println);
        final String container = docker.containerCreate(new ContainerSpec()
                                       .image("alpine").labels(label).cmd("sleep", "10"), null).getId();
        docker.containerStart(container);
        System.out.println("container ID: " + container);
        return container;
    }


}