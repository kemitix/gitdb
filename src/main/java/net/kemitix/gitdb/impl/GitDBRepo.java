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

import lombok.val;
import org.eclipse.jgit.lib.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Wrapper for interacting with the GitDB Repository.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
class GitDBRepo {

    private final Repository repository;
    private final ValueWriter valueWriter;
    private final KeyWriter keyWriter;
    private final CommitWriter commitWriter;
    private final KeyRemover keyRemover;

    /**
     * Creates a new instance of this class.
     *
     * @param repository the Git Repository
     */
    GitDBRepo(final Repository repository) {
        this.repository = repository;
        valueWriter = new ValueWriter(repository);
        keyWriter = new KeyWriter(repository);
        commitWriter = new CommitWriter(repository);
        keyRemover = new KeyRemover(repository);
    }

    /**
     * Insert a blob into the store, returning its unique id.
     *
     * @param blob content of the blob
     * @return the id of the blob
     * @throws IOException the blob could not be stored
     */
    ObjectId insertBlob(final byte[] blob) throws IOException {
        return valueWriter.write(blob);
    }

    /**
     * Insert a new, empty tree into the store, returning its unique id.
     *
     * @param key     the key to insert
     * @param valueId id of the value
     * @return the id of the inserted tree
     * @throws IOException the tree could not be stored
     */
    ObjectId insertNewTree(
            final String key,
            final ObjectId valueId
    ) throws IOException {
        return keyWriter.writeFirst(key, valueId);
    }

    /**
     * Insert a tree into the store, copying the exiting tree from the branch, returning its new unique id.
     *
     * @param branchRef the branch to copy the tree from
     * @param key       the key to insert
     * @param valueId   id of the value
     * @return the id of the inserted tree
     * @throws IOException the tree could not be stored
     */
    ObjectId insertTree(
            final Ref branchRef,
            final String key,
            final ObjectId valueId
    ) throws IOException {
        return keyWriter.write(key, valueId, branchRef);
    }

    /**
     * Insert a commit into the store, returning its unique id.
     *
     * @param treeId           id of the tree
     * @param branchRef        the branch to commit to
     * @param message          the message
     * @param userName         the user name
     * @param userEmailAddress the user email address
     * @return the id of the commit
     * @throws IOException the commit could not be stored
     */
    ObjectId insertCommit(
            final ObjectId treeId,
            final String message,
            final String userName,
            final String userEmailAddress,
            final Ref branchRef
    ) throws IOException {
        return commitWriter.write(treeId, branchRef, message, userName, userEmailAddress);
    }

    private Ref writeHead(
            final String branchName,
            final ObjectId commitId
    ) throws IOException {
        final Path branchRefPath = repository
                .getDirectory()
                .toPath()
                .resolve(branchName)
                .toAbsolutePath();
        final byte[] commitIdBytes = commitId.name().getBytes(StandardCharsets.UTF_8);
        Files.write(branchRefPath, commitIdBytes);
        return repository.findRef(branchName);
    }

    /**
     * Reads a value from the branch with the given key.
     *
     * @param branchRef the branch to select from
     * @param key       the key to get the value for
     * @return an Optional containing the value if found, or empty
     * @throws IOException if there was an error reading the value
     */
    Optional<String> readValue(
            final Ref branchRef,
            final String key
    ) throws IOException {
        val blob = new GitTreeReader(repository)
                .treeFilter(key)
                .stream(branchRef)
                .findFirst();
        if (blob.isPresent()) {
            return Optional.of(blob.get().blobAsString());
        }
        return Optional.empty();
    }

    /**
     * Add the key/value to the repo, returning the tree containing the update.
     *
     * <p>N.B. this creates a tree that has not been committed, the branch remains unaware of the update.</p>
     *
     * @param branchRef the branch to start from
     * @param key       the key to place the value under
     * @param value     the value
     * @return the id of the updated tree containing the update
     * @throws IOException if there was an error writing the value
     */
    ObjectId writeValue(final Ref branchRef, final String key, final String value) throws IOException {
        final ObjectId blob = insertBlob(value.getBytes(StandardCharsets.UTF_8));
        return insertTree(branchRef, key, blob);
    }

    /**
     * Updates the branch to point to the new commit.
     *
     * @param branchRef        the branch to update
     * @param tree             the tree to commit onto the branch
     * @param message          the commit message
     * @param userName         the user name
     * @param userEmailAddress the user email address
     * @return the Ref of the updated branch
     * @throws IOException if there was an error writing the branch
     */
    Ref writeCommit(
            final Ref branchRef,
            final ObjectId tree,
            final String message,
            final String userName,
            final String userEmailAddress
    ) throws IOException {
        final ObjectId commitId = insertCommit(tree, message, userName, userEmailAddress, branchRef);
        return writeHead(branchRef.getName(), commitId);
    }

    /**
     * Writes the initial commit into a repo (no parent).
     *
     * @param treeId      the to commit
     * @param initMessage the commit message
     * @param initUser    the user name
     * @param initEmail   the user email address
     * @return the id of the commit
     * @throws IOException if there was an error writing the commit
     */
    ObjectId initialCommit(
            final ObjectId treeId,
            final String initMessage,
            final String initUser,
            final String initEmail
    ) throws IOException {
        return commitWriter.write(treeId, ObjectId.zeroId(), initMessage, initUser, initEmail);
    }

    /**
     * Remove the key from the branch, returning the tree containing the update.
     *
     * <p>N.B. this creates a tree that has not been committed, the branch remains unaware of the update.</p>
     *
     * @param branchRef the branch to start from
     * @param key       the key to place the value under
     * @return an Optional containing the id of the updated tree containing the update, if the key was found, or an
     * empty Optional if there key was not found, the there was no changes made
     * @throws IOException if there was an error writing the value
     */
    Optional<ObjectId> removeKey(final Ref branchRef, final String key) throws IOException {
        return keyRemover.remove(branchRef, key);
    }
}
