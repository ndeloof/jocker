package com.docker.jocker.io;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class TarInputStreamBuilder {


    private final ByteArrayOutputStream bos;
    private final TarArchiveOutputStream tar;

    public TarInputStreamBuilder() {
        bos = new ByteArrayOutputStream();
        tar = new TarArchiveOutputStream(bos);
    }

    public TarInputStreamBuilder add(String name, int mode, InputStream content, int length) throws IOException {
        TarArchiveEntry entry = new TarArchiveEntry(name);
        entry.setSize(length);
        entry.setMode(mode);
        tar.putArchiveEntry(entry);
        int read;
        byte[] buffer = new byte[4096];
        while ((read = content.read(buffer,0,4096)) >= 0) {
            tar.write(buffer, 0, read);
            length -= read;
        }
        if (length != 0) throw new IOException("Unexpected context size, expected "+length+"bytes, got "+length);
        tar.closeArchiveEntry();
        return this;
    }

    public TarInputStreamBuilder add(String name, int mode, InputStream content) throws IOException {
        add(name, mode, IOUtils.toByteArray(content));
        return this;
    }

    public TarInputStreamBuilder add(String name, int mode, byte[] bytes) throws IOException {
        TarArchiveEntry entry = new TarArchiveEntry(name);
        entry.setSize(bytes.length);
        entry.setMode(mode);
        tar.putArchiveEntry(entry);
        tar.write(bytes);
        tar.closeArchiveEntry();
        return this;
    }

    public InputStream build() throws IOException {
        tar.close();
        return new ByteArrayInputStream(bos.toByteArray());
    }


}
