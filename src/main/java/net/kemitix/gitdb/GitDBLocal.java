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

package net.kemitix.gitdb;

import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.util.FS;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Implementation of GitDB for working with a local Repo.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */

class GitDBLocal implements GitDB {

    private final Repository repository;
    private final String userName;
    private final String userEmailAddress;

    /**
     * Create a new GitDB instance, while initialising a new git repo.
     *
     * @param dbDir            the path to instantiate the git repo in
     * @param userName         the user name
     * @param userEmailAddress the user email address
     * @throws IOException if there {@code dbDir} is a file or a non-empty directory
     */
    GitDBLocal(
            final File dbDir,
            final String userName,
            final String userEmailAddress
    ) throws IOException {
        this(GitDBLocal.initRepo(validDbDir(dbDir)), userName, userEmailAddress);
    }

    /**
     * Create a new GitDB instance using the Git repo.
     *
     * @param repository       the Git repository
     * @param userName         the user name
     * @param userEmailAddress the user email address
     */
    GitDBLocal(final Repository repository, final String userName, final String userEmailAddress) {
        this.repository = repository;
        this.userName = userName;
        this.userEmailAddress = userEmailAddress;
    }

    private static File validDbDir(final File dbDir) throws IOException {
        verifyIsNotAFile(dbDir);
        if (dbDir.exists()) {
            verifyIsEmpty(dbDir);
        }
        return dbDir;
    }

    private static void verifyIsEmpty(final File dbDir) throws IOException {
        if (Files.newDirectoryStream(dbDir.toPath()).iterator().hasNext()) {
            throw new DirectoryNotEmptyException(dbDir.toString());
        }
    }

    private static void verifyIsNotAFile(final File dbDir) throws NotDirectoryException {
        if (dbDir.isFile()) {
            throw new NotDirectoryException(dbDir.toString());
        }
    }

    @Override
    public Optional<GitDBBranch> branch(final String name) throws IOException {
        return Optional.ofNullable(repository.findRef(name))
                .map(ref -> GitDBBranch.withRef(ref, GitDBRepo.in(repository), userName, userEmailAddress));
    }

    private static Repository initRepo(final File dbDir) throws IOException {
        dbDir.mkdirs();
        final Repository repository = RepositoryCache.FileKey.exact(dbDir, FS.DETECTED).open(false);
        repository.create(true);
        createInitialBranchOnMaster(repository);
        return repository;
    }

    private static void createInitialBranchOnMaster(final Repository repository) throws IOException {
        final GitDBRepo repo = GitDBRepo.in(repository);
        final ObjectId objectId = repo.insertBlob(new byte[0]);
        final ObjectId treeId = repo.insertNewTree("isGitDB", objectId);
        final ObjectId commitId = repo.insertCommit(
                treeId,
                "Initialise GitDB v1",
                "GitDB",
                "pcampbell@kemitix.net",
                ObjectId.zeroId());
        createBranch(repository, commitId, "master");
    }

    private static void createBranch(
            final Repository repository,
            final ObjectId commitId,
            final String branchName
    ) throws IOException {
        final Path branchRefPath = repository
                .getDirectory()
                .toPath()
                .resolve("refs/heads/" + branchName)
                .toAbsolutePath();
        final byte[] commitIdBytes = commitId.name().getBytes(StandardCharsets.UTF_8);
        Files.write(branchRefPath, commitIdBytes);
    }

}
