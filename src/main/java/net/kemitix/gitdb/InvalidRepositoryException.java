package net.kemitix.gitdb;

import java.nio.file.Path;

/**
 * Runtime exception thrown when attempting to open a repository that it not a GitDB repo.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
public class InvalidRepositoryException extends RuntimeException {

    /**
     * Constructor.
     *
     * @param message the reason the repo is invalid
     * @param path the location of the repo
     */
    public InvalidRepositoryException(final String message, final Path path) {
        super(String.format("Invalid GitDB repo: %s [%s]", message, path));
    }
}
