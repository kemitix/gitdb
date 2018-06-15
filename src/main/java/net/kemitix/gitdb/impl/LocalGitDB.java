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

package net.kemitix.gitdb.impl;

import net.kemitix.gitdb.GitDB;

import java.io.IOException;
import java.nio.file.Path;

/**
 * API for connecting to a Local Git repo as a database.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
public interface LocalGitDB extends GitDB {

    /**
     * Create a new GitDB instance, while initialising a new git repo.
     *
     * @param dbDir            the path to instantiate the git repo in
     * @param userName         the user name
     * @param userEmailAddress the user email address
     * @return a GitDB instance for the created local gitdb
     * @throws IOException if there {@code dbDir} is a file or a non-empty directory
     */
    static GitDB init(final Path dbDir, final String userName, final String userEmailAddress) throws IOException {
        return LocalGitDBImpl.init(dbDir, userName, userEmailAddress);
    }

    /**
     * Create a new GitDB instance using the Git repo.
     *
     * @param dbDir            the path to instantiate the git repo in
     * @param userName         the user name
     * @param userEmailAddress the user email address
     * @return a GitDB instance for the created local gitdb
     */
    static GitDB open(final Path dbDir, final String userName, final String userEmailAddress) {
        return LocalGitDBImpl.open(dbDir, userName, userEmailAddress);
    }

}
