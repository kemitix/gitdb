package net.kemitix.gitdb;

import lombok.SneakyThrows;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

class GitDBLocal implements GitDB {

    private final Repository repository;

    @SneakyThrows
    GitDBLocal(final Path gitDir) {
        this.repository = openRepo(gitDir)
                .orElseGet(() -> initRepo(gitDir));
    }

    @SneakyThrows
    private Repository initRepo(Path gitDir) {
        return Git.init()
                .setGitDir(gitDir.toFile())
                .setBare(true)
                .call()
                .getRepository();
    }

    private Optional<Repository> openRepo(final Path gitDir) throws IOException {
        final Repository build = new FileRepositoryBuilder()
                .setBare()
                .setMustExist(false)
                .setGitDir(gitDir.toFile())
                .setup()
                .build();
        if (build.getObjectDatabase().exists()) {
            return Optional.of(build);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void close() {
        repository.close();
    }

    @Override
    public String get(Branch branch, Key key) {
        return get(branch, key, String.class);
    }

    @Override
    public <T> T get(Branch branch, Key key, Class<T> type) {
        return null;
    }

    @Override
    public Stream<String> getFiles(Branch branch, Key key) {
        return getFiles(branch, key, String.class);
    }

    @Override
    public <T> Stream<T> getFiles(Branch branch, Key key, Class<T> type) {
        return null;
    }

    @Override
    public String save(Branch branch, Message message, Document<String> document, Author author) {
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
