package io.timeywimey.jocker;


import com.google.gson.stream.JsonReader;
import io.timeywimey.jocker.model.*;
import io.timeywimey.jocker.io.ChunkedInputStream;
import io.timeywimey.jocker.io.DockerMultiplexedInputStream;
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
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.zip.GZIPOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Implement <a href="https://docs.docker.com/engine/api/v1.40">Docker API</a> using a plain old java
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

    public SystemVersion version() throws IOException {
        HttpRestClient.Response r = doGET("/version");
        return gson.fromJson(r.getBody(), SystemVersion.class);
    }

    public SystemInfo info() throws IOException {
        HttpRestClient.Response r = doGET("/v"+version+"/info");
        return gson.fromJson(r.getBody(), SystemInfo.class);
    }

    /**
     * see https://docs.docker.com/engine/api/v1.40/#operation/ContainerList
     */
    public ContainerSummary containerList(boolean all, int limit, boolean size, ContainersFilters filters) throws IOException {
        return containerList(all, limit, size, gson.toJson(filters));
    }

    /**
     * see https://docs.docker.com/engine/api/v1.40/#operation/ContainerList
     */
    public ContainerSummary containerList(boolean all, int limit, boolean size, String filters) throws IOException {
        Request req = Request("/v", version, "/containers/json")
            .query("all", all)
            .query("size", size);
        if (limit > 0) req.query("limit", limit);
        if (filters != null) req.query("filters", filters);

        HttpRestClient.Response r = doGET(req.toString());
        return gson.fromJson(r.getBody(), ContainerSummary.class);
    }

    /**
     * copy a single file to a container
     * see https://docs.docker.com/engine/api/v1.40/#operation/PutContainerArchive
     */
    public void putContainerArchive(String container, String path, boolean noOverwriteDirNonDir, byte[] tar) throws IOException {
        Request req = Request("/v", version, "/containers/", container, "/archive")
            .query("path", path)
            .query("noOverwriteDirNonDir", noOverwriteDirNonDir);

        doPUT(req.toString(), tar);
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
     * see https://docs.docker.com/engine/api/v1.40/#operation/ContainerCreate
     */
    public ContainerCreateResponse containerCreate(ContainerConfig containerConfig, String name) throws IOException {
        Request req = Request("/v", version, "/containers/create");
        if (name != null) {
            req.query("name", name);
        }

        String spec = gson.toJson(containerConfig);
        HttpRestClient.Response r = doPOST(req.toString(), spec);
        return gson.fromJson(r.getBody(), ContainerCreateResponse.class);
    }

    /**
     * see https://docs.docker.com/engine/api/v1.40/#operation/ContainerStart
     */
    public void containerStart(String container) throws IOException {
        Request req = Request("/v", version, "/containers/", container, "/start");
        doPOST(req.toString());
    }

    /**
     * see https://docs.docker.com/engine/api/v1.40/#operation/ContainerStop
     */
    public void containerStop(String container, int timeout) throws IOException {
        Request req = Request("/v", version, "/containers/", container, "/stop").query("t", timeout);
        doPOST(req.toString());
    }

    /**
     * see https://docs.docker.com/engine/api/v1.40/#operation/ContainerRestart
     */
    public void containerRestart(String container, int timeout) throws IOException {
        Request req = Request("/v", version, "/containers/", container, "/restart").query("t", timeout);
        doPOST(req.toString());
    }

    /**
     * see https://docs.docker.com/engine/api/v1.40/#operation/ContainerResize
     */
    public void containerResize(String container, int height, int width) throws IOException {
        Request req = Request("/v", version, "/containers/", container, "/resize")
                .query("h", height)
                .query("w", width);
        doPOST(req.toString());
    }

    /**
     * see https://docs.docker.com/engine/api/v1.40/#operation/ContainerChanges
     */
    public ContainerChangeResponseItem[] containerChanges(String container, boolean stream) throws IOException {
        Request req = Request("/v", version, "/containers/", container, "/changes");
        final HttpRestClient.Response response = doGET(req.toString());
        return gson.fromJson(response.getBody(), ContainerChangeResponseItem[].class);
    }

    /**
     * see https://docs.docker.com/engine/api/v1.40/#operation/ContainerStats
     * FIXME no model in swagger definition.
     */
    // public void containerStats(String container, boolean stream) throws IOException {

    /**
     * see https://docs.docker.com/engine/api/v1.40/#operation/ContainerKill
     */
    public void containerKill(String container, String signal) throws IOException {
        Request req = Request("/v", version, "/containers/", container, "/kill");
        if (signal != null) {
            req.query("signal", signal);
        }
        doPOST(req.toString());
    }

    /**
     * see https://docs.docker.com/engine/api/v1.40/#operation/ContainerKill
     */
    public void containerRename(String container, String name) throws IOException {
        Request req = Request("/v", version, "/containers/", container, "/rename").query("name", name);
        doPOST(req.toString());
    }

    /**
     * see https://docs.docker.com/engine/api/v1.40/#operation/ContainerPause
     */
    public void containerPause(String container) throws IOException {
        Request req = Request("/v", version, "/containers/", container, "/pause");
        doPOST(req.toString());
    }

    /**
     * see https://docs.docker.com/engine/api/v1.40/#operation/ContainerUnpause
     */
    public void containerUnpause(String container) throws IOException {
        Request req = Request("/v", version, "/containers/", container, "/unpause");
        doPOST(req.toString());
    }

    /**
     * see https://docs.docker.com/engine/api/v1.40/#operation/ContainerWait
     */
    public ContainerWaitResponse containerWait(String container, WaitCondition condition) throws IOException {
        Request req = Request("/v", version, "/containers/", container, "/wait");
        if (condition != null) {
            req.query("condition", condition.getValue());
        }
        HttpRestClient.Response r = doPOST(req.toString());
        return gson.fromJson(r.getBody(), ContainerWaitResponse.class);
    }

    /**
     * see https://docs.docker.com/engine/api/v1.40/#operation/ContainerDelete
     */
    public void containerDelete(String container, boolean volumes, boolean links, boolean force) throws IOException {
        Request req = Request("/v", version, "/containers/", container)
                .query("force", force)
                .query("v", volumes)
                .query("link", links);

        doDELETE(req.toString());
    }

    public InputStream containerLogs(String container, boolean follow, boolean stdout, boolean stderr, boolean timestamps, int since, String tail) throws IOException {
        Request req = Request("/v", version, "/containers/", container, "/logs")
                .query("follow", follow)
                .query("stdout", stdout)
                .query("stderr", stderr)
                .query("since", since);
        if (tail != null) {
            req.query("tail", tail);
        }

        HttpRestClient.Response r = doGET(req.toString());
        return new ChunkedInputStream(Channels.newInputStream(socket));
    }

    public Streams containerAttach(String id, boolean stdin, boolean stdout, boolean stderr, boolean stream, boolean logs, String detachKeys) throws IOException {
        Request req = Request("/v", version, "/containers/", id, "/attach")
                .query("stdin", stdin)
                .query("stdout", stdout)
                .query("stderr", stderr)
                .query("stream", stream)
                .query("logs", logs);
        if (detachKeys != null) {
            req.query("detachKeys", detachKeys);
        }

        final HttpRestClient.Response response = doPOST(req.toString());
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
     * see https://docs.docker.com/engine/api/v1.40/#operation/ContainerInspect
     */
    public ContainerInspectResponse containerInspect(String container) throws IOException {
        HttpRestClient.Response r = doGET("/v"+version+"/containers/"+container+"/json");
        final Reader body = r.getBody();
        return gson.fromJson(body, ContainerInspectResponse.class);
    }

    /**
     * see https://docs.docker.com/engine/api/v1.40/#operation/ContainerExec
     */
    public String containerExec(String container, ExecConfig execConfig) throws IOException {
        Request req = Request("/v", version, "/containers/", container, "/exec");
        String spec = gson.toJson(execConfig);
        HttpRestClient.Response r = doPOST(req.toString(), spec);
        return gson.fromJson(r.getBody(), IdResponse.class).getId();
    }

    /**
     * see https://docs.docker.com/engine/api/v1.40/#operation/ContainerArchiveInfo
     */
    public FileSystemHeaders containerArchiveInfo(String container, String path) throws IOException {
        Request req = Request("/v", version, "/containers/", container, "/archive").query("path", path);
        HttpRestClient.Response r = doHEAD(req.toString());
        final String stats = r.getHeaders().get("X-Docker-Container-Path-Stat");
        final byte[] json = Base64.getDecoder().decode(stats.getBytes(UTF_8));

        try (InputStream stream = new ByteArrayInputStream(json);
             Reader reader = new InputStreamReader(stream)) {
            return gson.fromJson(reader, FileSystemHeaders.class);
        }
    }

    /**
     * see https://docs.docker.com/engine/api/v1.40/#operation/ContainerArchive
     */
    public TarArchiveInputStream containerArchive(String container, String path) throws IOException {
        Request req = Request("/v", version, "/containers/", container, "/archive").query("path", path);
        HttpRestClient.Response r = doGET(req.toString());
        return new TarArchiveInputStream(new ChunkedInputStream(Channels.newInputStream(socket)));
    }

    /**
     * see https://docs.docker.com/engine/api/v1.40/#operation/ContainerPrune
     */
    public ContainerPruneResponse containerPrune(ContainersFilters filters) throws IOException {
        Request req = Request("/v", version, "/containers/prune");
        if (filters != null) {
            req.query("filters", gson.toJson(filters));
        }
        HttpRestClient.Response r = doPOST(req.toString());
        return gson.fromJson(r.getBody(), ContainerPruneResponse.class);
    }

    /**
     * see https://docs.docker.com/engine/api/v1.40/#operation/ExecStart
     */
    public Streams execStart(String id, boolean detach, boolean tty) throws IOException {
        Request req = Request("/v", version, "/exec/", id, "/start");
        final HttpRestClient.Response response = doPOST(req.toString(), "{\"Detach\": " + detach + ", \"Tty\": " + tty + "}");
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
     * see https://docs.docker.com/engine/api/v1.40/#operation/ExecInspect
     */
    public ExecInspectResponse execInspect(String id) throws IOException {
        Request req = Request("/v", version, "/exec/", id, "/json");
        HttpRestClient.Response r = doGET(req.toString());
        final Reader body = r.getBody();
        return gson.fromJson(body, ExecInspectResponse.class);
    }


    /**
     * "pull" flavor of ImageCreate
     * see https://docs.docker.com/engine/api/v1.40/#operation/ImageCreate
     */
    public void imagePull(String image, String tag, AuthConfig authentication, Consumer<CreateImageInfo> consumer) throws IOException {
        if (tag == null) tag = "latest";
        Request req = Request("/v", version, "/images/create")
                .query("fromImage", image)
                .query("tag", tag);
        Map headers = new HashMap();
        if (authentication != null) {
            headers.put("X-Registry-Auth", Base64.getEncoder().encodeToString(gson.toJson(authentication).getBytes(UTF_8)));
        }
        doPOST(req.toString(), "", headers);

        final ChunkedInputStream in = new ChunkedInputStream(Channels.newInputStream(socket));
        final InputStreamReader reader = new InputStreamReader(in);

        while (!in.isEof()) {
            consumer.accept(gson.fromJson(new JsonReader(reader), CreateImageInfo.class));
        }
    }

    /**
     * see https://docs.docker.com/engine/api/v1.40/#operation/ImageInspect
     */
    public Image imageInspect(String image) throws IOException {
        Request req = Request("/v", version, "/images/", image, "/json");
        final HttpRestClient.Response response = doGET(req.toString());
        final Reader body = response.getBody();
        return gson.fromJson(body, Image.class);
    }

    /**
     * see https://docs.docker.com/engine/api/v1.40/#operation/ImageBuild
     * @param buildImageRequest
     */
    public void imageBuild(BuildImageRequest buildImageRequest, AuthConfig authentication, InputStream context, Consumer<BuildInfo> consumer) throws IOException {
        Request req = Request("/v", version, "/build")
                .query("q", buildImageRequest.isQuiet())
                .query("nocache", buildImageRequest.isNocache())
                .query("rm", buildImageRequest.isRm())
                .query("forcerm", buildImageRequest.isForcerm())
                .query("squash", buildImageRequest.isSquash());

        if (buildImageRequest.getDockerfile() != null) {
            req.query("dockerfile", buildImageRequest.getDockerfile());
        }
        if (buildImageRequest.getBuildargs() != null && !buildImageRequest.getBuildargs().isEmpty()) {
            req.query("buildargs", gson.toJson(buildImageRequest.getBuildargs()));
        }
        if (buildImageRequest.getTag() != null) {
            req.query("t", buildImageRequest.getTag());
        }
        if (buildImageRequest.getExtrahosts() != null) {
            req.query("extrahosts", buildImageRequest.getExtrahosts());
        }
        if (buildImageRequest.getUlimits() != null) {
            req.query("ulimits", buildImageRequest.getUlimits());
        }
        if (buildImageRequest.getRemote() != null) {
            req.query("remote", buildImageRequest.getRemote());
        }
        if (buildImageRequest.getExtrahosts() != null) {
            req.query("extrahosts", buildImageRequest.getExtrahosts());
        }
        if (buildImageRequest.getCachefrom() != null && buildImageRequest.getCachefrom().size() > 0) {
            req.query("cachefrom", gson.toJson(buildImageRequest.getCachefrom()));
        }
        if (buildImageRequest.getPull() != null) {
            req.query("pull", buildImageRequest.getPull());
        }
        if (buildImageRequest.getMemory() >= 0) {
            req.query("memory", buildImageRequest.getMemory());
        }
        if (buildImageRequest.getMemswap() >= 0) {
            req.query("memswap", buildImageRequest.getMemswap());
        }
        if (buildImageRequest.getCpushares() >= 0) {
            req.query("cpushares", buildImageRequest.getCpushares());
        }
        if (buildImageRequest.getCpuperiod() >= 0) {
            req.query("cpuperiod", buildImageRequest.getCpuperiod());
        }
        if (buildImageRequest.getCpuquota() >= 0) {
            req.query("cpuquota", buildImageRequest.getCpuquota());
        }
        if (buildImageRequest.getCpusetcpus() != null) {
            req.query("cpusetcpus", buildImageRequest.getCpusetcpus());
        }
        if (buildImageRequest.getShmsize() >= 0) {
            req.query("shmsize", buildImageRequest.getShmsize());
        }
        if (buildImageRequest.getLabels() != null && !buildImageRequest.getLabels().isEmpty()) {
            req.query("labels", gson.toJson(buildImageRequest.getLabels()));
        }
        if (buildImageRequest.getNetworkmode() != null) {
            req.query("networkmode", buildImageRequest.getNetworkmode());
        }

        Map headers = new HashMap();
        headers.put("Content-Type", "application/x-tar");
        if (authentication != null) {
            headers.put("X-Registry-Auth", Base64.getEncoder().encodeToString(gson.toJson(authentication).getBytes(UTF_8)));
        }

        doPOST(req.toString(), context, headers);

        final ChunkedInputStream in = new ChunkedInputStream(Channels.newInputStream(socket));
        final InputStreamReader reader = new InputStreamReader(in);

        while (!in.isEof()) {
            consumer.accept(gson.fromJson(new JsonReader(reader), BuildInfo.class));
        }
    }

    /**
     * see https://docs.docker.com/engine/api/v1.40/#operation/ImagePush
     */
    public void imagePush(String image, String tag, AuthConfig authentication, Consumer<PushImageInfo> consumer) throws IOException {
        if (tag == null) tag = "latest";
        Request req = Request("/v", version, "/images/", image, "/push").query("tag", tag);
        Map headers = new HashMap();
        headers.put("X-Registry-Auth", Base64.getEncoder().encodeToString(gson.toJson(authentication).getBytes(UTF_8)));
        doPOST(req.toString(), "", headers);

        final ChunkedInputStream in = new ChunkedInputStream(Channels.newInputStream(socket));
        final InputStreamReader reader = new InputStreamReader(in);

        while (!in.isEof()) {
            consumer.accept(gson.fromJson(new JsonReader(reader), PushImageInfo.class));
        }
    }

    /**
     * see https://docs.docker.com/engine/api/v1.40/#operation/Volumes
     */
    public VolumeListResponse volumeList(VolumeFilters filters) throws IOException {
        Request req = Request("/v", version, "/volumes");
        if (filters != null) req.query("filters", filters.encode(gson));
        HttpRestClient.Response r = doGET(req.toString());
        return gson.fromJson(r.getBody(), VolumeListResponse.class);
    }

    /**
     * see https://docs.docker.com/engine/api/v1.40/#operation/VolumesCreate
     */
    public Volume volumeCreate(VolumeConfig volume) throws IOException {
        Request req = Request("/v", version, "/volumes/create");
        HttpRestClient.Response r = doPOST(req.toString(), gson.toJson(volume));
        return gson.fromJson(r.getBody(), Volume.class);
    }

    /**
     * see https://docs.docker.com/engine/api/v1.40/#operation/VolumeInspect
     */
    public Volume volumeInspect(String name) throws IOException {
        Request req = Request("/v", version, "/volumes/", name);
        HttpRestClient.Response r = doGET(req.toString());
        return gson.fromJson(r.getBody(), Volume.class);
    }

    /**
     * see https://docs.docker.com/engine/api/v1.40/#operation/VolumeDelete
     * @return
     */
    public void volumeDelete(String name) throws IOException {
        Request req = Request("/v", version, "/volumes/", name);
        HttpRestClient.Response r = doDELETE(req.toString());
    }


    /**
     * see https://docs.docker.com/engine/api/v1.40/#operation/NetworkList
     */
    public List<Network> networkList(NetworkFilters filters) throws IOException {
        Request req = Request("/v", version, "/networks");
        if (filters != null) req.query("filters", filters.encode(gson));
        HttpRestClient.Response r = doGET(req.toString());
        return gson.fromJson(r.getBody(), NetworkList.class);
    }

    /**
     * see https://docs.docker.com/engine/api/v1.40/#operation/VolumesCreate
     */
    public Network networkCreate(NetworkConfig volume) throws IOException {
        Request req = Request("/v", version, "/networks/create");
        HttpRestClient.Response r = doPOST(req.toString(), gson.toJson(volume));
        return gson.fromJson(r.getBody(), Network.class);
    }

    /**
     * see https://docs.docker.com/engine/api/v1.40/#operation/NetworkInspect
     */
    public Network networkInspect(String id) throws IOException {
        Request req = Request("/v", version, "/networks/", id);
        HttpRestClient.Response r = doGET(req.toString());
        return gson.fromJson(r.getBody(), Network.class);
    }

    public void networkConnect(String id, String container) throws IOException {
        networkConnect(id, container, null);
    }
    /**
     * see https://docs.docker.com/engine/api/v1.40/#operation/NetworkConnect
     */
    public void networkConnect(String id, String container, EndpointSettings endpoint) throws IOException {
        Request req = Request("/v", version, "/networks/", id, "/connect");
        String body = gson.toJson(new Container().container(container).endpointConfig(endpoint));
        HttpRestClient.Response r = doPOST(req.toString(), body);
    }

    public void networkDisconnect(String id, String container) throws IOException {
        networkDisconnect(id, container, false);
    }

    /**
     * see https://docs.docker.com/engine/api/v1.40/#operation/NetworkDisconnect
     */
    public void networkDisconnect(String id, String container, boolean force) throws IOException {
        Request req = Request("/v", version, "/networks/", id, "/disconnect");
        String body = gson.toJson(new Container1().container(container).force(force));
        HttpRestClient.Response r = doPOST(req.toString(), body);
    }

    /**
     * see https://docs.docker.com/engine/api/v1.40/#operation/NetworkDelete
     * @return
     */
    public void networkDelete(String name) throws IOException {
        Request req = Request("/v", version, "/networks/", name);
        HttpRestClient.Response r = doDELETE(req.toString());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DockerClient{");
        sb.append("version='").append(version).append('\'');
        sb.append('}');
        return sb.toString();
    }

    private Request Request(String ... strings)  {
        final Request Request = new Request();
        for (String s : strings) {
            Request.sb.append(s);
        }
        return Request;
    }

    private class Request {
        final StringBuilder sb = new StringBuilder();
        private boolean q;

        private Request query(String name, Object value) {
            sb.append( q ? "&": "?");
            sb.append(name);
            sb.append("=");
            sb.append(value);
            q = true;
            return this;
        }

        @Override
        public String toString() {
            return sb.toString();
        }
    }
}
