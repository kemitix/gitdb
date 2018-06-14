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

import org.eclipse.jgit.lib.*;

import java.io.IOException;

/**
 * Commits Key/Value updates into the Git Repository.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
class CommitWriter {

    private final ObjectInserter objectInserter;

    /**
     * Create new instance of this class.
     *
     * @param repository the repository to write commits to
     */
    CommitWriter(final Repository repository) {
        objectInserter = repository.getObjectDatabase().newInserter();
    }

    /**
     * Write a commit into the repository.
     *
     * @param treeId the tree to commit
     * @param parentId the id of the parent commit
     * @param message          the message
     * @param userName         the user name
     * @param userEmailAddress the user email address
     * @return the id of the commit
     * @throws IOException if there is an error writing the value
     */
    ObjectId write(
            final ObjectId treeId,
            final ObjectId parentId,
            final String message,
            final String userName,
            final String userEmailAddress
    ) throws IOException {
        final CommitBuilder commitBuilder = new CommitBuilder();
        commitBuilder.setTreeId(treeId);
        commitBuilder.setMessage(message);
        final PersonIdent ident = new PersonIdent(userName, userEmailAddress);
        commitBuilder.setAuthor(ident);
        commitBuilder.setCommitter(ident);
        commitBuilder.setParentId(parentId);
        return objectInserter.insert(commitBuilder);
    }

    /**
     * Write a commit into the repository.
     *
     * <p>N.B. While this adds the commit to a branch, it doesn't update the branch itself.</p>
     *
     * @param treeId the tree to commit
     * @param branchRef the branch to add the commit to
     * @param message          the message
     * @param userName         the user name
     * @param userEmailAddress the user email address
     * @return the id of the commit
     * @throws IOException if there is an error writing the value
     */
    ObjectId write(
            final ObjectId treeId,
            final Ref branchRef,
            final String message,
            final String userName,
            final String userEmailAddress
    ) throws IOException {
        return write(treeId, branchRef.getObjectId(), message, userName, userEmailAddress);
    }
}
