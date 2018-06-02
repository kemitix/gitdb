package net.kemitix.gitdb;

import org.eclipse.jgit.errors.RepositoryNotFoundException;

import java.nio.file.Path;

/**
 * Runtime exception thrown when attempting to open to location that is not a GitDB repo.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
public class GitDBRepoNotFoundException extends RuntimeException {

    /**
     * Constructor.
     *
     * @param cause the original exception
     */
    GitDBRepoNotFoundException(final Path path, final RepositoryNotFoundException cause) {
        super(String.format("GitDB repo not found: %s", path.toString()), cause);
    }
}
