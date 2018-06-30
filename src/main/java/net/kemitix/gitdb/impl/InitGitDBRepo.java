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
import java.io.IOException;
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
     * @throws IOException if there is an error in creating the repo files
     */
    static Result<Void> create(final Path dbDir) {
        final InitGitDBRepo initRepo = new InitGitDBRepo();
        final File validDbDir;
        try {
            validDbDir = initRepo.validDbDir(dbDir.toFile());
        } catch (IOException e) {
            return Result.error(e);
        }
        validDbDir.mkdirs();
        try (Repository repository = RepositoryCache.FileKey.exact(validDbDir, FS.DETECTED).open(false)) {
            repository.create(true);
            initRepo.createInitialBranchOnMaster(repository);
        } catch (IOException e) {
            return Result.error(e);
        }
        return Result.ok(null);
    }

    private File validDbDir(final File dbDir) throws IOException {
        verifyIsNotAFile(dbDir);
        if (dbDir.exists()) {
            verifyIsEmpty(dbDir);
        }
        return dbDir;
    }

    private Result<Void> createInitialBranchOnMaster(final Repository repository) {
        final GitDBRepo repo = new GitDBRepo(repository);
        return new ValueWriter(repository)
                .write(new FormatVersion().toBytes())
                .flatMap(oid -> repo.insertNewTree(GIT_DB_VERSION, oid))
                .flatMap(tid -> Result.of(() -> repo.initialCommit(tid, INIT_MESSAGE, INIT_USER, INIT_EMAIL)))
                .flatMap(cid -> Result.of(() -> {
                    createBranch(repository, cid, MASTER);
                    return null;
                }));
    }

    private void verifyIsNotAFile(final File dbDir) throws NotDirectoryException {
        if (dbDir.isFile()) {
            throw new NotDirectoryException(dbDir.toString());
        }
    }

    private void verifyIsEmpty(final File dbDir) throws IOException {
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dbDir.toPath())) {
            if (directoryStream.iterator().hasNext()) {
                throw new DirectoryNotEmptyException(dbDir.toString());
            }
        }
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
