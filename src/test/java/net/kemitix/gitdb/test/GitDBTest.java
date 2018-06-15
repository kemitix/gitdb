package net.kemitix.gitdb.test;

import com.github.zafarkhaja.semver.Version;
import net.kemitix.gitdb.GitDB;
import net.kemitix.gitdb.GitDBBranch;
import net.kemitix.gitdb.GitDBTransaction;
import net.kemitix.gitdb.InvalidRepositoryException;
import org.assertj.core.api.WithAssertions;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.*;
import java.nio.file.*;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assumptions.assumeThat;

@ExtendWith(MockitoExtension.class)
class GitDBTest implements WithAssertions {

    private final Supplier<String> stringSupplier = UUID.randomUUID()::toString;
    private String userName = stringSupplier.get();
    private String userEmailAddress = stringSupplier.get();

    // When initialising a repo in a dir that doesn't exist then a bare repo is created
    @Test
    void initRepo_whenDirNotExist_thenCreateBareRepo() throws IOException {
        //given
        final Path dir = dirDoesNotExist();
        //when
        final GitDB gitDB = GitDB.initLocal(dir, userName, userEmailAddress);
        //then
        assertThat(gitDB).isNotNull();
        assertThatIsBareRepo(dir);
    }

    private void assertThatIsBareRepo(final Path dbDir) throws IOException {
        final Git git = Git.open(dbDir.toFile());
        final Repository repository = git.getRepository();
        assertThat(repository.findRef(Constants.HEAD)).isNotNull();
        assertThat(repository.isBare()).isTrue();
        assertThat(repository.getDirectory()).isEqualTo(dbDir.toFile());
    }

    private Path dirDoesNotExist() throws IOException {
        final Path directory = Files.createTempDirectory("gitdb");
        Files.delete(directory);
        return directory;
    }

    // When initialising a repo in a dir that is a file then an exception is thrown
    @Test
    void initRepo_whenDirIsFile_thenThrowException() throws IOException {
        //given
        final Path dir = fileExists();
        //then
        assertThatExceptionOfType(NotDirectoryException.class)
                .isThrownBy(() -> GitDB.initLocal(dir, userName, userEmailAddress))
                .withMessageContaining(dir.toString());
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
        //then
        assertThatExceptionOfType(DirectoryNotEmptyException.class)
                .isThrownBy(() -> GitDB.initLocal(dir, userName, userEmailAddress))
                .withMessageContaining(dir.toString());
    }

    private void filesExistIn(final Path dir) throws IOException {
        Files.createTempFile(dir, "gitdb", "file");
    }

    private Path dirExists() throws IOException {
        return Files.createTempDirectory("gitdb");
    }

    // When initialising a repo in a empty dir then a bare repo is created
    @Test
    void initRepo_whenEmptyDir_thenCreateBareRepo() throws IOException {
        //given
        final Path dir = dirExists();
        //when
        final GitDB gitDB = GitDB.initLocal(dir, userName, userEmailAddress);
        //then
        assertThat(gitDB).isNotNull();
        assertThatIsBareRepo(dir);
    }

    // When opening a repo in a dir that is not a bare repo then an exception is thrown
    @Test
    void openRepo_NotBareRepo_thenThrowException() throws IOException {
        //given
        final Path dir = dirExists();
        //then
        assertThatExceptionOfType(InvalidRepositoryException.class)
                .isThrownBy(() -> GitDB.openLocal(dir, userName, userEmailAddress))
                .withMessageContaining(dir.toString());
    }

    // When opening a repo in a dir that is a file then an exception is thrown
    @Test
    void openRepo_whenDirIsFile_thenThrowException() throws IOException {
        //given
        final Path dir = fileExists();
        //then
        assertThatExceptionOfType(InvalidRepositoryException.class)
                .isThrownBy(() -> GitDB.openLocal(dir, userName, userEmailAddress))
                .withMessageContaining(dir.toString());
    }

    // When opening a repo in a dir that doesn't exist then an exception is thrown
    @Test
    void openRepo_whenDirNotExist_thenThrowException() throws IOException {
        //given
        final Path dir = dirDoesNotExist();
        //then
        assertThatExceptionOfType(InvalidRepositoryException.class)
                .isThrownBy(() -> GitDB.openLocal(dir, userName, userEmailAddress))
                .withMessageContaining(dir.toString());
    }

