package it.dockins.jocker;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.dockins.jocker.model.ContainerSpec;
import it.dockins.jocker.model.ContainerCreateResponse;
import it.dockins.jocker.model.ContainerInspect;
import io.dockins.jocker.model.ContainerSummary;
import io.dockins.jocker.model.SystemInfo;
import it.dockins.jocker.model.Filters;
import it.dockins.jocker.model.Version;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Implement <a href="https://docs.docker.com/engine/api/v1.32">Docker API</a> using a plain old java
 * {@link Socket}.
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class DockerClient {

    private final Socket socket;

    private final Gson gson;

    private String version;

    public DockerClient(URI uri) throws IOException {
        this(uri, null);
    }
    public DockerClient(URI uri, SSLContext ssl) throws IOException {

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


    public Version version() throws IOException {
        return gson.fromJson(doGET("/version"), Version.class);
    }


    public SystemInfo info() throws IOException {
        return gson.fromJson(doGET("/v"+version+"/info"), SystemInfo.class);
    }

    /**
     * see https://docs.docker.com/engine/api/v1.32/#operation/ContainerList
     */
    public ContainerSummary containerList(boolean all, int limit, boolean size, Filters filters) throws IOException {
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

        String json = doGET(path.toString());
        return gson.fromJson(json, ContainerSummary.class);
    }

    /**
     * see https://docs.docker.com/engine/api/v1.32/#operation/ContainerCreate
     */
    public ContainerCreateResponse containerCreate(String name, ContainerSpec containerConfig) throws IOException {

        StringBuilder path = new StringBuilder("/v").append(version).append("/containers/create");
        if (name != null) {
            path.append("?name=").append(name);
        }

        String spec = gson.toJson(containerConfig);
        String json = doPost(path.toString(), spec);
        return gson.fromJson(json, ContainerCreateResponse.class);
    }

    /**
     * see https://docs.docker.com/engine/api/v1.32/#operation/ContainerInspect
     */
    public ContainerInspect containerInspect(String id) throws IOException {
        return gson.fromJson(doGET("/v"+version+"/containers/"+id+"/json"), ContainerInspect.class);
    }


    private String doPost(String path, String payload) throws IOException {

        final InputStream in = socket.getInputStream();
        final OutputStream out = socket.getOutputStream();
        final byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);

        final PrintWriter w = new PrintWriter(out);
        w.println("POST " + path + " HTTP/1.1");
        w.println("Host: localhost");
        w.println("Content-Type: application/json; charset=utf-8");
        w.println("Content-Length: "+bytes.length);
        w.println();
        w.flush();
        out.write(bytes);

        return getResponse(in);
    }

    private String doGET(String path) throws IOException {
        final InputStream in = socket.getInputStream();
        final OutputStream out = socket.getOutputStream();

        final PrintWriter w = new PrintWriter(out);
        w.println("GET " + path + " HTTP/1.1");
        w.println("Host: localhost");
        w.println();
        w.flush();

        return getResponse(in);
    }

    private String getResponse(InputStream in) throws IOException {

        String line = readLine(in);
        System.out.println("> " + line);
        int i = line.indexOf(' ');
        int j = line.indexOf(' ',  i+1);
        int responseCode = Integer.parseInt(line.substring(i+1,j));
        String responseMessage = line.substring(j+1);

        if (responseCode / 100 > 2) {
            throw new IOException(line);
        }

        int length = -1;
        boolean chunked = false;
        while ((line = readLine(in)) != null) {
            if (line.length() == 0) break; // end of header
            final int x = line.indexOf(':');
            String header = line.substring(0, x);
            final String value = line.substring(x + 2);
            if ("Content-Length".equals(header)) {
                length = Integer.parseInt(value);
            }
            if ("Transfer-Encoding".equals(header) && "chunked".equals(value)) {
                chunked = true;
            }
        }

        if (length > 0) {
            return readPayload(in, length);
        } else if (chunked) {
            return readChunkedPayload(in);
        } else {
            throw new IOException("Unexpected Transfer-Encoding");
        }
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

    private String readChunkedPayload(final InputStream in) throws IOException {
            final StringBuilder s = new StringBuilder();
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
                s.append(new String(data, StandardCharsets.UTF_8));
            } while(length > 0);
            return s.toString();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DockerClient{");
        sb.append("version='").append(version).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        System.out.println(new DockerClient(new URI("unix:///var/run/docker.sock")).ContainerList(true, 0, true, null));
    }

}
