package net.kemitix.gitdb;

import org.assertj.core.api.WithAssertions;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GitDBTest implements WithAssertions {

//    private final Branch master = Branch.name("master");
//    private final Message message = Message.message(UUID.randomUUID().toString());
//    private final Key key = Key.name(UUID.randomUUID().toString());
//    private final Author author = Author.name("junit", "gitdb@kemitix.net");

    // When initialising a repo in a dir that doesn't exist then a bare repo is created
    @Test
    void initRepo_whenDirNotExist_thenCreateBareRepo() throws IOException {
        //given
        final Path dir = dirDoesNotExist();
        //when
        final GitDB gitDB = GitDB.initLocal(dir);
        //then
        assertThat(gitDB).isNotNull();
        assertThatIsBareRepo(dir);
    }

    private void assertThatIsBareRepo(final Path dbDir) throws IOException {
        final DirectoryStream<Path> paths = Files.newDirectoryStream(dbDir);
        assertThat(paths.iterator())
                .contains(
                        dbDir.resolve("branches"),
                        dbDir.resolve("HEAD"),
                        dbDir.resolve("config"),
                        dbDir.resolve("refs"),
                        dbDir.resolve("logs"),
                        dbDir.resolve("hooks"),
                        dbDir.resolve("objects")
                );
        final List<String> config = Files.readAllLines(dbDir.resolve("config"));
        assertThat(config).contains("\tbare = true");
    }

    private Path dirDoesNotExist() throws IOException {
        final Path directory = Files.createTempDirectory("gitdb");
        Files.delete(directory);
        return directory;
    }

    // When initialising a repo and an unexpected error occurs then an exception is thrown
    @Test
    void initRepo_whenUnexpectedError_thenThrowException() throws IOException, GitAPIException {
        //given
        final Path dbDir = dirDoesNotExist();
        final InitCommand initCommand = spy(InitCommand.class);
        given(initCommand.call()).willThrow(mock(GitAPIException.class));
        //then
        assertThatExceptionOfType(UnexpectedGitDbException.class)
                .isThrownBy(() -> new GitDBLocal(initCommand, dbDir.toFile()))
                .withMessageContaining("Unhandled Git API Exception");
    }

    // When initialising a repo in a dir that is a file then an exception is thrown
    @Test
    void initRepo_whenDirIsFile_thenThrowException() throws IOException {
        //given
        final Path dir = fileExists();
        //then
        assertThatExceptionOfType(NotDirectoryException.class)
                .isThrownBy(() -> GitDB.initLocal(dir))
                .withMessageContaining(dir.toString());
    }

    private Path fileExists() throws IOException {
        return Files.createTempFile("gitdb", "file");
    }

    // When initialising a repo in a dir is exists then an exception is thrown
    //@Test
    //void initRepo_whenDirExists_thenThrowException() {
    //}

    // When opening a repo in a dir that is not a bare repo then an exception is thrown
    // When opening a repo in a dir that is a file then an exception is thrown
    // When opening a repo in a dir that doesn't exist then an exception is thrown

    // When opening a repo in a dir that is a bare repo then GitDb is returned

    // Given a valid GitDb handle
    // When select a branch that doesn't exist then an exception is thrown
    // When select a valid branch then a GitDbBranch is returned

    // Given a valid GitDbBranch handle
    // When getting a key that does not exist then return an empty Optional
    // When putting a key/value pair then a GitDbBranch is returned
    // When getting a key that does exist then the value is returned inside an Optional
    // When removing a key that does not exist then the GitDbBranch is returned
    // When removing a key that does exist then a GitDbBranch is returned
    // When starting a named transaction then GitDbTransaction is returned
    // When starting an anonymous transaction then a GitDbTransaction is returned

    // Given a GitDbTransaction handle
    // When putting a new key/value pair then the GitDbBranch can't find it
    // When putting an existing key/value pair then the GitDbBranch finds the original value
    // When removing a key from then the GitDbBRanch still finds it

    // Given a GitDbTransaction handle with a added, updated and removed keys
    // When closing the transaction an GitDbBranch is returned
    // When closing the transaction the added key/value is found
    // When closing the transaction the updated value is found
    // When closing the transaction the removed key is not found


//    @Test
//    void saveFirstDocumentCreatesNewMasterBranch() {
//        //given
//        final String value = "{\"key\":\"value\"}";
//        final Document<String> document = Document.create(key, value);
//        final JsonObject expected = Json.createObjectBuilder()
//                .add("key", "value")
//                .build();
//        //when
//        final Key savedKey = gitDB.branch(master)
//                .put(message, document, author);
//        //then
//        final String retrievedObject = gitDB.branch(master).get(savedKey);
//
//    }

//    @Test
//    void getGivesSavedValue() {
//        //given
//        final String value = UUID.randomUUID().toString();
//        final Document<String> document = Document.create(key, value);
//        gitDB.save(master, message, document, author);
//        //when
//        final String result = gitDB.get(master, key);
//        //then
//        assertThat(result).isEqualTo(value);
//    }

//    @AfterEach
//    void tearDown() throws IOException {
//        gitDB.close();
//        //Files.walk(dbDir)
//        //        .sorted(Comparator.reverseOrder())
//        //        .map(Path::toFile)
//        //        .forEach(File::delete);
//    }
}
