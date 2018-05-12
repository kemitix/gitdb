package net.kemitix.gitdb;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;

class GitDBTest {

    private final Path dbDir = Files.createTempDirectory("gitdb");
    private final GitDB gitDB = GitDB.local(dbDir);

    GitDBTest() throws IOException, GitAPIException {
    }

    @Test
    void shouldInitialiseGitDB() {
        //then
        assertThat(gitDB).isNotNull();
        assertThat(gitDB.getGitDir()).isDirectory()
                .isEqualTo(dbDir);
        final Repository repository = gitDB.getRepository();
        assertThat(repository.isBare()).isTrue();
        assertThat(repository.getObjectDatabase().exists()).isTrue();
        assertThat(repository.getRefDatabase()).isNotNull();
    }



    @AfterEach
    void tearDown() throws IOException {
        gitDB.close();
        Files.walk(dbDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }
}
