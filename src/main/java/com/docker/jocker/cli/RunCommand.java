package com.docker.jocker.cli;

import com.docker.jocker.DockerClient;
import com.docker.jocker.model.ContainerCreateResponse;
import com.docker.jocker.model.ContainerInspectResponse;
import com.docker.jocker.model.ContainerSpec;
import com.docker.jocker.model.ContainerWaitResponse;
import com.docker.jocker.model.HostConfig;
import com.docker.jocker.model.Streams;
import com.docker.jocker.model.WaitCondition;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class RunCommand extends Command {

    private final DockerClient dockerClient;

    private String containerName;
    private boolean autoRemove;
    private boolean tty;
    private boolean detach;
    private boolean interactive;

    public RunCommand(DockerClient dockerClient) {
        super("run");
        this.dockerClient = dockerClient;
        options(
                new Option("name", null, this::setContainerName),
                new BoolOption("rm", null, this::setAutoRemove),
                new BoolOption("tty", "t", this::setTty),
                new BoolOption("interactive", "i", this::setInteractive),
                new BoolOption("detach", "d", this::setDetach)
        );
    }

    @Override
    void run(List<String> args) throws IOException {
        if (args.size() < 1) {
            throw new IllegalArgumentException(name + " require 1+ argument");
        }
        final String image = args.get(0);
        args.remove(0);
        final ContainerCreateResponse created = dockerClient.containerCreate(new ContainerSpec()
                .hostConfig(new HostConfig()
                    .autoRemove(autoRemove))
                .image(image)
                .hostname(containerName)
                .tty(tty)
                .openStdin(interactive)
                .stdinOnce(interactive)
                .attachStdin(interactive)
                .attachStdout(interactive)
                .attachStderr(interactive)
                .cmd(args), containerName);
        final String containerID = created.getId();

        if (detach){
            dockerClient.containerStart(containerID);
            System.out.println(containerID);
            return;
        }


        Streams streams = dockerClient.containerAttach(containerID, true, true, true, true, false, null, tty);
        if (!tty) {
            streams.redirectStderr(System.err);
        }

        dockerClient.containerStart(containerID);

        final InputStream stdout = streams.stdout();
        final OutputStream stdin = streams.stdin();
        if (interactive) {
            new Thread(() -> {
                try {
                    int c;
                    // NOTE: java does not allow to configure terminal in character mode
                    while ((c = System.in.read()) > 0) {
                        stdin.write(c);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
        new Thread(() -> {
            try {
                int c;
                while ((c = stdout.read()) > 0) {
                    System.out.print((char)c);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        final ContainerWaitResponse st = dockerClient.containerWait(containerID, WaitCondition.NEXT_EXIT);
        System.out.println(st);
        System.exit(st.getStatusCode());
    }

    protected void setContainerName(String n) {
        containerName = n;
    }

    public void setAutoRemove(boolean autoRemove) {
        this.autoRemove = autoRemove;
    }

    public void setTty(boolean tty) {
        this.tty = tty;
    }

    public void setDetach(boolean detach) {
        this.detach = detach;
    }

    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
    }
}
