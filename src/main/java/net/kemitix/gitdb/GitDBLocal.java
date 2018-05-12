package net.kemitix.gitdb;

import lombok.Getter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import java.nio.file.Path;

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
}
