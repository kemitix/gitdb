package net.kemitix.gitdb;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Document<T>  {

    public static <T> Document<T> create(Key key, T value) {
        return new Document<>(key, value);
    }

    @Getter
    private final Key key;

    @Getter
    private final T value;
}
