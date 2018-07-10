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
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.util.FS;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * Initialise a new GitDB Repo.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
class InitGitDBRepo {

    private static final String INIT_MESSAGE = "Initialise GitDB v1";
    private static final String INIT_USER = "GitDB";
    private static final String INIT_EMAIL = "pcampbell@kemitix.net";
    private static final String MASTER = "master";
    private static final String GIT_DB_VERSION = "GitDB.Version";
    private static final String REFS_HEADS_FORMAT = "refs/heads/%s";

    /**
     * Initialise a new GitDB repo.
     *
     * @param dbDir the directory to initialise the repo in
     * @return a Result containing the created Repository
     */
    static Result<Repository> create(final Path dbDir) {
        final InitGitDBRepo initRepo = new InitGitDBRepo();
        return initRepo.validDbDir(dbDir.toFile())
                .peek(File::mkdirs)
                .map(dir -> RepositoryCache.FileKey.exact(dir, FS.DETECTED))
                .andThen(fileKey -> () -> fileKey.open(false))
                .thenWith(repository -> () -> repository.create(true))
                .thenWith(repository -> () -> initRepo.createInitialBranchOnMaster(repository));
    }

    private Result<File> validDbDir(final File dbDir) {
        return Result.ok(dbDir)
                .flatMap(this::verifyIsNotAFile)
                .flatMap(this::isEmptyIfExists);
    }

    private Result<Void> createInitialBranchOnMaster(final Repository repository) {
        final GitDBRepo repo = new GitDBRepo(repository);
        return new ValueWriter(repository)
                .write(new FormatVersion().toBytes())
                .flatMap(oid -> repo.insertNewTree(GIT_DB_VERSION, oid))
                .flatMap(tid -> repo.initialCommit(tid, INIT_MESSAGE, INIT_USER, INIT_EMAIL))
                .flatMap(cid -> Result.of(() -> {
                    createBranch(repository, cid, MASTER);
                    return null;
                }));
    }

    private Result<File> verifyIsNotAFile(final File dbDir) {
        if (dbDir.isFile()) {
            return Result.error(new NotDirectoryException(dbDir.toString()));
        }
        return Result.ok(dbDir);
    }

    private Result<File> isEmptyIfExists(final File dbDir) {
        if (dbDir.exists()) {
            return Result.of(() -> {
                        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dbDir.toPath())) {
                            if (directoryStream.iterator().hasNext()) {
                                throw new DirectoryNotEmptyException(dbDir.toString());
                            }
                        }
                        return dbDir;
                    }
            );
        }
        return Result.ok(dbDir);
    }

    private Result<Void> createBranch(
            final Repository repository,
            final ObjectId commitId,
            final String branchName
    ) {
        final Path branchRefPath = branchRefPath(repository, branchName);
        final byte[] commitIdBytes = commitId.name().getBytes(StandardCharsets.UTF_8);
        return Result.of(() -> {
            Files.write(branchRefPath, commitIdBytes);
            return null;
        });
    }

    private Path branchRefPath(
            final Repository repository,
            final String branchName
    ) {
        return repository.getDirectory()
                .toPath()
                .resolve(String.format(REFS_HEADS_FORMAT, branchName))
                .toAbsolutePath();
    }
}
