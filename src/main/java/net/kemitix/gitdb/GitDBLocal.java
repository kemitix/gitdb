package net.kemitix.gitdb;

import lombok.Getter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import java.nio.file.Path;
import java.util.stream.Stream;

class GitDBLocal implements GitDB {

    @Getter
    private final Repository repository;

    @Getter
    private final Path gitDir;

    GitDBLocal(final Path gitDir) throws GitAPIException {
        this.gitDir = gitDir;
        this.repository = Git
                .init()
                .setBare(true)
                .setGitDir(gitDir.toFile())
                .call()
                .getRepository();
    }

    @Override
    public void close() {
        repository.close();
    }

    @Override
    public String get(Branch branch, Key key) {
        return null;
    }

    @Override
    public <T> T get(Branch branch, Key key, Class<T> type) {
        return null;
    }

    @Override
    public Stream<String> getFiles(Branch branch, Key key) {
        return null;
    }

    @Override
    public <T> Stream<T> getFiles(Branch branch, Key key, Class<T> type) {
        return null;
    }

    @Override
    public <T> T save(Branch branch, Message message, Document<T> document, Author author) {
        return document.getValue();
    }

    @Override
    public String delete(Branch branch, Key key, Message message, Author author) {
        return null;
    }

    @Override
    public void tag(Reference reference) {

    }

    @Override
    public void createBranch(Reference reference) {

    }

    @Override
    public Stream<String> getAllBranches() {
        return null;
    }

    @Override
    public Transaction createTransaction(Branch branch) {
        return null;
    }

}
