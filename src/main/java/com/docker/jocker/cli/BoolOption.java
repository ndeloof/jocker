package com.docker.jocker.cli;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class BoolOption extends Option {

    private final Consumer<Boolean> setter;

    public BoolOption(String name, String shortName, Consumer<Boolean> setter) {
        super(name, shortName, null);
        this.setter = setter;
    }

    public int set(List<String> args) {
         setter.accept(true);
         return 0;
    }
}


