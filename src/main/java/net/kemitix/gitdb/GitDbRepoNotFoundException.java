package net.kemitix.gitdb;

import org.eclipse.jgit.errors.RepositoryNotFoundException;

import java.nio.file.Path;

/**
 * Runtime exception thrown when attempting to open to location that is not a GitDB repo.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
public class GitDbRepoNotFoundException extends RuntimeException {

    /**
     * Constructor.
     *
     * @param path the path that is not a valid GitDB repo
     * @param cause the original exception
     */
    GitDbRepoNotFoundException(final Path path, final RepositoryNotFoundException cause) {
        super(cause);
    }
}
