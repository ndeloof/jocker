package com.docker.jocker.io;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class ChunkedInputStreamTest {


    @Test
    public void readChunked() throws IOException {
        // from https://en.wikipedia.org/wiki/Chunked_transfer_encoding
        String s = "4\r\n" +
                "Wiki\r\n" +
                "5\r\n" +
                "pedia\r\n" +
                "E\r\n" +
                " in\r\n" +
                "\r\n" +
                "chunks.\r\n" +
                "0\r\n" +
                "\r\n";

        InputStream in = new ChunkedInputStream(new ByteArrayInputStream(s.getBytes()));
        final String out = IOUtils.toString(in);
        Assert.assertEquals("Wikipedia in\r\n\r\nchunks.", out);
    }
}
