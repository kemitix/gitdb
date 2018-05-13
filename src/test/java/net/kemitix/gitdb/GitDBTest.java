package net.kemitix.gitdb;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GitDBTest {

    private final Path dbDir = Files.createTempDirectory("gitdb");
    private final GitDB gitDB = GitDB.local(dbDir);
    private final Branch master = Branch.name("master");
    private final Message message = Message.message(UUID.randomUUID().toString());
    private final Key key = Key.name(UUID.randomUUID().toString());
    private final Author author = Author.name("junit", "gitdb@kemitix.net");

    GitDBTest() throws IOException {
    }

    @Test
    void shouldInitialiseGitDB() throws IOException {
        //then
        assertThat(gitDB).isNotNull();
        assertThat(Files.isDirectory(dbDir)).isTrue();
        assertThat(Files.newDirectoryStream(dbDir).iterator())
                .contains(
                        dbDir.resolve("branches"),
                        dbDir.resolve("HEAD"),
                        dbDir.resolve("config"),
                        dbDir.resolve("refs"),
                        dbDir.resolve("logs"),
                        dbDir.resolve("hooks"),
                        dbDir.resolve("objects")
                );
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
