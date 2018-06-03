/*
  The MIT License (MIT)

  Copyright (c) 2018 Paul Campbell

  Permission is hereby granted, free of charge, to any person obtaining a copy of this software
  and associated documentation files (the "Software"), to deal in the Software without restriction,
  including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
  and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
  subject to the following conditions:

  The above copyright notice and this permission notice shall be included in all copies
  or substantial portions of the Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
  INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
  AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
  DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.kemitix.gitdb;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Main API for connecting to a Git repo as a database.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
public interface GitDB {

    /**
     * Initialise a new local gitdb.
     *
     * @param dbDir the path to initialise the local repo in
     * @return a GitDB instance for the created local gitdb
     * @throws IOException if there {@code dbDir} is a file or a non-empty directory
     */
    static GitDB initLocal(final Path dbDir) throws IOException {
        return new GitDBLocal(
                dbDir.toFile()
        );
    }

    /**
     * Open an existing local gitdb.
     *
     * @param dbDir the path to open as a local repo
     * @return a GitDB instance for the local gitdb
     */
    static GitDBLocal openLocal(final Path dbDir) {
        if (dbDir.toFile().isFile()) {
            throw new GitDBRepoNotFoundException("Not a directory", dbDir);
        }
        if (!dbDir.toFile().exists()) {
            throw new GitDBRepoNotFoundException("Directory not found", dbDir);
        }
        Repository repository = null;
        try {
            repository = Git.open(dbDir.toFile()).getRepository();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (repository != null && repository.isBare()) {
            return new GitDBLocal(repository);
        }
        throw new InvalidRepositoryException("Not a bare repo", dbDir);
    }

    /**
     * Select the named branch.
     *
     * @param name the branch to select
     * @return an Optional containing the branch if it exists
     */
    Optional<GitDBBranch> branch(String name);
}
