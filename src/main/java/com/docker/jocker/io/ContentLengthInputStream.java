package com.docker.jocker.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class ContentLengthInputStream extends InputStream {

    private final InputStream in;
    private int length;

    public ContentLengthInputStream(InputStream in, int length) {
        this.in = in;
        this.length = length;
    }

    @Override
    public int read() throws IOException {
        if (length <= 0) return -1;
        length--;
        return in.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (length <= 0) return -1;
        final int read = in.read(b, off, Math.min(length, len));
        length -= read;
        return read;
    }
}
