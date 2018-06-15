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

import com.github.zafarkhaja.semver.Version;
import net.kemitix.gitdb.impl.LocalGitDB;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Main API for connecting to a Git repo as a database.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
public interface GitDB {

    int MAJOR = 1;
    int MINOR = 0;
    int PATCH = 0;

    Version VERSION = Version.forIntegers(MAJOR, MINOR, PATCH);

    /**
     * Initialise a new local gitdb.
     *
     * @param dbDir            the path to initialise the local repo in
     * @param userName         the user name
     * @param userEmailAddress the user email address
     * @return a GitDB instance for the created local gitdb
     * @throws IOException if there {@code dbDir} is a file or a non-empty directory
     */
    static GitDB initLocal(
            final Path dbDir,
            final String userName,
            final String userEmailAddress
    ) throws IOException {
        return LocalGitDB.init(dbDir, userName, userEmailAddress);
    }

    /**
     * Open an existing local gitdb.
     *
     * @param dbDir            the path to open as a local repo
     * @param userName         the user name
     * @param userEmailAddress the user email address
     * @return a GitDB instance for the local gitdb
     */
    static GitDB openLocal(final Path dbDir, final String userName, final String userEmailAddress) {
        return LocalGitDB.open(dbDir, userName, userEmailAddress);
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
