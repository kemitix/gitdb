package net.kemitix.gitdb;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import java.nio.file.Path;

public interface GitDB {

    static GitDB local(Path dbDir) throws GitAPIException {
        return new GitDBLocal(dbDir);
    }

    void close();

    Repository getRepository();

    Path getGitDir();
}
