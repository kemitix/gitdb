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

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Function;

/**
 * API for interacting with a branch in a GirDB.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class GitDBBranch {

    private static final String KEY_PREFIX = "key:";
    private final Ref branchRef;
    private final GitDBRepo gitDBRepo;
    private final String userName;
    private final String userEmailAddress;

    private static GitDBBranch select(
            final Ref branchRef,
            final GitDBRepo gitDBRepo,
            final String userName,
            final String userEmailAddress
    ) {
        return new GitDBBranch(branchRef, gitDBRepo, userName, userEmailAddress);
    }

    /**
     * Initialise the creation of new GitDBBranch instances.
     *
     * @param repository the Git Repository
     * @param userName the user name to record against changes
     * @param userEmailAddress the user's email address to record against changes
     * @return a Function for creating a GitDBBranch when supplied with a Ref for a branch
     */
    static Function<Ref, GitDBBranch> init(
            final Repository repository,
            final String userName,
            final String userEmailAddress
    ) {
        return ref -> select(ref, GitDBRepo.in(repository), userName, userEmailAddress);
    }

    /**
     * Lookup a value for the key.
     *
     * @param key the key to lookup
     * @return an Optional containing the value, if it exists, or empty if not
     * @throws IOException if there was an error reading the value
     */
    public Optional<String> get(final String key) throws IOException {
        return gitDBRepo.readValue(branchRef, KEY_PREFIX + key);
    }

    /**
     * Put a value into the store for the key.
     *
     * @param key   the key to place the value under
     * @param value the value (must be Serializable)
     * @return an updated branch containing the new key/value
     * @throws IOException if there was an error writing the value
     */
    public GitDBBranch put(final String key, final String value) throws IOException {
        final ObjectId objectId = insertBlob(value.getBytes(StandardCharsets.UTF_8));
        final ObjectId treeId = insertTree(KEY_PREFIX + key, objectId);
        final String commitMessage = String.format("Add key [%s] = [%s]", key, value);
        final ObjectId commitId = insertCommit(treeId, commitMessage);
        return updateBranch(commitId);
    }

    /**
     * Removes a key and its value from the store.
     *
     * @param key the key to remove
     * @return an updated branch without the key, or the original if the key was not found
     */
    public GitDBBranch remove(final String key) {
        return this;
    }

    private ObjectId insertBlob(final byte[] blob) throws IOException {
        return gitDBRepo.insertBlob(blob);
    }

    private ObjectId insertTree(final String key, final ObjectId valueId) throws IOException {
        return gitDBRepo.insertTree(branchRef, key, valueId);
    }

    private ObjectId insertCommit(final ObjectId treeId, final String message) throws IOException {
        final ObjectId headCommitId = branchRef.getObjectId();
        return gitDBRepo.insertCommit(treeId, message, userName, userEmailAddress, headCommitId);
    }

    private GitDBBranch updateBranch(final ObjectId commitId) throws IOException {
        final Ref updatedRef = gitDBRepo.writeHead(branchRef.getName(), commitId);
        return select(updatedRef, gitDBRepo, userName, userEmailAddress);
    }
}
