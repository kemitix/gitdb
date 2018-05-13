package net.kemitix.gitdb;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Author {

    @Getter
    private final String name;
    @Getter
    private final String email;

    public static Author name(final String name, final String email) {
        return new Author(name, email);
    }
}
