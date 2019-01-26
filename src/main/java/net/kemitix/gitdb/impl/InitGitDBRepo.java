/*
  The MIT License (MIT)

  Copyright (c) 2018 Paul Campbell

  Permission is hereby granted, free of charge, to any person obtaining a copy of this software
  and associated documentation files (the "Software"), to deal in the Software without restriction,
  including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
  and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
  subject to the following conditions:

  The above copyright notice and this permission notice shall be included in all copies
  or substantial portions of the Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
  INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
  AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
  DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.kemitix.gitdb.impl;

import net.kemitix.gitdb.FormatVersion;
import net.kemitix.mon.result.Result;
import net.kemitix.mon.result.WithResultContinuation;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.util.FS;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Optional;
import java.util.concurrent.Callable;

import static net.kemitix.conditional.Condition.where;

/**
 * Initialise a new GitDB Repo.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
final class InitGitDBRepo {

    private static final String INIT_MESSAGE = "Initialise GitDB v1";
    private static final String INIT_USER = "GitDB";
    private static final String INIT_EMAIL = "pcampbell@kemitix.net";
    private static final String MASTER = "master";
    private static final String GIT_DB_VERSION = "GitDB.Version";
    private static final String REFS_HEADS_FORMAT = "refs/heads/%s";

    private InitGitDBRepo() {
        throw new UnsupportedOperationException();
    }

    /**
     * Initialise a new GitDB repo.
     *
     * @param dbDir the directory to initialise the repo in
     * @return a Result containing the created Repository
     */
    static Result<Repository> create(final Path dbDir) {
        return validDbDir(dbDir.toFile())
                .peek(File::mkdirs)
                .map(InitGitDBRepo::exactDirectory)
                .andThen(InitGitDBRepo::openRepository)
                .thenWith(InitGitDBRepo::createRepoDirectory)
                .thenWith(InitGitDBRepo::createInitialMasterBranch);
    }

    private static Result<File> validDbDir(final File dbDir) {
        return Result.ok(dbDir)
                .flatMap(InitGitDBRepo::isNotAFile)
                .flatMap(InitGitDBRepo::ifExistsThenIsEmpty);
    }

    private static Result<File> isNotAFile(final File dbDir) {
        return Result.ok(dbDir)
                .thenWith(dir -> () -> where(dir.isFile()).thenThrow(new NotDirectoryException(dbDir.toString())));
    }

    private static Result<File> ifExistsThenIsEmpty(final File dbDir) {
        return Result.ok(dbDir)
                .thenWith(dir -> () ->
                        where(dir.exists())
                                .and(() -> Optional.ofNullable(dir.listFiles()).orElse(new File[0]).length != 0)
                                .thenThrow(new DirectoryNotEmptyException(dir.toString())));
    }

    private static RepositoryCache.FileKey exactDirectory(final File dir) {
        return RepositoryCache.FileKey.exact(dir, FS.DETECTED);
    }

    private static Callable<Repository> openRepository(final RepositoryCache.FileKey fileKey) {
        return () -> fileKey.open(false);
    }

    private static WithResultContinuation<Repository> createRepoDirectory(final Repository repository) {
        return () -> repository.create(true);
    }

    private static WithResultContinuation<Repository> createInitialMasterBranch(
            final Repository repository
    ) {
        return () -> {
            final GitDBRepo repo = new GitDBRepo(repository);
            new ValueWriter(repository)
                    .write(new FormatVersion().toBytes())
                    .flatMap(oid -> repo.insertNewTree(GIT_DB_VERSION, oid))
                    .flatMap(tid -> repo.initialCommit(tid, INIT_MESSAGE, INIT_USER, INIT_EMAIL))
                    .flatMap(cid -> createBranch(repository, cid, MASTER));
        };
    }

    private static Result<Path> createBranch(
            final Repository repository,
            final ObjectId commitId,
            final String branchName
    ) {
        final Path branchRefPath = branchRefPath(repository, branchName);
        final byte[] commitIdBytes = commitId.name().getBytes(StandardCharsets.UTF_8);
        return Result.of(() -> Files.write(branchRefPath, commitIdBytes));
    }

    private static Path branchRefPath(
            final Repository repository,
            final String branchName
    ) {
        return repository.getDirectory()
                .toPath()
                .resolve(String.format(REFS_HEADS_FORMAT, branchName))
                .toAbsolutePath();
    }
}
