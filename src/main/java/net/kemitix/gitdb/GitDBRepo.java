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

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Wrapper for interacting with the GitDB Repository.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class GitDBRepo {

    private final Repository repository;

    /**
     * Create a GitDBRepo wrapper for the Repository.
     *
     * @param repository the repository to wrap
     * @return the GitDBRepo wrapper
     */
    public static GitDBRepo in(final Repository repository) {
        return new GitDBRepo(repository);
    }

    /**
     * Insert a blob into the store, returning its unique id.
     *
     * @param blob content of the blob
     * @return the id of the blob
     * @throws IOException the blob could not be stored
     */
    ObjectId insertBlob(final byte[] blob) throws IOException {
        return repository.getObjectDatabase().newInserter().insert(Constants.OBJ_BLOB, blob);
    }

    /**
     * Insert a new, empty tree into the store, returning its unique id.
     *
     * @param key     the key to insert
     * @param valueId id of the value
     * @return the id of the inserted tree
     * @throws IOException the tree could not be stored
     */
    ObjectId insertNewTree(
            final String key,
            final ObjectId valueId
    ) throws IOException {
        final TreeFormatter treeFormatter = new TreeFormatter();
        return writeTree(key, valueId, treeFormatter);
    }

    /**
     * Insert a tree into the store, copying the exiting tree from the branch, returning its new unique id.
     *
     * @param branchRef the branch to copy the tree from
     * @param key       the key to insert
     * @param valueId   id of the value
     * @return the id of the inserted tree
     * @throws IOException the tree could not be stored
     */
    ObjectId insertTree(
            final Ref branchRef,
            final String key,
            final ObjectId valueId
    ) throws IOException {
        return writeTree(key, valueId, treeFormatterForBranch(branchRef));
    }

    private ObjectId writeTree(
            final String key,
            final ObjectId valueId,
            final TreeFormatter treeFormatter
    ) throws IOException {
        treeFormatter.append(key, FileMode.REGULAR_FILE, valueId);
        return repository.getObjectDatabase().newInserter().insert(treeFormatter);
    }

    /**
     * Insert a commit into the store, returning its unique id.
     *
     * @param treeId           id of the tree
     * @param message          the message
     * @param userName         the user name
     * @param userEmailAddress the user email address
     * @param parent           the commit to link to as parent
     * @return the id of the commit
     * @throws IOException the commit could not be stored
     */
    ObjectId insertCommit(
            final ObjectId treeId,
            final String message,
            final String userName,
            final String userEmailAddress,
            final AnyObjectId parent
    ) throws IOException {
        final CommitBuilder commitBuilder = new CommitBuilder();
        commitBuilder.setTreeId(treeId);
        commitBuilder.setMessage(message);
        final PersonIdent ident = new PersonIdent(userName, userEmailAddress);
        commitBuilder.setAuthor(ident);
        commitBuilder.setCommitter(ident);
        commitBuilder.setParentId(parent);
        return repository.getObjectDatabase().newInserter().insert(commitBuilder);
    }

    /**
     * Updates the branch to point to the new commit.
     *
     * @param branchName the branch to update
     * @param commitId   the commit to point the branch at
     * @return the Ref of the updated branch
     * @throws IOException if there was an error writing the branch
     */
    Ref writeHead(
            final String branchName,
            final ObjectId commitId
    ) throws IOException {
        final Path branchRefPath = repository
                .getDirectory()
                .toPath()
                .resolve(branchName)
                .toAbsolutePath();
        final byte[] commitIdBytes = commitId.name().getBytes(StandardCharsets.UTF_8);
        Files.write(branchRefPath, commitIdBytes);
        return repository.findRef(branchName);
    }

    /**
     * Reads a value from the branch with the given key.
     *
     * @param branchRef the branch to select from
     * @param key       the key to get the value for
     * @return an Optional containing the value if found, or empty
     * @throws IOException if there was an error reading the value
     */
    Optional<String> readValue(
            final Ref branchRef,
            final String key
    ) throws IOException {
        try (TreeWalk treeWalk = getTreeWalk(branchRef)) {
            treeWalk.setFilter(PathFilter.create(key));
            if (treeWalk.next()) {
                return Optional.of(new String(
                        repository.open(treeWalk.getObjectId(0), Constants.OBJ_BLOB).getBytes()));
            }
        }
        return Optional.empty();
    }

    private TreeFormatter treeFormatterForBranch(final Ref branchRef) throws IOException {
        final TreeFormatter treeFormatter = new TreeFormatter();
        try (TreeWalk treeWalk = getTreeWalk(branchRef)) {
            while (treeWalk.next()) {
                treeFormatter.append(
                        treeWalk.getNameString(),
                        new RevWalk(repository).lookupBlob(treeWalk.getObjectId(0)));
            }
        }
        return treeFormatter;
    }

    private TreeWalk getTreeWalk(final Ref branchRef) throws IOException {
        final TreeWalk treeWalk = new TreeWalk(repository);
        treeWalk.addTree(new RevWalk(repository).parseCommit(branchRef.getObjectId()).getTree());
        treeWalk.setRecursive(false);
        return treeWalk;
    }

}
