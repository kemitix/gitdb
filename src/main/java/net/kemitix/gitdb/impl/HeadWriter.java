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

import lombok.RequiredArgsConstructor;
import net.kemitix.mon.result.Result;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes the head.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
@RequiredArgsConstructor
class HeadWriter {

    private final Repository repository;

    /**
     * Writes the head for the named branch for the given commit.
     *
     * @param branchName the branch name
     * @param commitId   the commit to point the branch at
     * @return the Ref of the new branch
     */
    Result<Ref> write(final String branchName, final ObjectId commitId) {
        return writeRef(branchName, commitId, repository)
                .flatMap(x -> findRef(branchName, repository));
    }

    private static Result<Path> writeRef(
            final String branchName,
            final ObjectId commitId,
            final Repository repository
    ) {
        return Result.of(() ->
                Files.write(
                        branchRefPath(branchName, repository).toAbsolutePath(),
                        commitIdBytes(commitId)));
    }

    private static Result<Ref> findRef(final String branchName, final Repository repository) {
        return Result.of(() -> repository.findRef(branchName));
    }

    private static Path branchRefPath(final String branchName, final Repository repository) {
        return gitDirPath(repository).resolve(branchName);
    }

    private static byte[] commitIdBytes(final ObjectId commitId) {
        return commitId.name().getBytes(StandardCharsets.UTF_8);
    }

    private static Path gitDirPath(final Repository repository) {
        return gitDir(repository).toPath();
    }

    private static File gitDir(final Repository repository) {
        return repository.getDirectory();
    }
}
