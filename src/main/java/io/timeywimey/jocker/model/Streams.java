package io.timeywimey.jocker.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public interface Streams {

    InputStream stdout() throws IOException;

    OutputStream stdin() throws IOException;
}
