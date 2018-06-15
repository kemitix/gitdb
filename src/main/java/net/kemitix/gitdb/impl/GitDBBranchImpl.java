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

import com.github.zafarkhaja.semver.Version;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.kemitix.gitdb.GitDBBranch;
import net.kemitix.gitdb.GitDBTransaction;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

import java.io.*;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * API for interacting with a branch in a GirDB.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class GitDBBranchImpl implements GitDBBranch {

    private static final String KEY_PREFIX = "key:";
    private final Ref branchRef;
    private final GitDBRepo gitDBRepo;
    private final String userName;
    private final String userEmailAddress;
    private final String name;

    private static GitDBBranch select(
            final Ref branchRef,
            final GitDBRepo gitDBRepo,
            final String userName,
            final String userEmailAddress
    ) {
        return new GitDBBranchImpl(branchRef, gitDBRepo, userName, userEmailAddress, branchRef.getName());
    }

    /**
     * Initialise the creation of new GitDBBranch instances.
     *
     * @param repository       the Git Repository
     * @param userName         the user name to record against changes
     * @param userEmailAddress the user's email address to record against changes
     * @return a Function for creating a GitDBBranch when supplied with a Ref for a branch
     */
    static Function<Ref, GitDBBranch> init(
            final Repository repository,
            final String userName,
            final String userEmailAddress
    ) {
        return ref -> select(ref, new GitDBRepo(repository), userName, userEmailAddress);
    }

    @Override
    public Optional<String> get(final String key) throws IOException {
        return gitDBRepo.readValue(branchRef, KEY_PREFIX + key);
    }

    @Override
    public GitDBBranch put(final String key, final String value) throws IOException {
        final ObjectId newTree = gitDBRepo.writeValue(branchRef, KEY_PREFIX + key, value);
        final Ref newBranch =
                gitDBRepo.writeCommit(branchRef, newTree, commitMessageForAdd(key, value), userName, userEmailAddress);
        return select(newBranch, gitDBRepo, userName, userEmailAddress);
    }

    @Override
    public GitDBBranch remove(final String key) throws IOException {
        final Optional<ObjectId> newTree = gitDBRepo.removeKey(branchRef, KEY_PREFIX + key);
        if (newTree.isPresent()) {
            final Ref newBranch =
                    gitDBRepo.writeCommit(
                            branchRef, newTree.get(),
                            commitMessageForRemove(key),
                            userName,
                            userEmailAddress);
            return select(newBranch, gitDBRepo, userName, userEmailAddress);
        }
        return this;
    }

    @Override
    public Optional<Version> getFormatVersion() throws IOException {
        return gitDBRepo.readValue(branchRef, "GitDB.Version")
                .map(Version::valueOf);
    }

    @Override
    public GitDBTransaction transaction(String name) throws IOException {
        final Ref ref = gitDBRepo.createBranch(branchRef, UUID.randomUUID().toString());
        final GitDBBranch branch = new GitDBBranchImpl(ref, gitDBRepo, userName, userEmailAddress, name);
        return new GitDBTransactionImpl(this, branch);
    }

    @Override
    public GitDBTransaction transaction() throws IOException {
        return transaction(UUID.randomUUID().toString());
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String getCommitId() {
        return branchRef.getObjectId().name();
    }

    private String commitMessageForAdd(final String key, final String value) {
        return String.format("Add key [%s] = [%s]", key, value);
    }

    private String commitMessageForRemove(final String key) {
        return String.format("Remove Key [%s]", key);
    }

}
