package io.timeywimey.jocker.io;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class ContentLengthInputStreamTest {


    @Test
    public void readContentLength() throws IOException {
        String s = "Content-LengthXXXXXXXXXX";

        InputStream in = new ContentLengthInputStream(new ByteArrayInputStream(s.getBytes()), 14);
        final String out = IOUtils.toString(in);
        Assert.assertEquals("Content-Length", out);
    }
}
