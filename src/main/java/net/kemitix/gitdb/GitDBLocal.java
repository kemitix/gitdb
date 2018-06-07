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

    /**
     * Create a new GitDB instance, while initialising a new git repo.
     *
     * @param dbDir the path to instantiate the git repo in
     * @throws IOException if there {@code dbDir} is a file or a non-empty directory
     */
    GitDBLocal(final File dbDir) throws IOException {
        validateDbDir(dbDir);
        this.repository = initRepo(dbDir);
    }

    /**
     * Create a new GitDB instance using the Git repo.
     *
     * @param repository the Git repository
     */
    GitDBLocal(final Repository repository) {
        this.repository = repository;
    }

    private void validateDbDir(final File dbDir) throws IOException {
        verifyIsNotAFile(dbDir);
        if (dbDir.exists()) {
            verifyIsEmpty(dbDir);
        }
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

    private static Repository initRepo(final File dbDir) throws IOException {
        dbDir.mkdirs();
        final Repository repository = RepositoryCache.FileKey.exact(dbDir, FS.DETECTED).open(false);
        repository.create(true);
        createInitialBranchOnMaster(repository);
        return repository;
    }

    private static void createInitialBranchOnMaster(final Repository repository) throws IOException {
        // create empty file
        final ObjectId objectId = insertAnEmptyBlob(repository);
        // create tree
        final ObjectId treeId = insertTree(repository, objectId);
        // create commit
        final ObjectId commitId = insertCommit(repository, treeId);
        // create branch
        writeBranch(repository, commitId, "master");
    }

    private static void writeBranch(
            final Repository repository,
            final ObjectId commitId,
            final String branchName
    ) throws IOException {
        final Path branchRefPath =
                repository.getDirectory().toPath().resolve("refs/heads/" + branchName).toAbsolutePath();
        final byte[] commitIdBytes = commitId.name().getBytes(StandardCharsets.UTF_8);
        Files.write(branchRefPath, commitIdBytes);
    }

    private static ObjectId insertCommit(
            final Repository repository,
            final ObjectId treeId
    ) throws IOException {
        final CommitBuilder commitBuilder = new CommitBuilder();
        commitBuilder.setTreeId(treeId);
        commitBuilder.setMessage("Initialise GitDB v1");
        final PersonIdent ident = new PersonIdent("GitDB", "pcampbell@kemitix.net");
        commitBuilder.setAuthor(ident);
        commitBuilder.setCommitter(ident);
        commitBuilder.setParentId(ObjectId.zeroId());
        return repository.getObjectDatabase().newInserter().insert(commitBuilder);
    }

    private static ObjectId insertTree(
            final Repository repository,
            final ObjectId objectId
    ) throws IOException {
        final TreeFormatter treeFormatter = new TreeFormatter();
        treeFormatter.append("isGitDB", FileMode.REGULAR_FILE, objectId);
        return repository.getObjectDatabase().newInserter().insert(treeFormatter);
    }

    private static ObjectId insertAnEmptyBlob(final Repository repository) throws IOException {
        return repository.getObjectDatabase().newInserter().insert(Constants.OBJ_BLOB, new byte[0]);
    }

    @Override
    public Optional<GitDBBranch> branch(final String name) throws IOException {
        return Optional.ofNullable(repository.findRef(name))
                .map(GitDBBranch::withRef);
    }

}
