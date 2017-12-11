package it.dockins.jocker;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import it.dockins.jocker.io.ChunkedInputStream;
import it.dockins.jocker.io.DockerMultiplexedInputStream;
import it.dockins.jocker.io.ContentLengthInputStream;
import it.dockins.jocker.model.*;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.zip.GZIPOutputStream;

/**
 * Implement <a href="https://docs.docker.com/engine/api/v1.32">Docker API</a> using a plain old java
 * {@link Socket}.
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class DockerClient implements Closeable {

    private final Socket socket;

    private final Gson gson;

    private String version;

    public DockerClient(String dockerHost) throws IOException {
        this(dockerHost, null);
    }

    public DockerClient(String dockerHost, SSLContext ssl) throws IOException {

        URI uri = URI.create(dockerHost);
        if ("unix".equals(uri.getScheme())) {
            
            final AFUNIXSocketAddress unix = new AFUNIXSocketAddress(new File(uri.getPath()));
            //UnixSocketAddress address = new UnixSocketAddress("/var/run/docker.sock");
            // socket = UnixSocketChannel.open(address).socket();
            socket = AFUNIXSocket.newInstance();
            socket.connect(unix);
        } else {
            if (ssl == null) {
                socket = new Socket(uri.getHost(), uri.getPort());
            } else {
                socket = ssl.getSocketFactory().createSocket(uri.getHost(), uri.getPort());
            }
        }

        gson = new GsonBuilder().create();
        version = version().getApiVersion();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

    public SystemVersionResponse version() throws IOException {
        Response r = doGET("/version");
        return gson.fromJson(r.getBody(), SystemVersionResponse.class);
    }


    public SystemInfo info() throws IOException {
        Response r = doGET("/v"+version+"/info");
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

        Response r = doGET(path.toString());
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

        doPut(uri.toString(), tar);
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
    public ContainerCreateResponse containerCreate(ContainerSpec containerConfig, String name) throws IOException {

        StringBuilder path = new StringBuilder("/v").append(version).append("/containers/create");
        if (name != null) {
            path.append("?name=").append(name);
        }

        String spec = gson.toJson(containerConfig);
        Response r = doPOST(path.toString(), spec);
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
        Response r = doPOST(uri.toString());
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

        Response r = doGET(uri.toString());
        return new ChunkedInputStream(socket.getInputStream());
    }


    /**
     * see https://docs.docker.com/engine/api/v1.32/#operation/ContainerInspect
     */
    public ContainerInspectResponse containerInspect(String container) throws IOException {
        Response r = doGET("/v"+version+"/containers/"+container+"/json");
        final Reader body = r.getBody();
        return gson.fromJson(body, ContainerInspectResponse.class);
    }

    public String containerExec(String container, ExecConfig execConfig) throws IOException {
        StringBuilder path = new StringBuilder("/v").append(version).append("/containers/").append(container).append("/exec");
        String spec = gson.toJson(execConfig);
        Response r = doPOST(path.toString(), spec);
        return gson.fromJson(r.getBody(), IdResponse.class).getId();
    }


    public Streams execStart(String id, boolean detach, boolean tty) throws IOException {
        StringBuilder path = new StringBuilder("/v").append(version).append("/exec/").append(id).append("/start");
        doPOST(path.toString(), "{\"Detach\": "+detach+", \"Tty\": "+tty+"}");
        if (detach) return null;
        return new Streams() {

            @Override
            public InputStream stdout() throws IOException {
                return new DockerMultiplexedInputStream(socket.getInputStream());
            }

            @Override
            public OutputStream stdin() throws IOException {
                return socket.getOutputStream();
            }
        };
    }

    public ExecInspectResponse execInspect(String id) throws IOException {
        StringBuilder path = new StringBuilder("/v").append(version).append("/exec/").append(id).append("/json");
        Response r = doGET(path.toString());
        final Reader body = r.getBody();
        return gson.fromJson(body, ExecInspectResponse.class);
    }


    /** "pull" flavor of ImageCreate */
    public void imagePull(String image, String tag, AuthConfig authentication, Consumer<PullStatus> consumer) throws IOException {
        if (tag == null) tag = "latest";
        StringBuilder path = new StringBuilder("/v").append(version).append("/images/create?fromImage=").append(image).append("&tag=").append(tag);
        Map headers = new HashMap();
        if (authentication != null) {
            headers.put("X-Registry-Auth", Base64.getEncoder().encodeToString(gson.toJson(authentication).getBytes(StandardCharsets.UTF_8)));
        }
        doPOST(path.toString(), "", headers);

        final ChunkedInputStream in = new ChunkedInputStream(socket.getInputStream());
        final InputStreamReader reader = new InputStreamReader(in);

        while (!in.isEof()) {
            consumer.accept(gson.fromJson(new JsonReader(reader), PullStatus.class));
        }
    }

    public Image imageInspect(String image) throws IOException {
        StringBuilder path = new StringBuilder("/v").append(version).append("/images/").append(image).append("/json");
        final Response response = doGET(path.toString());
        final Reader body = response.getBody();
        return gson.fromJson(body, Image.class);
    }


    private Response doGET(String path) throws IOException {
        final OutputStream out = socket.getOutputStream();

        final PrintWriter w = new PrintWriter(out);
        w.println("GET " + path + " HTTP/1.1");
        w.println("Host: localhost");
        w.println();
        w.flush();

        return getResponse();

    }

    private Response doPOST(String path) throws IOException {
        return doPOST(path, "");
    }

    private Response doPOST(String path, String payload) throws IOException {
        return doPOST(path, payload, Collections.EMPTY_MAP);
    }

    private Response doPOST(String path, String payload, Map<String, String> headers) throws IOException {

        final OutputStream out = socket.getOutputStream();
        final byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);

        final PrintWriter w = new PrintWriter(out);
        w.println("POST " + path + " HTTP/1.1");
        w.println("Host: localhost");
        w.println("Content-Type: application/json; charset=utf-8");
        w.println("Content-Length: "+bytes.length);
        for (Map.Entry<String, String> header : headers.entrySet()) {
            w.println(header.getKey() +": "+header.getValue());
        }
        w.println();
        w.flush();
        out.write(bytes);

        return getResponse();
    }

    private Response doPut(String path, byte[] bytes) throws IOException {

        final OutputStream out = socket.getOutputStream();

        final PrintWriter w = new PrintWriter(out);
        w.println("PUT " + path + " HTTP/1.1");
        w.println("Host: localhost");
        w.println("Content-Type: application/gzip");
        w.println("Content-Length: "+bytes.length);
        w.println();
        w.flush();
        out.write(bytes);

        return getResponse();
    }

    private Response doDELETE(String path) throws IOException {

        final OutputStream out = socket.getOutputStream();

        final PrintWriter w = new PrintWriter(out);
        w.println("DELETE " + path + " HTTP/1.1");
        w.println("Host: localhost");
        w.println();
        w.flush();

        return getResponse();
    }

    private Response getResponse() throws IOException {

        final InputStream in = socket.getInputStream();
        int status = readHttpStatus(in);
        Map<String, String> headers = readHttpResponseHeaders(in);

        Response body;
        if (headers.containsKey("Content-Length")) {
            body = () -> readPayload(in, Integer.parseInt(headers.get("Content-Length")));
        } else if (headers.containsKey("Transfer-Encoding") && "chunked".equals(headers.get("Transfer-Encoding"))) {
            body = () -> readChunkedPayload(in);
        } else body = () -> new InputStreamReader(socket.getInputStream());

        if (status / 100 > 2) {
            String message = String.valueOf(status);
            final String type = headers.get("Content-Type");
            if (type != null && type.startsWith("application/json")) {
                message = gson.fromJson(body.getBody(), ErrorDetail.class).getMessage();
            }
            if (status == 404) {
                throw new NotFoundException(message);
            }
            if (status == 409) {
                throw new ConflictException(message);
            }
            throw new IOException(message);
        }

        return body;
    }

    private Map<String, String> readHttpResponseHeaders(InputStream in) throws IOException {
        String line;
        int responseCode;

        Map<String, String> headers = new HashMap<>();
        int length = -1;
        boolean chunked = false;
        while ((line = readLine(in)) != null) {
            if (line.length() == 0) break; // end of header
            final int x = line.indexOf(':');
            String header = line.substring(0, x);
            final String value = line.substring(x + 2);
            headers.put(header, value);
        }
        return headers;
    }

    private int readHttpStatus(InputStream in) throws IOException {
        String line = readLine(in);
        int i = line.indexOf(' ');
        int j = line.indexOf(' ',  i+1);
        return Integer.parseInt(line.substring(i+1,j));
    }

    private String readLine(final InputStream in) throws IOException {
        StringBuilder s = new StringBuilder();
        char c;
        while((c = (char) in.read()) != '\r') {
            s.append(c);
        }
        in.read(); // \n
        return s.toString();
    }

    private Reader readPayload(final InputStream in, int length) throws IOException {
        return new InputStreamReader(new ContentLengthInputStream(in, length), StandardCharsets.UTF_8);
    }

    private Reader readChunkedPayload(final InputStream in) throws IOException {
        return new InputStreamReader(new ChunkedInputStream(in), StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DockerClient{");
        sb.append("version='").append(version).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        final DockerClient client = new DockerClient("unix:///var/run/docker.sock");

        client.imagePull("jenkins/jenkins","lts", null, System.out::println);

        /*
        final String container = client.containerCreate(new ContainerSpec().image("ubuntu").cmd("sleep", "100"), null).getId();
        client.containerStart(container);
        Thread.sleep(1);
        client.putContainerFile(container, "/tmp/", false, new File("./pom.xml"));
        final String id = client.containerExec(container, new ExecConfig().cmd(Arrays.asList("cat", "/tmp/pom.xml")).attachStdout(true));
        final Streams streams = client.execStart(id, false, false);
        try {
            byte[] buffer = new byte[1024];
            int i;
            while ((i = streams.stdout().read(buffer, 0, 1024)) > 0) {
                System.out.println(new String(buffer, 0, i));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }             */
    }


    interface Response {
        Reader getBody() throws IOException;
    }
}