    // When opening a repo in a dir that is not a bare repo then an exception is thrown
    @Test
    void openRepo_whenRepoNotBare_thenThrowException() throws IOException, GitAPIException {
        //given
        final Path dir = nonBareRepo();
        //then
        assertThatExceptionOfType(InvalidRepositoryException.class)
                .isThrownBy(() -> GitDB.openLocal(dir, userName, userEmailAddress))
                .withMessageContaining("Invalid GitDB repo")
                .withMessageContaining("Not a bare repo")
                .withMessageContaining(dir.toString());
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
        final GitDB gitDB = GitDB.openLocal(dir, userName, userEmailAddress);
        //then
        assertThat(gitDB).isNotNull();
    }

    private Path gitDBRepoPath() throws IOException {
        final Path dbDir = dirDoesNotExist();
        GitDB.initLocal(dbDir, userName, userEmailAddress);
        return dbDir;
    }

    // Given a valid GitDb handle

    private GitDB gitDB(final Path dbDir) throws IOException {
        return GitDB.initLocal(dbDir, userName, userEmailAddress);
    }

    // When select a branch that doesn't exist then an empty Optional is returned
    @Test
    void selectBranch_whenBranchNotExist_thenEmptyOptional() throws IOException {
        //given
        final GitDB gitDb = gitDB(dirDoesNotExist());
        //when
        final Optional<GitDBBranch> branch = gitDb.branch("unknown");
        //then
        assertThat(branch).isEmpty();
    }

    // When select a valid branch then a GitDbBranch is returned
    @Test
    void selectBranch_branchExists_thenReturnBranch() throws IOException {
        //given
        final Path dbDir = dirDoesNotExist();
        final GitDB gitDb = gitDB(dbDir);
        //when
        final Optional<GitDBBranch> branch = gitDb.branch("master");
        //then
        assertThat(branch).as("Branch master exists").isNotEmpty();
    }

    // Given a valid GitDbBranch handle

    private GitDBBranch gitDBBranch() throws IOException {
        final GitDB gitDB = gitDB(dirDoesNotExist());
        final Optional<GitDBBranch> branchOptional = gitDB.branch("master");
        assumeThat(branchOptional).isNotEmpty();
        return branchOptional.get();
    }

    // When getting a key that does not exist then return an empty Optional
    @Test
    void getKey_whenKeyNotExist_thenReturnEmptyOptional() throws IOException, ClassNotFoundException {
        //given
        final GitDBBranch branch = gitDBBranch();
        //when
        final Optional<String> value = branch.get("unknown");
        //then
        assertThat(value).isEmpty();
    }

