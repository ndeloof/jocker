package com.docker.jocker.model;

import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public abstract class Filters {

    /* The API filter type is golang `map[string]map[string]bool` ¯\_(ツ)_/¯ */
    private Map<String, Map<String, Boolean>> args = new HashMap<>();

    protected void add(String key, String value) {
        Map<String, Boolean> entry = args.get(key);
        if (entry == null) {
            entry = new HashMap<>();
            args.put(key, entry);
        }
        entry.put(value, true);
    }

    public String encode(Gson gson) {
        try {
            return URLEncoder.encode(gson.toJson(args), StandardCharsets.ISO_8859_1.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("This Java runtime does not support ISO_8859_1", e);
        }
    }
}
