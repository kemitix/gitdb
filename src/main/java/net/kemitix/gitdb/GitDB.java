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
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.nio.file.Path;

public interface GitDB
//        extends Closeable
{

    /**
     * Initialise a new local gitdb.
     *
     * @param dbDir the path to initialise the local repo in
     * @return a GitDB instance for the created local gitdb
     */
    static GitDB initLocal(final Path dbDir) throws GitAPIException {
        final InitCommand initCommand = Git.init().setGitDir(dbDir.toFile()).setBare(true);
        final Git git = initCommand.call();
        return new GitDBLocal(git);
    }

//    /**
//     * Open an existing local gitdb.
//     *
//     * @param dbDir the path to the local repo
//     * @return a GitDB instance for the local gitdb
//     */
//    static GitDB local(final Path dbDir) throws IOException {
//        return new GitDBLocal(Git.open(dbDir.toFile()));
//    }

//    /**
//     * Select a branch.
//     *
//     * @param branch the branch to select
//     * @return a branch within the gitdb
//     */
//    GitDbBranch branch(Branch branch);

//    interface GitDbBranch {

//        String get(Key key);
//
//        <T> T get(Key key, Class<T> type);
//
//        Key put(Message message, Document<String> document, Author author);
//
//        GitDbBranch delete(Branch branch, Key key, Message message, Author author);
//
//        GitDbBranch tag(Reference reference);
//
//        Transaction startTransaction(Branch branch);
//
//        GitDbBranch fork(Branch branch);

//    }

//    Stream<Branch> allBranches();

}
