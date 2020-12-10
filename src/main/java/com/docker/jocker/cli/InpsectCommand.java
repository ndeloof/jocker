package com.docker.jocker.cli;

import com.docker.jocker.DockerClient;
import com.docker.jocker.model.ContainerInspectResponse;
import com.docker.jocker.model.SystemVersion;

import java.io.IOException;
import java.util.List;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class InpsectCommand extends Command {

    private final DockerClient dockerClient;

    public InpsectCommand(DockerClient dockerClient) {
        super("inspect");
        this.dockerClient = dockerClient;
    }

    @Override
    void run(List<String> args) throws IOException {
        if (args.size() != 1) {
            throw new IllegalArgumentException(name + " require 1 argument");
        }
        final String id = args.get(0);
        final ContainerInspectResponse inspect = dockerClient.containerInspect(id);
        System.out.println(inspect.toString());
    }
}
