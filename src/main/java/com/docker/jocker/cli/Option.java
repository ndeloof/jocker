package com.docker.jocker.cli;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class Option {

    final String name;
    final String shortName;
    private final Consumer<String> setter;

    public Option(String name, String shortName, Consumer<String> setter) {
        this.name = name;
        this.shortName = shortName;
        this.setter = setter;
    }

    public int set(List<String> args) {
         setter.accept(args.get(0));
         return 1;
    }
}


