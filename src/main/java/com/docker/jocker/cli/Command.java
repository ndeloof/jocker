package com.docker.jocker.cli;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public abstract class Command {

    final String name;
    final Collection<Command> subCommands;
    final Collection<Option> options;

    public Command(String name) {
        this.name = name;
        this.subCommands = new ArrayList<>();
        this.options = new ArrayList<>();
    }

    protected Command options(Option...options) {
        for (Option option : options) {
            this.options.add(option);
        }
        return this;
    }

    protected Command subCommands(Command...commands) {
        for (Command command : commands) {
            this.subCommands.add(command);
        }
        return this;
    }

    protected void parse(List<String> args) throws IOException {
        ListIterator<String> it = args.listIterator();
        ARGS: while (it.hasNext()) {
            String arg = it.next();
            if (arg.startsWith("--")) {
                if (applyOption(args, it, arg.substring(2))) continue;
                throw new IllegalArgumentException("unknown option "+arg);
            }
            if (arg.startsWith("-")) {
                if (applyOption(args, it, arg.substring(1))) continue ARGS;
                throw new IllegalArgumentException("unknown option "+arg);
            }
            for (Command c : subCommands) {
                if (c.name.equals(arg)) {
                    it.remove();
                    c.parse(args);
                    return;
                }
            }
            run(args);
            return;
        }
    }

    private boolean applyOption(List<String> args, ListIterator<String> it, String arg) {
        for (Option option : options) {
            if (option.name.equals(arg)) {
                it.remove();
                int i = option.set(args);
                while (i-- > 0) {
                    it.remove();
                }
                return true;
            }
        }
        return false;
    }

    abstract void run(List<String> args) throws IOException;

}
