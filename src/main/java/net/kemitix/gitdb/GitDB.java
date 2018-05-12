package net.kemitix.gitdb;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import java.nio.file.Path;
import java.util.stream.Stream;

public interface GitDB {

    static GitDB local(Path dbDir) throws GitAPIException {
        return new GitDBLocal(dbDir);
    }

    void close();

    Repository getRepository();

    Path getGitDir();

    String get(Branch branch, Key key);

    <T> T get(Branch branch, Key key, Class<T> type);

    Stream<String> getFiles(Branch branch, Key key);

    <T> Stream<T> getFiles(Branch branch, Key key, Class<T> type);

    <T> T save(Branch branch, Message message, Document<T> document, Author author);

    String delete(Branch branch, Key key, Message message, Author author);

    void tag(Reference reference);

    void createBranch(Reference reference);

    Stream<String> getAllBranches();

    Transaction createTransaction(Branch branch);
}
