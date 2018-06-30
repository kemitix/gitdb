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
import net.kemitix.mon.maybe.Maybe;
import net.kemitix.mon.result.Result;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

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

    /**
     * Initialise the creation of new GitDBBranch instances.
     *
     * @param repository       the Git Repository
     * @param userName         the user name to record against changes
     * @param userEmailAddress the user's email address to record against changes
     * @return a Function for creating a GitDBBranch when supplied with a Ref for a branch
     */
    static Function<Ref, Result<GitDBBranch>> init(
            final Repository repository,
            final String userName,
            final String userEmailAddress
    ) {
        return ref -> select(ref, new GitDBRepo(repository), userName, userEmailAddress);
    }

    private static Result<GitDBBranch> select(
            final Ref branchRef,
            final GitDBRepo gitDBRepo,
            final String userName,
            final String userEmailAddress
    ) {
        return Result.ok(new GitDBBranchImpl(branchRef, gitDBRepo, userName, userEmailAddress, branchRef.getName()));
    }

    @Override
    public Result<Maybe<String>> get(final String key) {
        return gitDBRepo.readValue(branchRef, KEY_PREFIX + key);
    }

    @Override
    public Result<GitDBBranch> put(final String key, final String value) {
        final String message = String.format("Add key [%s] = [%s]", key, value);
        return gitDBRepo.writeValue(branchRef, KEY_PREFIX + key, value)
                .flatMap(nt -> gitDBRepo.writeCommit(branchRef, nt, message, userName, userEmailAddress))
                .flatMap(nb -> select(nb, gitDBRepo, userName, userEmailAddress));
    }

    @Override
    public Result<GitDBBranch> remove(final String key) {
        return gitDBRepo.removeKey(branchRef, KEY_PREFIX + key).flatMap(treeId ->
                writeRemoveKeyCommit(key, treeId)
                        .map(selectUpdatedBranch())
                        .orElse(Result.ok(this)));
    }

    private Maybe<Result<Ref>> writeRemoveKeyCommit(final String key, final Maybe<ObjectId> idMaybe) {
        return idMaybe.map(objectId -> {
            final String message = String.format("Remove Key [%s]", key);
            return gitDBRepo.writeCommit(branchRef, objectId, message, userName, userEmailAddress);
        });
    }

    private Function<Result<Ref>, Result<GitDBBranch>> selectUpdatedBranch() {
        return refResult -> refResult.flatMap(ref ->
                select(ref, gitDBRepo, userName, userEmailAddress));
    }

    @Override
    public Result<Maybe<Version>> getFormatVersion() {
        return gitDBRepo.readValue(branchRef, "GitDB.Version")
                .map(version -> version.map(Version::valueOf));
    }

}
