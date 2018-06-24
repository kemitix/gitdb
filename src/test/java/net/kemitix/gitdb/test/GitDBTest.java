package net.kemitix.gitdb.test;

import com.github.zafarkhaja.semver.Version;
import net.kemitix.gitdb.FormatVersion;
import net.kemitix.gitdb.GitDB;
import net.kemitix.gitdb.GitDBBranch;
import net.kemitix.gitdb.InvalidRepositoryException;
import net.kemitix.mon.maybe.Maybe;
import net.kemitix.mon.result.Result;
import org.assertj.core.api.WithAssertions;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@ExtendWith(MockitoExtension.class)
class GitDBTest implements WithAssertions {

    private final Supplier<String> stringSupplier = UUID.randomUUID()::toString;
    private final String userName = stringSupplier.get();
    private final String userEmailAddress = stringSupplier.get();

    private static void tree(final Path dbDir, final PrintStream out) throws IOException {
        final Process treeProcess = new ProcessBuilder("tree", dbDir.toString()).start();
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(treeProcess.getInputStream()))) {
            String line;
            while (null != (line = reader.readLine())) {
                out.println("line = " + line);
            }
        }
    }

    // When initialising a repo in a dir that doesn't exist then a bare repo is created
    @Test
    void initRepo_whenDirNotExist_thenCreateBareRepo() throws IOException {
        //given
        final Path dir = dirDoesNotExist();
        //when
        final Result<GitDB> gitDB = GitDB.initLocal(dir, userName, userEmailAddress);
        //then
        assertThat(gitDB.isOkay()).isTrue();
        assertThatIsBareRepo(dir);
    }

    private Path dirDoesNotExist() throws IOException {
        final Path directory = Files.createTempDirectory("gitdb");
        Files.delete(directory);
        return directory;
    }

    private void assertThatIsBareRepo(final Path dbDir) throws IOException {
        final Git git = Git.open(dbDir.toFile());
        final Repository repository = git.getRepository();
        assertThat(repository.findRef(Constants.HEAD)).isNotNull();
        assertThat(repository.isBare()).isTrue();
        assertThat(repository.getDirectory()).isEqualTo(dbDir.toFile());
    }

    // When initialising a repo in a dir that is a file then an exception is thrown
    @Test
    void initRepo_whenDirIsFile_thenThrowException() throws IOException {
        //given
        final Path dir = fileExists();
        //when
        final Result<GitDB> gitDBResult = GitDB.initLocal(dir, userName, userEmailAddress);
        //then
        gitDBResult.match(
                success -> fail("Is a file not a directory"),
                error -> assertThat(error)
                        .isInstanceOf(NotDirectoryException.class)
                        .hasMessageContaining(dir.toString())
        );
    }

    private Path fileExists() throws IOException {
        return Files.createTempFile("gitdb", "file");
    }

    // When initialising a repo in a non-empty dir then an exception is thrown
    @Test
    void initRepo_whenNotEmptyDir_thenThrowException() throws IOException {
        //given
        final Path dir = dirExists();
        filesExistIn(dir);
        //when
        final Result<GitDB> gitDBResult = GitDB.initLocal(dir, userName, userEmailAddress);
        //then
        gitDBResult.match(
                success -> fail("Directory is not empty"),
                error -> assertThat(error)
                        .isInstanceOf(DirectoryNotEmptyException.class)
                        .hasMessageContaining(dir.toString())
        );
    }

    private Path dirExists() throws IOException {
        return Files.createTempDirectory("gitdb");
    }

    private void filesExistIn(final Path dir) throws IOException {
        Files.createTempFile(dir, "gitdb", "file");
    }

    // When initialising a repo in a empty dir then a bare repo is created
    @Test
    void initRepo_whenEmptyDir_thenCreateBareRepo() throws IOException {
        //given
        final Path dir = dirExists();
        //when
        final Result<GitDB> gitDB = GitDB.initLocal(dir, userName, userEmailAddress);
        //then
        assertThat(gitDB.isOkay()).isTrue();
        assertThatIsBareRepo(dir);
    }

    // When opening a repo in a dir that is not a bare repo then an exception is thrown
    @Test
    void openRepo_NotBareRepo_thenThrowException() throws IOException {
        //given
        final Path dir = dirExists();
        //when
        final Result<GitDB> gitDBResult = GitDB.openLocal(dir, userName, userEmailAddress);
        //then
        gitDBResult.match(
                success -> fail("Not a bare repo"),
                error -> assertThat(error)
                        .isInstanceOf(InvalidRepositoryException.class)
                        .hasMessageContaining(dir.toString())
        );

    }

    // When opening a repo in a dir that is a file then an exception is thrown
    @Test
    void openRepo_whenDirIsFile_thenThrowException() throws IOException {
        //given
        final Path dir = fileExists();
        //when
        final Result<GitDB> gitDBResult = GitDB.openLocal(dir, userName, userEmailAddress);
        //then
        gitDBResult.match(
                success -> fail("Directory is a file"),
                error -> assertThat(error)
                        .isInstanceOf(InvalidRepositoryException.class)
                        .hasMessageContaining(dir.toString())
        );
    }

    // When opening a repo in a dir that doesn't exist then an exception is thrown
    @Test
    void openRepo_whenDirNotExist_thenThrowException() throws IOException {
        //given
        final Path dir = dirDoesNotExist();
        //when
        final Result<GitDB> gitDBResult = GitDB.openLocal(dir, userName, userEmailAddress);
        //then
        gitDBResult.match(
                success -> fail("Directory does not exist"),
                error -> assertThat(error)
                        .isInstanceOf(InvalidRepositoryException.class)
                        .hasMessageContaining(dir.toString())
        );
    }

    // When opening a repo in a dir that is not a bare repo then an exception is thrown
    @Test
    void openRepo_whenRepoNotBare_thenThrowException() throws IOException, GitAPIException {
        //given
        final Path dir = nonBareRepo();
        //when
        final Result<GitDB> gitDBResult = GitDB.openLocal(dir, userName, userEmailAddress);
        //then
        gitDBResult.match(
                success -> fail("Not a bare repo"),
                error -> assertThat(error)
                        .isInstanceOf(InvalidRepositoryException.class)
                        .hasMessageContaining("Invalid GitDB repo")
                        .hasMessageContaining("Not a bare repo")
                        .hasMessageContaining(dir.toString())
        );
    }

    private Path nonBareRepo() throws IOException, GitAPIException {
        final Path dbDir = dirDoesNotExist();
        Git.init().setGitDir(dbDir.toFile()).setBare(false).call();
        return dbDir;
    }

    // When opening a repo in a dir that is a bare repo then GitDb is returned
    @Test
    void openRepo_whenGitDB_thenReturnGitDB() throws IOException {
        //given
        final Path dir = gitDBRepoPath();
        //when
        final Result<GitDB> gitDB = GitDB.openLocal(dir, userName, userEmailAddress);
        //then
        assertThat(gitDB.isOkay()).isTrue();
        gitDB.match(
                success -> assertThat(success).isNotNull(),
                error -> fail("did not open local repo")
        );
    }

    private Path gitDBRepoPath() throws IOException {
        final Path dbDir = dirDoesNotExist();
        GitDB.initLocal(dbDir, userName, userEmailAddress);
        return dbDir;
    }

    // Given a valid GitDb handle

    // When select a branch that doesn't exist then an empty Optional is returned
    @Test
    void selectBranch_whenBranchNotExist_thenEmptyOptional() throws Throwable {
        //given
        final Result<GitDB> gitDb = gitDB(dirDoesNotExist());
        //when
        final Result<Maybe<GitDBBranch>> branch = gitDb.flatMap(db -> db.branch("unknown"));
        //then
        assertThat(branch.orElseThrow().toOptional()).isEmpty();
    }

    private Result<GitDB> gitDB(final Path dbDir) {
        return GitDB.initLocal(dbDir, userName, userEmailAddress);
    }

    // When select a valid branch then a GitDbBranch is returned
    @Test
    void selectBranch_branchExists_thenReturnBranch() throws Throwable {
        //given
        final Path dbDir = dirDoesNotExist();
        final Result<GitDB> gitDb = gitDB(dbDir);
        //when
        final Result<Maybe<GitDBBranch>> branch = gitDb.flatMap(db -> db.branch("master"));
        //then
        assertThat(branch.orElseThrow().toOptional()).as("Branch master exists").isNotEmpty();
    }

    // Given a valid GitDbBranch handle

    // When getting a key that does not exist then return an empty Optional
    @Test
    void getKey_whenKeyNotExist_thenReturnEmptyOptional() throws IOException {
        //given
        final GitDBBranch branch = gitDBBranch();
        //when
        final Optional<String> value = branch.get("unknown");
        //then
        assertThat(value).isEmpty();
    }

    private GitDBBranch gitDBBranch() {
        try {
            return gitDB(dirDoesNotExist())
                    .flatMap(db -> db.branch("master"))
                    .orElseThrow()
                    .orElse(null);
        } catch (Throwable throwable) {
            throw new RuntimeException("Couldn't create master branch");
        }
    }

    // When getting the format version it matches expected
    @Test
    void getVersionFormat_thenFormatIsSet() throws IOException {
        //given
        final GitDBBranch gitDBBranch = gitDBBranch();
        //when
        final Optional<Version> formatVersion = gitDBBranch.getFormatVersion();
        //then
        final Version version = new FormatVersion().getVersion();
        assertThat(formatVersion).contains(version);
        assertThat(formatVersion.get()).isNotSameAs(version);
    }

    // When putting a key/value pair then a GitDbBranch is returned
    @Test
    void putValue_thenReturnUpdatedGitDBBranch() throws IOException {
        //given
        final GitDBBranch originalBranch = gitDBBranch();
        //when
        final GitDBBranch updatedBranch = originalBranch.put("key-name", "value");
        //then
        assertThat(updatedBranch).isNotNull();
        assertThat(updatedBranch).isNotSameAs(originalBranch);
    }

    // When getting a key that does exist then the value is returned inside an Optional
    @Test
    void getKey_whenExists_thenReturnValueInOptional() throws IOException {
        //given
        final String key = stringSupplier.get();
        final String value = stringSupplier.get();
        final GitDBBranch originalBranch = gitDBBranch();
        final GitDBBranch updatedBranch = originalBranch.put(key, value);
        //when
        final Optional<String> result = updatedBranch.get(key);
        //then
        assertThat(result).contains(value);
    }

    // When removing a key that does not exist then the GitDbBranch is returned
    @Test
    void removeKey_whenNotExist_thenReturnOriginal() throws IOException {
        //given
        final GitDBBranch gitDBBranch = gitDBBranch();
        //when
        final GitDBBranch result = gitDBBranch.remove("unknown");
        //then
        assertThat(result).isSameAs(gitDBBranch);
    }

    // When removing a key that does exist then a GitDbBranch is returned
    @Test
    void removeKey_whenExists_thenReturnUpdatedBranch() throws IOException {
        //given
        final String key = stringSupplier.get();
        final String value = stringSupplier.get();
        final GitDBBranch originalBranch = gitDBBranchWithKeyValue(key, value);
        //when
        final GitDBBranch updatedBranch = originalBranch.remove(key);
        //then
        assertThat(updatedBranch).isNotSameAs(originalBranch);
    }

    private GitDBBranch gitDBBranchWithKeyValue(final String key, final String value) throws IOException {
        return gitDBBranch().put(key, value);
    }

    // When removing a key that does exist then original GitDbBranch can still find it
    @Test
    void removeKey_whenExists_thenOriginalCanStillFind() throws IOException {
        //given
        final String key = stringSupplier.get();
        final String value = stringSupplier.get();
        final GitDBBranch originalBranch = gitDBBranchWithKeyValue(key, value);
        //when
        final GitDBBranch updatedBranch = originalBranch.remove(key);
        //then
        assertThat(originalBranch.get(key)).contains(value);
    }

    // When removing a key that does exist then the updated GitDbBranch can't find it
    @Test
    void removeKey_whenExists_thenUpdatedCanNotFind() throws IOException {
        //given
        final String key = stringSupplier.get();
        final String value = stringSupplier.get();
        final GitDBBranch originalBranch = gitDBBranchWithKeyValue(key, value);
        //when
        final GitDBBranch updatedBranch = originalBranch.remove(key);
        //then
        assertThat(updatedBranch.get(key)).isEmpty();
    }

}
