package net.kemitix.gitdb;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

public interface GitDB extends Closeable {

    /**
     * Open an existing local gitdb.
     *
     * @param dbDir the path to the local repo
     * @return a GitDB instance for the local gitdb
     */
    static GitDB local(final Path dbDir) throws IOException {
        return new GitDBLocal(Git.open(dbDir.toFile()));
    }

    /**
     * Initialise a new local gitdb.
     *
     * @param dbDir the path to initialise the local repo in
     * @return a GitDB instance for the created local gitdb
     */
    static GitDB initLocal(final Path dbDir) throws GitAPIException {
        return new GitDBLocal(Git.init()
                .setGitDir(dbDir.toFile())
                .setBare(true)
                .call());
    }

    /**
     * Select a branch.
     *
     * @param branch the branch to select
     * @return a branch within the gitdb
     */
    GitDbBranch branch(Branch branch);

    interface GitDbBranch {

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

    }

    Stream<Branch> allBranches();

}
