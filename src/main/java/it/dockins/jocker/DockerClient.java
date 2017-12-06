package it.dockins.jocker;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.dockins.jocker.model.AuthConfig;
import it.dockins.jocker.model.ContainerInspectResponse;
import it.dockins.jocker.model.ExecConfig;
import it.dockins.jocker.model.ExecInspectResponse;
import it.dockins.jocker.model.IdResponse;
import it.dockins.jocker.model.ContainerSpec;
import it.dockins.jocker.model.ContainerCreateResponse;
import it.dockins.jocker.model.ContainerSummary;
import it.dockins.jocker.model.Image;
import it.dockins.jocker.model.PullStatus;
import it.dockins.jocker.model.SystemInfo;
import it.dockins.jocker.model.ContainersFilters;
import it.dockins.jocker.model.Streams;
import it.dockins.jocker.model.SystemVersionResponse;
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
import java.io.OutputStream;
import java.io.PrintWriter;
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
    public void putContainerArchive(String container, String path, boolean noOverwriteDirNonDir, File file) throws IOException {
        StringBuilder uri = new StringBuilder("/v").append(version)
                .append("/containers/").append(container).append("/archive")
                .append("?path=").append(path)
                .append("&noOverwriteDirNonDir=").append(noOverwriteDirNonDir);

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(bos);
             TarArchiveOutputStream tar = new TarArchiveOutputStream(gz);
             InputStream in = new FileInputStream(file)) {
            final ArchiveEntry entry = tar.createArchiveEntry(file, file.getName());
            tar.putArchiveEntry(entry);
            IOUtils.copy(in, tar);
            tar.closeArchiveEntry();
        }
        doPut(uri.toString(), bos.toByteArray());
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
        Response r = doPost(path.toString(), spec);
        return gson.fromJson(r.getBody(), ContainerCreateResponse.class);
    }

    /**
     * see https://docs.docker.com/engine/api/v1.32/#operation/ContainerStart
     */
    public void containerStart(String container) throws IOException {
        StringBuilder uri = new StringBuilder("/v").append(version)
                .append("/containers/").append(container).append("/start");
        doPost(uri.toString(), "");
    }

    /**
     * see https://docs.docker.com/engine/api/v1.32/#operation/ContainerDelete
     */
    public void containerDelete(String container, boolean volumes, boolean links, boolean force) throws IOException {
        StringBuilder uri = new StringBuilder("/v").append(version)
                .append("/containers/").append(container)
                .append("?v=").append(volumes)
                .append("?link=").append(links)
                .append("&force=").append(force);
        doDelete("/v"+version+"/containers/"+container);
    }


    /**
     * see https://docs.docker.com/engine/api/v1.32/#operation/ContainerInspect
     */
    public ContainerInspectResponse containerInspect(String container) throws IOException {
        Response r = doGET("/v"+version+"/containers/"+container+"/json");
        final String body = r.getBody();
        return gson.fromJson(body, ContainerInspectResponse.class);
    }

    public String containerExec(String container, ExecConfig execConfig) throws IOException {
        StringBuilder path = new StringBuilder("/v").append(version).append("/containers/").append(container).append("/exec");
        String spec = gson.toJson(execConfig);
        Response r = doPost(path.toString(), spec);
        return gson.fromJson(r.getBody(), IdResponse.class).getId();
    }


    public Streams execStart(String id, boolean detach, boolean tty) throws IOException {
        StringBuilder path = new StringBuilder("/v").append(version).append("/exec/").append(id).append("/start");
        doPost(path.toString(), "{\"Detach\": "+detach+", \"Tty\": "+tty+"}");
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
        final String body = r.getBody();
        return gson.fromJson(body, ExecInspectResponse.class);
    }


    /** "pull" flavor of ImageCreate */
    public void imagePull(String fromImage, AuthConfig authentication, Consumer<PullStatus> consumer) throws IOException {
        StringBuilder path = new StringBuilder("/v").append(version).append("/images/create?fromImage=").append(fromImage);
        Map headers = new HashMap();
        if (authentication != null) {
            headers.put("X-Registry-Auth", Base64.getEncoder().encodeToString(gson.toJson(authentication).getBytes(StandardCharsets.UTF_8)));
        }
        doPost(path.toString(), "", headers);
        readChunkedPayload(socket.getInputStream(), s -> {
            for (String event : s.split("\n")) {
                consumer.accept(gson.fromJson(event, PullStatus.class));
            }
        });
    }

    public Image imageInspect(String image) throws IOException {
        StringBuilder path = new StringBuilder("/v").append(version).append("/images/").append(image).append("/json");
        final Response response = doGET(path.toString());
        final String body = response.getBody();
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

    private Response doPost(String path, String payload) throws IOException {
        return doPost(path, payload, Collections.EMPTY_MAP);
    }

    private Response doPost(String path, String payload, Map<String, String> headers) throws IOException {

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

    private Response doDelete(String path) throws IOException {

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
        Map<String, String> headers = readHttpResponseHeaders(in);

        if (headers.containsKey("Content-Length")) {
            return () -> readPayload(in, Integer.parseInt(headers.get("Content-Length")));
        } else if (headers.containsKey("Transfer-Encoding") && "chunked".equals(headers.get("Transfer-Encoding"))) {
            return () -> {
                final StringBuilder s = new StringBuilder();
                readChunkedPayload(in, s::append);
                return s.toString();
            };
        }
        return null;
    }

    private Map<String, String> readHttpResponseHeaders(InputStream in) throws IOException {
        String line = readLine(in);
        int i = line.indexOf(' ');
        int j = line.indexOf(' ',  i+1);
        int responseCode = Integer.parseInt(line.substring(i+1,j));
        String responseMessage = line.substring(j+1);

        if (responseCode == 404) {
            throw new NotFoundException();
        }
        if (responseCode / 100 > 2) {
            throw new IOException(line);
        }

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

    private String readLine(final InputStream in) throws IOException {
        StringBuilder s = new StringBuilder();
        char c;
        while((c = (char) in.read()) != '\r') {
            s.append(c);
        }
        in.read(); // \n
        return s.toString();
    }

    private String readPayload(final InputStream in, int length) throws IOException {
            byte[] payload = new byte[length];
            int read = 0;
            while(read < length) {
                read += in.read(payload, read, length);
            }
            return new String(payload);
    }

    private void readChunkedPayload(final InputStream in, Consumer<String> consumer) throws IOException {
            int length = 0;
            do {
                final String chunk = readLine(in);
                length = Integer.parseInt(chunk, 16);
                byte[] data = new byte[length];
                int read = 0;
                while(read < length) {
                    read += in.read(data, read, length);
                }
                readLine(in); // CTRLF
                consumer.accept(new String(data, StandardCharsets.UTF_8));
            } while(length > 0);
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

        client.imagePull("jenkins/jenkins:lts", null, System.out::println);

        /**
        final String container = client.containerCreate(new ContainerSpec().image("ubuntu").cmd("sleep", "100"), null).getId();
        client.containerStart(container);
        Thread.sleep(1);
        client.putContainerArchive(container, "/tmp/", false, new File("./pom.xml"));
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
        }   */
    }


    interface Response {
        String getBody() throws IOException;
    }
}
