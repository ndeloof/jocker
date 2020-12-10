package com.docker.jocker.cli;

import com.docker.jocker.DockerClient;
import com.docker.jocker.model.SystemVersion;

import java.io.IOException;
import java.util.List;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class VersionCommand extends Command {

    private final DockerClient dockerClient;

    public VersionCommand(DockerClient dockerClient) {
        super("version");
        this.dockerClient = dockerClient;
    }

    @Override
    void run(List<String> args) throws IOException {
        final SystemVersion version = dockerClient.version();
        System.out.println(version.toString());
    }
}
