package net.kemitix.gitdb;

import net.kemitix.mon.TypeAlias;

public class Author extends TypeAlias<String> {
    protected Author(String value) {
        super(value);
    }

    public static Author name(final String name) {
        return new Author(name);
    }
}