    // When getting the format version it matches expected
    @Test
    void getVersionFormat_thenFormatIsSet() throws IOException {
        //given
        final GitDBBranch gitDBBranch = gitDBBranch();
        //when
        final Optional<Version> formatVersion = gitDBBranch.getFormatVersion();
        //then
        assertThat(formatVersion).contains(GitDB.VERSION);
        assertThat(formatVersion.get()).isNotSameAs(GitDB.VERSION);
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
    void getValue_whenExists_thenReturnValueInOptional() throws IOException, ClassNotFoundException {
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

    // When starting a named transaction then GitDbTransaction is returned
    @Test
    void startTransaction_thenReturnGitDBTransaction() throws IOException {
        //given
        final String key = stringSupplier.get();
        final String value = stringSupplier.get();
        final GitDBBranch gitDBBranch = gitDBBranchWithKeyValue(key, value);
        final String name = stringSupplier.get();
        //when
        final GitDBTransaction transaction = gitDBBranch.transaction(name);
        //then
        assertThat(transaction).isNotNull();
        assertThat(transaction.name()).isEqualTo(name);
    }

    // When starting an anonymous transaction then a GitDbTransaction is returned
    @Test
    void startAnonymousTransaction_thenReturnGitDBTransaction() throws IOException {
        //given
        final GitDBBranch gitDBBranch = gitDBBranch();
        //when
        final GitDBTransaction transaction = gitDBBranch.transaction();
        //then
        assertThat(transaction).isNotNull();
        assertThat(transaction.name()).isNotNull();
    }

    // When starting an anonymous transaction then original branch is unchanged
    @Test
    void startAnonymousTransaction_thenOriginalBranchUnchanged() throws IOException {
        //given
        final GitDB gitDB = gitDB(dirDoesNotExist());
        final GitDBBranch gitDBBranch = gitDB.branch("master").get();
        final String commitId = gitDBBranch.getCommitId();
        //when
        final GitDBTransaction transaction = gitDBBranch.transaction();
        //then
        assertThat(gitDBBranch.getCommitId()).isEqualTo(commitId);
        assertThat(gitDB.branch("master").map(GitDBBranch::getCommitId)).contains(commitId);
    }

    // When starting an anonymous transaction then transaction starts on the same commit
    @Test
    void startAnonymousTransaction_thenTransactionCommitMatchesOriginal() throws IOException {
        //given
        final GitDBBranch gitDBBranch = gitDBBranch();
        final String commitId = gitDBBranch.getCommitId();
        //when
        final GitDBTransaction transaction = gitDBBranch.transaction();
        //then
        assertThat(transaction.getCommitId()).isEqualTo(commitId);
    }

    // Given a GitDbTransaction handle (i.e. a new branch)
    // When putting a new key/value pair then the original GitDbBranch can't find it
    @Test
    void putValue_whenTransaction_thenOriginalBranchNotFind() throws IOException {
        //given
        final GitDBBranch originalBranch = gitDBBranch();
        final GitDBTransaction transaction = originalBranch.transaction();
        final String key = stringSupplier.get();
        final String value = stringSupplier.get();
        //when
        final GitDBBranch updatedBranch = transaction.put(key, value);
        //then
        assertThat(originalBranch.get(key)).isEmpty();
    }

    // When putting an existing key/value pair then the original GitDbBranch finds the original value
    @Test
    void putValue_whenTransaction_thenUpdatedBranchFinds() throws IOException {
        //given
        final GitDBBranch originalBranch = gitDBBranch();
        final GitDBTransaction transaction = originalBranch.transaction();
        final String key = stringSupplier.get();
        final String value = stringSupplier.get();
        //when
        final GitDBBranch updatedBranch = transaction.put(key, value);
        //then
        assertThat(updatedBranch.get(key)).contains(value);
    }

    // When removing a key in a transaction then the original GitDbBRanch still finds it
    @Test
    void removeKey_whenTransaction_thenOriginalBranchStillFinds() throws IOException {
        //given
        final String key = stringSupplier.get();
        final String value = stringSupplier.get();
        final GitDBBranch originalBranch = gitDBBranchWithKeyValue(key, value);
        final GitDBTransaction transaction = originalBranch.transaction();
        //when
        final GitDBBranch removed = transaction.remove(key);
        //then
        assertThat(originalBranch.get(key)).contains(value);
    }

    // When removing a key in a transaction then the transaction branch no longer finds it
    @Test
    void removeKey_whenTransaction_thenTransactionNotFind() throws IOException {
        //given
        final String key = stringSupplier.get();
        final String value = stringSupplier.get();
        final GitDBBranch originalBranch = gitDBBranchWithKeyValue(key, value);
        final GitDBTransaction transaction = originalBranch.transaction();
        //when
        final GitDBBranch removed = transaction.remove(key);
        //then
        assertThat(removed.get(key)).isEmpty();
    }

    // Given a GitDbTransaction handle with a added, updated and removed keys
    // When closing the transaction an GitDbBranch is returned
    // When closing the transaction the added key/value is found
    // When closing the transaction the updated value is found
    // When closing the transaction the removed key is not found

    private static void tree(final Path dbDir, final PrintStream out) throws IOException {
        final Process treeProcess = new ProcessBuilder("tree", dbDir.toString()).start();
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(treeProcess.getInputStream()))) {
            String line;
            while (null != (line = reader.readLine())) {
                out.println("line = " + line);
            }
        }
    }

}
