package it.dockins.jocker;


import com.google.gson.stream.JsonReader;
import it.dockins.jocker.io.ChunkedInputStream;
import it.dockins.jocker.io.DockerMultiplexedInputStream;
import it.dockins.jocker.model.*;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.Socket;
import java.net.URI;
import java.nio.channels.Channels;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.zip.GZIPOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Implement <a href="https://docs.docker.com/engine/api/v1.32">Docker API</a> using a plain old java
 * {@link Socket}.
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class DockerClient extends HttpRestClient {

    private String version;
    
    public DockerClient(String dockerHost) throws IOException {
        this(dockerHost, null);
    }

    public DockerClient(String dockerHost, SSLContext ssl) throws IOException {
        super(URI.create(dockerHost), ssl);
        version = version().getApiVersion();
    }

    public SystemVersionResponse version() throws IOException {
        HttpRestClient.Response r = doGET("/version");
        return gson.fromJson(r.getBody(), SystemVersionResponse.class);
    }

    public SystemInfo info() throws IOException {
        HttpRestClient.Response r = doGET("/v"+version+"/info");
        return gson.fromJson(r.getBody(), SystemInfo.class);
    }

    /**
     * see https://docs.docker.com/engine/api/v1.32/#operation/ContainerList
     */
    public ContainerSummary containerList(boolean all, int limit, boolean size, ContainersFilters filters) throws IOException {
        return containerList(all, limit, size, gson.toJson(filters));
    }

    /**
     * see https://docs.docker.com/engine/api/v1.32/#operation/ContainerList
     */
    public ContainerSummary containerList(boolean all, int limit, boolean size, String filters) throws IOException {
        StringBuilder path = new StringBuilder("/v").append(version).append("/containers/json");
        path.append("?all=").append(all);
        path.append("&size=").append(size);
        if (limit > 0) path.append("&limit=").append(limit);
        if (filters != null) path.append("&filters=").append(filters);

        HttpRestClient.Response r = doGET(path.toString());
        return gson.fromJson(r.getBody(), ContainerSummary.class);
    }

    /**
     * copy a single file to a container
     * see https://docs.docker.com/engine/api/v1.32/#operation/PutContainerArchive
     */
    public void putContainerArchive(String container, String path, boolean noOverwriteDirNonDir, byte[] tar) throws IOException {
        StringBuilder uri = new StringBuilder("/v").append(version)
                .append("/containers/").append(container).append("/archive")
                .append("?path=").append(path)
                .append("&noOverwriteDirNonDir=").append(noOverwriteDirNonDir);

        doPUT(uri.toString(), tar);
    }

    /** Helper method to put a single file inside container */
    public void putContainerFile(String container, String path, boolean noOverwriteDirNonDir, File file) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(bos);
             TarArchiveOutputStream tar = new TarArchiveOutputStream(gz);
             InputStream in = new FileInputStream(file)) {
            final ArchiveEntry entry = tar.createArchiveEntry(file, file.getName());
            tar.putArchiveEntry(entry);
            IOUtils.copy(in, tar);
            tar.closeArchiveEntry();
        }
        putContainerArchive(container, path, noOverwriteDirNonDir, bos.toByteArray());
    }

    /**
     * see https://docs.docker.com/engine/api/v1.32/#operation/ContainerCreate
     */
    public ContainerCreateResponse containerCreate(ContainerConfig containerConfig, String name) throws IOException {
        StringBuilder path = new StringBuilder("/v").append(version).append("/containers/create");
        if (name != null) {
            path.append("?name=").append(name);
        }

        String spec = gson.toJson(containerConfig);
        HttpRestClient.Response r = doPOST(path.toString(), spec);
        return gson.fromJson(r.getBody(), ContainerCreateResponse.class);
    }

    /**
     * see https://docs.docker.com/engine/api/v1.32/#operation/ContainerStart
     */
    public void containerStart(String container) throws IOException {
        StringBuilder uri = new StringBuilder("/v").append(version)
                .append("/containers/").append(container).append("/start");
        doPOST(uri.toString());
    }

    /**
     * see https://docs.docker.com/engine/api/v1.32/#operation/ContainerStop
     */
    public void containerStop(String container, int timeout) throws IOException {
        StringBuilder uri = new StringBuilder("/v").append(version)
                .append("/containers/").append(container).append("/stop?t=").append(timeout);
        doPOST(uri.toString());
    }

    /**
     * see https://docs.docker.com/engine/api/v1.32/#operation/ContainerRestart
     */
    public void containerRestart(String container, int timeout) throws IOException {
        StringBuilder uri = new StringBuilder("/v").append(version)
                .append("/containers/").append(container).append("/restart?t=").append(timeout);
        doPOST(uri.toString());
    }

    /**
     * see https://docs.docker.com/engine/api/v1.32/#operation/ContainerResize
     */
    public void containerResize(String container, int height, int width) throws IOException {
        StringBuilder uri = new StringBuilder("/v").append(version)
                .append("/containers/").append(container).append("/resize?h=").append(height).append("&w=").append(width);
        doPOST(uri.toString());
    }

    /**
     * see https://docs.docker.com/engine/api/v1.32/#operation/ContainerChanges
     */
    public ContainerChangeResponseItem[] containerChanges(String container, boolean stream) throws IOException {
        StringBuilder uri = new StringBuilder("/v").append(version)
                .append("/containers/").append(container).append("/changes");
        final HttpRestClient.Response response = doGET(uri.toString());
        return gson.fromJson(response.getBody(), ContainerChangeResponseItem[].class);
    }

    /**
     * see https://docs.docker.com/engine/api/v1.32/#operation/ContainerStats
     * FIXME no model in swagger definition.
     */
    // public void containerStats(String container, boolean stream) throws IOException {

    /**
     * see https://docs.docker.com/engine/api/v1.32/#operation/ContainerKill
     */
    public void containerKill(String container, String signal) throws IOException {
        StringBuilder uri = new StringBuilder("/v").append(version)
                .append("/containers/").append(container).append("/kill");
        if (signal != null) {
            uri.append("?signal=").append(signal);
        }
        doPOST(uri.toString());
    }

    /**
     * see https://docs.docker.com/engine/api/v1.32/#operation/ContainerKill
     */
    public void containerRename(String container, String name) throws IOException {
        StringBuilder uri = new StringBuilder("/v").append(version)
                .append("/containers/").append(container).append("/rename?name=").append(name);
        doPOST(uri.toString());
    }

    /**
     * see https://docs.docker.com/engine/api/v1.32/#operation/ContainerPause
     */
    public void containerPause(String container) throws IOException {
        StringBuilder uri = new StringBuilder("/v").append(version)
                .append("/containers/").append(container).append("/pause");
        doPOST(uri.toString());
    }

    /**
     * see https://docs.docker.com/engine/api/v1.32/#operation/ContainerUnpause
     */
    public void containerUnpause(String container) throws IOException {
        StringBuilder uri = new StringBuilder("/v").append(version)
                .append("/containers/").append(container).append("/unpause");
        doPOST(uri.toString());
    }

    /**
     * see https://docs.docker.com/engine/api/v1.32/#operation/ContainerWait
     */
    public ContainerWaitResponse containerWait(String container, WaitCondition condition) throws IOException {
        StringBuilder uri = new StringBuilder("/v").append(version)
                .append("/containers/").append(container).append("/wait");
        if (condition != null) {
            uri.append("?condition=").append(condition.getValue());
        }
        HttpRestClient.Response r = doPOST(uri.toString());
        return gson.fromJson(r.getBody(), ContainerWaitResponse.class);
    }

    /**
     * see https://docs.docker.com/engine/api/v1.32/#operation/ContainerDelete
     */
    public void containerDelete(String container, boolean volumes, boolean links, boolean force) throws IOException {
        StringBuilder uri = new StringBuilder("/v").append(version)
                .append("/containers/").append(container)
                .append("?force=").append(force)
                .append("&v=").append(volumes)
                .append("&link=").append(links);

        doDELETE(uri.toString());
    }

    public InputStream containerLogs(String container, boolean follow, boolean stdout, boolean stderr, boolean timestamps, int since, String tail) throws IOException {
        StringBuilder uri = new StringBuilder("/v").append(version)
                .append("/containers/").append(container).append("/logs")
                .append("?follow=").append(follow)
                .append("&stdout=").append(stdout)
                .append("&stderr=").append(stderr)
                .append("&since=").append(since);
        if (tail != null) {
            uri.append("&tail=").append(tail);
        }

        HttpRestClient.Response r = doGET(uri.toString());
        return new ChunkedInputStream(Channels.newInputStream(socket));
    }

    public Streams containerAttach(String id, boolean stdin, boolean stdout, boolean stderr, boolean stream, boolean logs, String detachKeys) throws IOException {

        StringBuilder path = new StringBuilder("/v").append(version).append("/containers/").append(id).append("/attach")
                .append("?stdin=").append(stdin)
                .append("&stdout=").append(stdout)
                .append("&stderr=").append(stderr)
                .append("&stream=").append(stream)
                .append("&logs=").append(logs);
        if (detachKeys != null) {
            path.append("&detachKeys=").append(detachKeys);
        }

        final HttpRestClient.Response response = doPOST(path.toString());
        return new Streams() {

            @Override
            public InputStream stdout() throws IOException {
                // FIXME https://github.com/moby/moby/issues/35761
                return new DockerMultiplexedInputStream(Channels.newInputStream(socket));
            }

            @Override
            public OutputStream stdin() throws IOException {
                if (!stdin) throw new IOException("stdin is not attached");
                return Channels.newOutputStream(socket);
            }
        };
    }

    /**
     * see https://docs.docker.com/engine/api/v1.32/#operation/ContainerInspect
     */
    public ContainerInspectResponse containerInspect(String container) throws IOException {
        HttpRestClient.Response r = doGET("/v"+version+"/containers/"+container+"/json");
        final Reader body = r.getBody();
        return gson.fromJson(body, ContainerInspectResponse.class);
    }

    /**
     * see https://docs.docker.com/engine/api/v1.32/#operation/ContainerExec
     */
    public String containerExec(String container, ExecConfig execConfig) throws IOException {
        StringBuilder uri = new StringBuilder("/v").append(version).append("/containers/").append(container).append("/exec");
        String spec = gson.toJson(execConfig);
        HttpRestClient.Response r = doPOST(uri.toString(), spec);
        return gson.fromJson(r.getBody(), IdResponse.class).getId();
    }

    /**
     * see https://docs.docker.com/engine/api/v1.32/#operation/ContainerArchiveInfo
     */
    public FileSystemHeaders containerArchiveInfo(String container, String path) throws IOException {
        StringBuilder uri = new StringBuilder("/v").append(version).append("/containers/").append(container).append("/archive?path=").append(path);
        HttpRestClient.Response r = doHEAD(uri.toString());
        final String stats = r.getHeaders().get("X-Docker-Container-Path-Stat");
        final byte[] json = Base64.getDecoder().decode(stats.getBytes(UTF_8));

        try (InputStream stream = new ByteArrayInputStream(json);
             Reader reader = new InputStreamReader(stream)) {
            return gson.fromJson(reader, FileSystemHeaders.class);
        }
    }

    /**
     * see https://docs.docker.com/engine/api/v1.32/#operation/ContainerArchive
     */
    public TarArchiveInputStream containerArchive(String container, String path) throws IOException {
        StringBuilder uri = new StringBuilder("/v").append(version).append("/containers/").append(container).append("/archive?path=").append(path);
        HttpRestClient.Response r = doGET(uri.toString());
        return new TarArchiveInputStream(new ChunkedInputStream(Channels.newInputStream(socket)));
    }

    /**
     * see https://docs.docker.com/engine/api/v1.32/#operation/ContainerPrune
     */
    public ContainerPruneResponse containerPrune(ContainersFilters filters) throws IOException {
        StringBuilder path = new StringBuilder("/v").append(version).append("/containers/prune");
        if (filters != null) {
            path.append("?filters=").append(gson.toJson(filters));
        }
        HttpRestClient.Response r = doPOST(path.toString());
        return gson.fromJson(r.getBody(), ContainerPruneResponse.class);
    }

    /**
     * see https://docs.docker.com/engine/api/v1.32/#operation/ExecStart
     */
    public Streams execStart(String id, boolean detach, boolean tty) throws IOException {
        StringBuilder path = new StringBuilder("/v").append(version).append("/exec/").append(id).append("/start");
        final HttpRestClient.Response response = doPOST(path.toString(), "{\"Detach\": " + detach + ", \"Tty\": " + tty + "}");
        if (detach) return null;
        return new Streams() {

            @Override
            public InputStream stdout() throws IOException {
                // FIXME https://github.com/moby/moby/issues/35761
                return new DockerMultiplexedInputStream(Channels.newInputStream(socket));
            }

            @Override
            public OutputStream stdin() throws IOException {
                return Channels.newOutputStream(socket);
            }
        };
    }

    /**
     * see https://docs.docker.com/engine/api/v1.32/#operation/ExecInspect
     */
    public ExecInspectResponse execInspect(String id) throws IOException {
        StringBuilder path = new StringBuilder("/v").append(version).append("/exec/").append(id).append("/json");
        HttpRestClient.Response r = doGET(path.toString());
        final Reader body = r.getBody();
        return gson.fromJson(body, ExecInspectResponse.class);
    }


    /**
     * "pull" flavor of ImageCreate
     * see https://docs.docker.com/engine/api/v1.32/#operation/ImageCreate
     */
    public void imagePull(String image, String tag, AuthConfig authentication, Consumer<CreateImageInfo> consumer) throws IOException {
        if (tag == null) tag = "latest";
        StringBuilder path = new StringBuilder("/v").append(version).append("/images/create?fromImage=").append(image).append("&tag=").append(tag);
        Map headers = new HashMap();
        if (authentication != null) {
            headers.put("X-Registry-Auth", Base64.getEncoder().encodeToString(gson.toJson(authentication).getBytes(UTF_8)));
        }
        doPOST(path.toString(), "", headers);

        final ChunkedInputStream in = new ChunkedInputStream(Channels.newInputStream(socket));
        final InputStreamReader reader = new InputStreamReader(in);

        while (!in.isEof()) {
            consumer.accept(gson.fromJson(new JsonReader(reader), CreateImageInfo.class));
        }
    }

    /**
     * see https://docs.docker.com/engine/api/v1.32/#operation/ImageInspect
     */
    public Image imageInspect(String image) throws IOException {
        StringBuilder path = new StringBuilder("/v").append(version).append("/images/").append(image).append("/json");
        final HttpRestClient.Response response = doGET(path.toString());
        final Reader body = response.getBody();
        return gson.fromJson(body, Image.class);
    }

    /**
     * see https://docs.docker.com/engine/api/v1.32/#operation/ImageBuild
     * @param buildImageRequest
     */
    public void imageBuild(BuildImageRequest buildImageRequest, AuthConfig authentication, InputStream context, Consumer<BuildInfo> consumer) throws IOException {
        StringBuilder uri = new StringBuilder("/v").append(version).append("/build")
                .append("?q=").append(buildImageRequest.isQuiet())
                .append("&nocache=").append(buildImageRequest.isNocache())
                .append("&rm=").append(buildImageRequest.isRm())
                .append("&forcerm=").append(buildImageRequest.isForcerm())
                .append("&squash=").append(buildImageRequest.isSquash());

        if (buildImageRequest.getDockerfile() != null) {
            uri.append("&dockerfile=").append(buildImageRequest.getDockerfile());
        }
        if (buildImageRequest.getBuildargs() != null && !buildImageRequest.getBuildargs().isEmpty()) {
            uri.append("&buildargs=").append(gson.toJson(buildImageRequest.getBuildargs()));
        }
        if (buildImageRequest.getTag() != null) {
            uri.append("&t=").append(buildImageRequest.getTag());
        }
        if (buildImageRequest.getExtrahosts() != null) {
            uri.append("&extrahosts=").append(buildImageRequest.getExtrahosts());
        }
        if (buildImageRequest.getUlimits() != null) {
            uri.append("&ulimits=").append(buildImageRequest.getUlimits());
        }
        if (buildImageRequest.getRemote() != null) {
            uri.append("&remote=").append(buildImageRequest.getRemote());
        }
        if (buildImageRequest.getExtrahosts() != null) {
            uri.append("&extrahosts=").append(buildImageRequest.getExtrahosts());
        }
        if (buildImageRequest.getCachefrom() != null && buildImageRequest.getCachefrom().size() > 0) {
            uri.append("&cachefrom=").append(gson.toJson(buildImageRequest.getCachefrom()));
        }
        if (buildImageRequest.getPull() != null) {
            uri.append("&pull=").append(buildImageRequest.getPull());
        }
        if (buildImageRequest.getMemory() >= 0) {
            uri.append("&memory=").append(buildImageRequest.getMemory());
        }
        if (buildImageRequest.getMemswap() >= 0) {
            uri.append("&memswap=").append(buildImageRequest.getMemswap());
        }
        if (buildImageRequest.getCpushares() >= 0) {
            uri.append("&cpushares=").append(buildImageRequest.getCpushares());
        }
        if (buildImageRequest.getCpuperiod() >= 0) {
            uri.append("&cpuperiod=").append(buildImageRequest.getCpuperiod());
        }
        if (buildImageRequest.getCpuquota() >= 0) {
            uri.append("&cpuquota=").append(buildImageRequest.getCpuquota());
        }
        if (buildImageRequest.getCpusetcpus() != null) {
            uri.append("&cpusetcpus=").append(buildImageRequest.getCpusetcpus());
        }
        if (buildImageRequest.getShmsize() >= 0) {
            uri.append("&shmsize=").append(buildImageRequest.getShmsize());
        }
        if (buildImageRequest.getLabels() != null && !buildImageRequest.getLabels().isEmpty()) {
            uri.append("&labels=").append(gson.toJson(buildImageRequest.getLabels()));
        }
        if (buildImageRequest.getNetworkmode() != null) {
            uri.append("&networkmode=").append(buildImageRequest.getNetworkmode());
        }

        Map headers = new HashMap();
        headers.put("Content-Type", "application/x-tar");
        if (authentication != null) {
            headers.put("X-Registry-Auth", Base64.getEncoder().encodeToString(gson.toJson(authentication).getBytes(UTF_8)));
        }

        doPOST(uri.toString(), context, headers);

        final ChunkedInputStream in = new ChunkedInputStream(Channels.newInputStream(socket));
        final InputStreamReader reader = new InputStreamReader(in);

        while (!in.isEof()) {
            consumer.accept(gson.fromJson(new JsonReader(reader), BuildInfo.class));
        }
    }

    /**
     * see https://docs.docker.com/engine/api/v1.32/#operation/ImagePush
     */
    public void imagePush(String image, String tag, AuthConfig authentication, Consumer<PushImageInfo> consumer) throws IOException {
        if (tag == null) tag = "latest";
        StringBuilder path = new StringBuilder("/v").append(version).append("/images/").append(image).append("/push?tag=").append(tag);
        Map headers = new HashMap();
        headers.put("X-Registry-Auth", Base64.getEncoder().encodeToString(gson.toJson(authentication).getBytes(UTF_8)));
        doPOST(path.toString(), "", headers);

        final ChunkedInputStream in = new ChunkedInputStream(Channels.newInputStream(socket));
        final InputStreamReader reader = new InputStreamReader(in);

        while (!in.isEof()) {
            consumer.accept(gson.fromJson(new JsonReader(reader), PushImageInfo.class));
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DockerClient{");
        sb.append("version='").append(version).append('\'');
        sb.append('}');
        return sb.toString();
    }

}
