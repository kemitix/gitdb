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

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.kemitix.mon.result.Result;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Reads the entries in a Git Tree object.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class GitTreeReader {

    private final Repository repository;

    private TreeFilter treeFilter;

    /**
     * Opens a stream of entries found on the branch.
     *
     * @param branchRef the branch to read
     * @return a stream of key/value pairs as NamedRevBlobs
     */
    Result<Stream<NamedRevBlob>> entries(final Ref branchRef) {
        final RevWalk revWalk = new RevWalk(repository);
        return Result.of(parseTree(branchRef, revWalk))
                .andThen(configureFilter())
                .andThen(buildStream(revWalk));
    }

    private Callable<RevTree> parseTree(final Ref branchRef, final RevWalk revWalk) {
        return () -> revWalk.parseCommit(branchRef.getObjectId()).getTree();
    }

    private Function<RevTree, Callable<TreeWalk>> configureFilter() {
        return tree -> () -> {
            final TreeWalk treeWalk = new TreeWalk(repository);
            treeWalk.addTree(tree);
            treeWalk.setRecursive(false);
            Optional.ofNullable(treeFilter)
                    .ifPresent(treeWalk::setFilter);
            return treeWalk;
        };
    }

    private Function<TreeWalk, Callable<Stream<NamedRevBlob>>> buildStream(final RevWalk revWalk) {
        return treeWalk -> () -> {
            final Stream.Builder<NamedRevBlob> builder = Stream.builder();
            while (treeWalk.next()) {
                builder.add(namedRevBlob(treeWalk, revWalk));
            }
            return builder.build();
        };
    }

    private NamedRevBlob namedRevBlob(final TreeWalk treeWalk, final RevWalk revWalk) {
        return new NamedRevBlob(
                treeWalk.getNameString(),
                revWalk.lookupBlob(treeWalk.getObjectId(0)),
                repository);
    }

    /**
     * Sets a path filter to limit the stream by.
     *
     * @param path the path to filter by
     * @return the GitTreeReader
     */
    GitTreeReader treeFilter(final String path) {
        treeFilter = PathFilter.create(path);
        return this;
    }

}
