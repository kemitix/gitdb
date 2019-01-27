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

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

class GitDBTest implements WithAssertions {

    private final Supplier<String> stringSupplier = UUID.randomUUID()::toString;
    private final String userName = stringSupplier.get();
    private final String userEmailAddress = stringSupplier.get();

    // When initialising a repo in a dir that doesn't exist then a bare repo is created
    @Test
    void initRepo_whenDirNotExist_thenCreateBareRepo() throws IOException {
        //given
        final Path dir = dirDoesNotExist();
        //when
        final Result<GitDB> gitDB = GitDB.initLocal(dir, userName, userEmailAddress);
        //then
        assertThatResultIsOkay(gitDB);
        assertThatIsBareRepo(dir);
    }

    private Path dirDoesNotExist() throws IOException {
        final Path directory = Files.createTempDirectory("gitdb");
        Files.delete(directory);
        return directory;
    }

    private <T> void assertThatResultIsOkay(final Result<T> result) {
        assertThat(result.isOkay()).isTrue();
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
                failOnSuccess("Is a file not a directory"),
                error -> assertThat(error)
                        .isInstanceOf(NotDirectoryException.class)
                        .hasMessageContaining(dir.toString())
        );
    }

    private Path fileExists() throws IOException {
        return Files.createTempFile("gitdb", "file");
    }

    private <T> Consumer<T> failOnSuccess(String message) {
        return success -> fail(message);
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
                failOnSuccess("Directory is not empty"),
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
        assertThatResultIsOkay(gitDB);
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
                failOnSuccess("Not a bare repo"),
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
                failOnSuccess("Directory is a file"),
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
                failOnSuccess("Directory does not exist"),
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
                failOnSuccess("Not a bare repo"),
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
        final Result<Maybe<GitDBBranch>> branch = gitDb.flatMap(selectBranch("unknown"));
        //then
        assertThat(branch.orElseThrow().toOptional()).isEmpty();
    }

    private Result<GitDB> gitDB(final Path dbDir) {
        return GitDB.initLocal(dbDir, userName, userEmailAddress);
    }

    private Function<GitDB, Result<Maybe<GitDBBranch>>> selectBranch(final String branchName) {
        return db -> db.branch(branchName);
    }

    // Given a valid GitDbBranch handle

    // When select a valid branch then a GitDbBranch is returned
    @Test
    void selectBranch_branchExists_thenReturnBranch() throws Throwable {
        //given
        final Path dbDir = dirDoesNotExist();
        final Result<GitDB> gitDb = gitDB(dbDir);
        //when
        final Result<Maybe<GitDBBranch>> branch = gitDb.flatMap(selectBranch("master"));
        //then
        assertThat(branch.orElseThrow().toOptional()).as("Branch master exists").isNotEmpty();
    }

    // When getting a key that does not exist then return an empty Optional
    @Test
    void getKey_whenKeyNotExist_thenReturnEmptyOptional() {
        //given
        final GitDBBranch branch = gitDBBranch();
        //when
        final Result<Maybe<String>> value = branch.get("unknown");
        //then
        value.match(
                success -> assertThat(success.toOptional()).isEmpty(),
                failOnError()
        );
    }

    private GitDBBranch gitDBBranch() {
        try {
            return gitDB(dirDoesNotExist())
                    .flatMap(selectBranch("master"))
                    .orElseThrow().orElse(null);
        } catch (Throwable throwable) {
            throw new RuntimeException("Couldn't create master branch", throwable);
        }
    }

    private Consumer<Throwable> failOnError() {
        return error -> fail("Not an error");
    }

    // When getting the format version it matches expected
    @Test
    void getVersionFormat_thenFormatIsSet() {
        //given
        final GitDBBranch gitDBBranch = gitDBBranch();
        final Version version = new FormatVersion().getVersion();
        //when
        final Result<Maybe<Version>> formatVersion = gitDBBranch.getFormatVersion();
        //then
        formatVersion.match(
                success -> success.peek(v -> assertThat(v).isEqualTo(version).isNotSameAs(version)),
                failOnError()
        );
    }

    // When putting a key/value pair then a GitDbBranch is returned
    @Test
    void putValue_thenReturnUpdatedGitDBBranch() {
        //given
        final GitDBBranch originalBranch = gitDBBranch();
        //when
        final Result<GitDBBranch> updatedBranch = originalBranch.put("key-name", "value");
        //then
        updatedBranch.match(
                success -> assertThat(success).isNotNull().isNotSameAs(originalBranch),
                failOnError()
        );
    }

    // When getting a key that does exist then the value is returned inside an Optional
    @Test
    void getKey_whenExists_thenReturnValueInOptional() {
        //given
        final String key = stringSupplier.get();
        final String value = stringSupplier.get();
        final GitDBBranch originalBranch = gitDBBranch();
        final Result<GitDBBranch> updatedBranch = originalBranch.put(key, value);
        //when
        final Result<Maybe<String>> result = updatedBranch.flatMap(b -> b.get(key));
        //then
        result.match(
                success -> success.map(v -> assertThat(v).contains(value)),
                failOnError()
        );
    }

    // When removing a key that does not exist then the GitDbBranch is returned
    @Test
    void removeKey_whenNotExist_thenReturnOriginal() {
        //given
        final GitDBBranch gitDBBranch = gitDBBranch();
        //when
        final Result<GitDBBranch> result = gitDBBranch.remove("unknown");
        //then
        result.match(
                success -> assertThat(success).isSameAs(gitDBBranch),
                failOnError()
        );
    }

    // When removing a key that does exist then a GitDbBranch is returned
    @Test
    void removeKey_whenExists_thenReturnUpdatedBranch() {
        //given
        final String key = stringSupplier.get();
        final String value = stringSupplier.get();
        final Result<GitDBBranch> originalBranch = gitDBBranchWithKeyValue(key, value);
        //when
        final Result<GitDBBranch> updatedBranch = originalBranch.flatMap(b -> b.remove(key));
        //then
        updatedBranch.match(
                success -> assertThat(success).isNotSameAs(originalBranch),
                failOnError()
        );
    }

    private Result<GitDBBranch> gitDBBranchWithKeyValue(final String key, final String value) {
        return gitDBBranch().put(key, value);
    }

    // When removing a key that does exist then original GitDbBranch can still find it
    @Test
    void removeKey_whenExists_thenOriginalCanStillFind() {
        //given
        final String key = stringSupplier.get();
        final String value = stringSupplier.get();
        final Result<GitDBBranch> originalBranch = gitDBBranchWithKeyValue(key, value);
        //when
        final Result<GitDBBranch> updatedBranch = originalBranch.flatMap(b -> b.remove(key));
        //then
        originalBranch.flatMap(b -> b.get(key))
                .match(
                        success -> assertThat(success.toOptional()).contains(value),
                        failOnError()
                );
    }

    // When removing a key that does exist then the updated GitDbBranch can't find it
    @Test
    void removeKey_whenExists_thenUpdatedCanNotFind() {
        //given
        final String key = stringSupplier.get();
        final String value = stringSupplier.get();
        final Result<GitDBBranch> originalBranch = gitDBBranchWithKeyValue(key, value);
        //when
        final Result<GitDBBranch> updatedBranch = originalBranch.flatMap(b -> b.remove(key));
        //then
        updatedBranch.flatMap(b -> b.get(key))
                .match(
                        success -> assertThat(success.toOptional()).isEmpty(),
                        failOnError()
                );
    }

    @Test
    void selectBranch_branchExists_thenBranchName() throws Throwable {
        //given
        final Path dbDir = dirDoesNotExist();
        final Result<GitDB> gitDb = gitDB(dbDir);
        //when
        final Result<Maybe<String>> branchName = gitDb.flatMap(selectBranch("master"))
                .flatMap(branch -> Result.swap(branch.map(GitDBBranch::name)));
        //then
        assertThat(branchName.orElseThrow().toOptional()).as("Branch name is master")
                .contains("refs/heads/master");
    }

}
