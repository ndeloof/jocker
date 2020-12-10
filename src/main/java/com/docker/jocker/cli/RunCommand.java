package com.docker.jocker.cli;

import com.docker.jocker.DockerClient;
import com.docker.jocker.model.ContainerCreateResponse;
import com.docker.jocker.model.ContainerInspectResponse;
import com.docker.jocker.model.ContainerSpec;
import com.docker.jocker.model.HostConfig;

import java.io.IOException;
import java.util.List;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class RunCommand extends Command {

    private final DockerClient dockerClient;

    private String containerName;
    private boolean autoRemove;

    public RunCommand(DockerClient dockerClient) {
        super("run");
        this.dockerClient = dockerClient;
        options(
                new Option("name", null, this::setContainerName),
                new BoolOption("rm", null, this::setAutoRemove)
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
                .cmd(args), containerName);
        dockerClient.containerStart(created.getId());
        System.out.println(created.getId());
    }

    protected void setContainerName(String n) {
        containerName = n;
    }

    public void setAutoRemove(boolean autoRemove) {
        this.autoRemove = autoRemove;
    }
}
