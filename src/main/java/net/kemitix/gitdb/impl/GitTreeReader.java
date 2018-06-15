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
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

import java.io.IOException;
import java.util.Optional;
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
     * @throws IOException if there is an error reading the commit or walking the tree
     */
    Stream<NamedRevBlob> stream(final Ref branchRef) throws IOException {
        final TreeWalk treeWalk = new TreeWalk(repository);
        treeWalk.addTree(new RevWalk(repository).parseCommit(branchRef.getObjectId()).getTree());
        treeWalk.setRecursive(false);
        Optional.ofNullable(treeFilter)
                .ifPresent(treeWalk::setFilter);
        final Stream.Builder<NamedRevBlob> builder = Stream.builder();
        while (treeWalk.next()) {
            builder.add(new NamedRevBlob(
                    treeWalk.getNameString(),
                    new RevWalk(repository).lookupBlob(treeWalk.getObjectId(0)),
                    repository));
        }
        return builder.build();
    }

    /**
     * Sets a path filter to limit the stream by.
     *
     * @param path the path to filter by
     * @return the GitTreeReader
     */
    GitTreeReader treeFilter(final String path) {
        this.treeFilter = PathFilter.create(path);
        return this;
    }

}
