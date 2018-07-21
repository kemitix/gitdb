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
import net.kemitix.mon.maybe.Maybe;
import net.kemitix.mon.result.Result;
import net.kemitix.mon.result.WithResultContinuation;
import org.eclipse.jgit.lib.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static net.kemitix.conditional.Condition.where;

/**
 * Remove Key from the Git Repository.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
@RequiredArgsConstructor
class KeyRemover {

    private final Repository repository;

    /**
     * Remove a key from the repository.
     *
     * @param branchRef the branch to update
     * @param key       the key to remove
     * @return the id of the updated tree
     */
    Result<Maybe<ObjectId>> remove(final Ref branchRef, final String key) {
        final TreeFormatter treeFormatter = new TreeFormatter();
        final AtomicBoolean removed = new AtomicBoolean(false);
        return new GitTreeReader(repository)
                .entries(branchRef)
                .thenWith(s -> addOthersToTree(key, treeFormatter, removed, s))
                .flatMap(s -> insertTree(treeFormatter))
                .maybe(oi -> removed.get());
    }

    private static WithResultContinuation<Stream<NamedRevBlob>> addOthersToTree(
            final String key,
            final TreeFormatter treeFormatter,
            final AtomicBoolean removed,
            final Stream<NamedRevBlob> s) {
        return () -> s
                .peek(flagIfFound(key, removed))
                .filter(isNotKey(key))
                .forEach(addToTree(treeFormatter));
    }

    /**
     * Sets the boolean to true if the key matches a NamedRevBlob's name.
     *
     * @param key     the key to match
     * @param removed the boolean to update
     * @return a Consumer
     */
    private static Consumer<NamedRevBlob> flagIfFound(final String key, final AtomicBoolean removed) {
        return nvb -> where(nvb.getName().equals(key))
                .then(() -> removed.set(true));
    }

    /**
     * Filter to exclude named blobs where the name matches the key.
     *
     * @param key the key to match
     * @return a Predicate
     */
    private static Predicate<NamedRevBlob> isNotKey(final String key) {
        return item -> !item.getName().equals(key);
    }

    /**
     * Adds the named value blob to the tree formatter.
     *
     * @param treeFormatter the tree formatter to add to
     * @return a Consumer
     */
    private static Consumer<NamedRevBlob> addToTree(final TreeFormatter treeFormatter) {
        return item -> treeFormatter.append(item.getName(), item.getRevBlob());
    }

    /**
     * Insert a tree into the repo, returning its id.
     *
     * @param treeFormatter the formatter containing the proposed tree's data.
     * @return the name of the tree object.
     */
    private Result<ObjectId> insertTree(final TreeFormatter treeFormatter) {
        return Result.ok(repository.getObjectDatabase())
                .map(ObjectDatabase::newInserter)
                .andThen(inserter -> () -> inserter.insert(treeFormatter));
    }
}
