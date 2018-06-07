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
import org.eclipse.jgit.lib.Repository;

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
        try {
            return Optional.of(Git.open(dbDir.toFile()))
                    .map(Git::getRepository)
                    .filter(Repository::isBare)
                    .map(GitDBLocal::new)
                    .orElseThrow(() -> new InvalidRepositoryException("Not a bare repo", dbDir));
        } catch (IOException e) {
            throw new InvalidRepositoryException("Error opening repository", dbDir, e);
        }
    }

    /**
     * Select the named branch.
     *
     * @param name the branch to select
     * @return an Optional containing the branch if it exists
     * @throws IOException if there is an error accessing the branch name
     */
    Optional<GitDBBranch> branch(String name) throws IOException;
}
