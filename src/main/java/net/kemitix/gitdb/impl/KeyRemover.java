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
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TreeFormatter;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Remove Key from the Git Repository.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
@RequiredArgsConstructor
class KeyRemover {

    private final Repository repository;

    /**
     * Sets the boolean to true if the key matches a NamedRevBlob's name.
     *
     * @param key     the key to match
     * @param removed the boolean to update
     * @return a Consumer
     */
    private static Consumer<NamedRevBlob> flagIfFound(final String key, final AtomicBoolean removed) {
        return nvb -> {
            if (nvb.getName().equals(key)) {
                removed.set(true);
            }
        };
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
     * Remove a key from the repository.
     *
     * @param branchRef the branch to update
     * @param key       the key to remove
     * @return the id of the updated tree
     * @throws IOException if there is an error writing the value
     */
    Optional<ObjectId> remove(final Ref branchRef, final String key) throws IOException {
        final TreeFormatter treeFormatter = new TreeFormatter();
        final AtomicBoolean removed = new AtomicBoolean(false);
        new GitTreeReader(repository)
                .stream(branchRef)
                .peek(flagIfFound(key, removed))
                .filter(isNotKey(key))
                .forEach(addToTree(treeFormatter));
        if (removed.get()) {
            return Optional.of(insertTree(treeFormatter));
        }
        return Optional.empty();
    }

    /**
     * Insert a tree into the repo, returning its id.
     *
     * @param treeFormatter the formatter containing the proposed tree's data.
     * @return the name of the tree object.
     * @throws IOException the object could not be stored.
     */
    private ObjectId insertTree(final TreeFormatter treeFormatter) throws IOException {
        return repository.getObjectDatabase().newInserter().insert(treeFormatter);
    }
}
