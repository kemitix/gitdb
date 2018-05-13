package net.kemitix.gitdb;

import java.nio.file.Path;
import java.util.stream.Stream;

public interface GitDB {

    static GitDB local(Path dbDir) {
        return new GitDBLocal(dbDir);
    }

    void close();

    Path getGitDir();

    String get(Branch branch, Key key);

    <T> T get(Branch branch, Key key, Class<T> type);

    Stream<String> getFiles(Branch branch, Key key);

    <T> Stream<T> getFiles(Branch branch, Key key, Class<T> type);

    String save(Branch branch, Message message, Document<String> document, Author author);

    String delete(Branch branch, Key key, Message message, Author author);

    void tag(Reference reference);

    void createBranch(Reference reference);

    Stream<String> getAllBranches();

    Transaction createTransaction(Branch branch);
}
