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

import net.kemitix.gitdb.GitDB;
import net.kemitix.gitdb.GitDBBranch;
import net.kemitix.gitdb.InvalidRepositoryException;
import net.kemitix.mon.maybe.Maybe;
import net.kemitix.mon.result.Result;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Function;

/**
 * Implementation of GitDB for working with a local Repo.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
final class LocalGitDBImpl implements GitDB, LocalGitDB {

    private static final String NOT_A_BARE_REPO = "Not a bare repo";
    private static final String ERROR_OPENING_REPOSITORY = "Error opening repository";

    private final Repository repository;

    private final Function<Ref, Result<GitDBBranch>> branchInit;

    private LocalGitDBImpl(
            final Repository repository,
            final String userName,
            final String userEmailAddress
    ) {
        this.repository = repository;
        branchInit = GitDBBranchImpl.init(this.repository, userName, userEmailAddress);
    }

    /**
     * Create a new GitDB instance, while initialising a new git repo.
     *
     * @param dbDir            the path to instantiate the git repo in
     * @param userName         the user name
     * @param userEmailAddress the user email address
     * @return a GitDB instance for the created local gitdb
     */
    static Result<GitDB> init(
            final Path dbDir,
            final String userName,
            final String userEmailAddress
    ) {
        return InitGitDBRepo.create(dbDir)
                .flatMap(c -> open(dbDir, userName, userEmailAddress));
    }

    /**
     * Create a new GitDB instance using the Git repo.
     *
     * @param dbDir            the path to instantiate the git repo in
     * @param userName         the user name
     * @param userEmailAddress the user email address
     * @return a GitDB instance for the created local gitdb
     */
    static Result<GitDB> open(
            final Path dbDir,
            final String userName,
            final String userEmailAddress
    ) {
        return gitOpen(dbDir)
                .map(Git::getRepository)
                .maybe(Repository::isBare)
                .flatMap(asErrorIfNotBare(dbDir))
                .map(toLocalGitDB(userName, userEmailAddress));
    }

    private static Result<Git> gitOpen(final Path dbDir) {
        try {
            return Result.ok(Git.open(dbDir.toFile()));
        } catch (IOException e) {
            return Result.error(new InvalidRepositoryException(ERROR_OPENING_REPOSITORY, dbDir, e));
        }
    }

    private static Function<Maybe<Repository>, Result<Repository>> asErrorIfNotBare(final Path dbDir) {
        return maybe -> Result.fromMaybe(maybe, () -> new InvalidRepositoryException(NOT_A_BARE_REPO, dbDir));
    }

    private static Function<Repository, GitDB> toLocalGitDB(final String userName, final String userEmailAddress) {
        return repository -> new LocalGitDBImpl(repository, userName, userEmailAddress);
    }

    @Override
    public Result<Maybe<GitDBBranch>> branch(final String name) {
        return Result.flatMapMaybe(
                Result.of(() -> Maybe.maybe(repository.findRef(name))),
                refMaybe -> Result.invert(refMaybe.map(branchInit)));
    }

}
