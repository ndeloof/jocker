package com.docker.jocker.cli;

import com.docker.jocker.DockerClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A docker CLI implementation using Jocker library to access the daemon.
 * This code is for demonstration/test purpose only (so it's naive design), not intended to be a strict replacement
 * for <pre>docker</pre> command line, nor to become anything production ready.
 *
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class DockerCLI extends Command {


    public DockerCLI() throws IOException {
        super("docker");

        String dockerHost = System.getenv("DOCKER_HOST");
        if (dockerHost == null) dockerHost = "unix:///var/run/docker.sock";
        final DockerClient dockerClient = new DockerClient(dockerHost);
        
        subCommands(
                new VersionCommand(dockerClient),
                new RunCommand(dockerClient),
                new InpsectCommand(dockerClient));
    }

    public static void main(String[] args) throws IOException {
        new DockerCLI().parse(new ArrayList(Arrays.asList(args)));
    }

    @Override
    void run(List<String> args) {
        throw new IllegalArgumentException("unknown command "+ args);
    }
}
