package com.docker.jocker;

import com.docker.jocker.model.ContainerCreateResponse;
import com.docker.jocker.model.ContainerSpec;
import com.docker.jocker.model.Streams;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.util.Arrays;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class DockerCliDemo {

    public static void main(String[] args) throws IOException, InterruptedException {
        DockerClient docker = new DockerClient("tcp://localhost:2376");
        ContainerCreateResponse created = docker.containerCreate(new ContainerSpec()
                .image("alpine")
                .cmd(Arrays.asList("/bin/sh"))
                .tty(true)
                .openStdin(true)
                .stdinOnce(true)
                .attachStdin(true)
                .attachStdout(true)
                .attachStderr(true), "");
        String id = created.getId();

        Streams streams = docker.containerAttach(id, true, true, true, true, false, "X", true);

        new Thread(() -> {
            try {
                final InputStream o = streams.stdout();
                int c;
                while ((c = o.read()) > 0) {
                    System.out.write(c);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        docker.containerStart(id);


        int c;
        while ((c = System.in.read()) > 0) {
            streams.stdin().write(c);
        }
    }

}
