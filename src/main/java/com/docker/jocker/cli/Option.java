package com.docker.jocker.cli;

import java.util.List;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class Option {

    final String name;
    final String shortName;

    public Option(String name, String shortName) {
        this.name = name;
        this.shortName = shortName;
    }

    public void set(List<String> args) {

    }
}
