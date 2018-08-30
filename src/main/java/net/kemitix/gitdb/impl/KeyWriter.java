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

import net.kemitix.mon.result.Result;
import org.eclipse.jgit.lib.*;

/**
 * Writes Keys into the Git Repository.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
class KeyWriter {

    private final Repository repository;
    private final ObjectInserter objectInserter;

    /**
     * Create new instance of this class.
     *
     * @param repository the repository to write keys to
     */
    KeyWriter(final Repository repository) {
        this.repository = repository;
        objectInserter = repository.getObjectDatabase().newInserter();
    }

    /**
     * Write the first key into a new tree.
     *
     * @param key     the key
     * @param valueId the id of the value
     * @return the id of the new tree
     */
    Result<ObjectId> writeFirst(final String key, final ObjectId valueId) {
        return writeTree(key, valueId, new TreeFormatter());
    }

    /**
     * Write the key into a tree.
     *
     * @param key       the key
     * @param valueId   the id of the value
     * @param branchRef the branch whose tree should be updated
     * @return the id of the updated tree
     */
    Result<ObjectId> write(final String key, final ObjectId valueId, final Ref branchRef) {
        return getTreeFormatter(branchRef)
                .flatMap(f -> writeTree(key, valueId, f));
    }

    private Result<TreeFormatter> getTreeFormatter(final Ref branchRef) {
        final TreeFormatter treeFormatter = new TreeFormatter();
        final GitTreeReader gitTreeReader = new GitTreeReader(repository);
        return gitTreeReader.entries(branchRef)
                .peek(s -> s.forEach(item -> treeFormatter.append(item.getName(), item.getRevBlob())))
                .map(x -> treeFormatter);
    }

    private Result<ObjectId> writeTree(
            final String key,
            final ObjectId valueId,
            final TreeFormatter treeFormatter
    ) {
        treeFormatter.append(key, FileMode.REGULAR_FILE, valueId);
        return Result.of(() -> objectInserter.insert(treeFormatter));
    }
}
