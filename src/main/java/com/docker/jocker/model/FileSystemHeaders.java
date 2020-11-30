package com.docker.jocker.model;

import java.util.HashMap;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class FileSystemHeaders extends HashMap<String, String> {

    public String getName() {
        return get("name");
    }

    public long getSize() {
        return Long.parseLong(get("size"));
    }

    public String getMode() {
        return get("mode");
    }

}
