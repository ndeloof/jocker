package com.docker.jocker.cli;

import java.util.ListIterator;
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

    public void set(ListIterator<String> args) {
         setter.accept(args.next());
         args.remove();
    }
}


