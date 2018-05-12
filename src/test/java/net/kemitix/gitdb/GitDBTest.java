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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GitDBTest {

    private final Path dbDir = Files.createTempDirectory("gitdb");
    private final GitDB gitDB = GitDB.local(dbDir);
    private final Branch master = Branch.name("master");
    private final Message message = Message.message(UUID.randomUUID().toString());
    private final Key key = Key.name(UUID.randomUUID().toString());
    private final Author author = Author.name("junit");

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

    @Test
    void saveDocumentGivesSavedValue() {
        //given
        final String value = UUID.randomUUID().toString();
        final Document<String> document = Document.create(key, value);
        //when
        final String result = gitDB.save(master, message, document, author);
        //then
        assertThat(result).isEqualTo(value);
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
