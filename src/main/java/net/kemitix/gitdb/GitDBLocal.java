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

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;

/**
 * Implementation of GitDB for working with a local Repo.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */

class GitDBLocal implements GitDB {

    private final Git git;

    /**
     * Constructors a new instance of this class.
     *
     * @param initCommand a JGit InitCommand
     * @param dbDir       the path to instantiate the git repo in
     * @throws IOException if there {@code dbDir} is a file or a non-empty directory
     */
    @SuppressWarnings("avoidhidingcauseexception")
    GitDBLocal(final InitCommand initCommand, final File dbDir) throws IOException {
        validateDbDir(dbDir);
        try {
            this.git = initRepo(initCommand, dbDir);
        } catch (GitAPIException e) {
            throw new UnexpectedGitDbException("Unhandled Git API Exception", e);
        }
    }

    private void validateDbDir(final File dbDir) throws IOException {
        verifyIsNotAFile(dbDir);
        if (dbDir.exists()) {
            verifyIsEmpty(dbDir);
        }
    }

    private static void verifyIsEmpty(final File dbDir) throws IOException {
        if (Files.newDirectoryStream(dbDir.toPath()).iterator().hasNext()) {
            throw new DirectoryNotEmptyException(dbDir.toString());
        }
    }

    private static void verifyIsNotAFile(final File dbDir) throws NotDirectoryException {
        if (dbDir.isFile()) {
            throw new NotDirectoryException(dbDir.toString());
        }
    }

    private static Git initRepo(final InitCommand initCommand, final File dbDir) throws GitAPIException {
        return initCommand.setGitDir(dbDir).setBare(true).call();
    }

    //    @Override
    //    @SneakyThrows
    //    public <T> T get(Branch branch, Key key, Class<T> type) {
    //        //branch
    //        final RefDatabase refDatabase = repository.getRefDatabase();
    //        final String branchValue = branch.getValue();
    //        final Ref refDatabaseRef = refDatabase.getRef(branchValue);
    //        final ObjectId commitId = refDatabaseRef.getObjectId();
    //
    //        final RevCommit revCommit = repository.parseCommit(commitId);
    //        final RevTree tree = revCommit.getTree();
    //        tree.copyTo(System.out);
    //
    //        final ObjectLoader open = repository.getObjectDatabase().open(objectId, Constants.OBJ_TREE);
    //        final byte[] bytes = open.getBytes();
    //        final String s = new String(bytes);
    //        System.out.println("s = " + s);
    //        //key
    //        return null;
    //    }

    //    @Override
    //    @SneakyThrows
    //    public String put(Branch branch, Message message, Document<String> document, Author author) {
    ////        return document.getValue();
    //
    //        final ObjectInserter objectInserter = repository.newObjectInserter();
    //        final ObjectReader objectReader = repository.newObjectReader();
    //        final RevWalk revWalk = new RevWalk(repository);
    //
    //        //blob
    //        System.out.println("document = " + document.getKey());
    //        final ObjectId blobId = objectInserter.insert(Constants.OBJ_BLOB, document.getValue().getBytes(UTF_8));
    //        //tree
    //        final TreeFormatter treeFormatter = new TreeFormatter();
    //        treeFormatter.append(document.getKey().getValue(), FileMode.REGULAR_FILE, blobId);
    //        final ObjectId treeId = objectInserter.insert(treeFormatter);
    //        //commit
    //        final CommitBuilder commitBuilder = new CommitBuilder();
    //        final PersonIdent ident = new PersonIdent(author.getName(), author.getEmail());
    //        commitBuilder.setCommitter(ident);
    //        commitBuilder.setAuthor(ident);
    //        commitBuilder.setTreeId(treeId);
    //        commitBuilder.setMessage(message.getValue());
    //        //TODO: setParentId()
    //        final ObjectId commitId = objectInserter.insert(commitBuilder);
    //        //branch
    //        final RevCommit revCommit = revWalk.parseCommit(commitId);
    //        revCommit.getShortMessage();
    //        git.branchCreate()
    //                .setStartPoint(revCommit)
    //                .setName(branch.getValue())
    //                .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.NOTRACK)
    //                .call();
    //
    //        //READ
    //
    //        //block
    //        final String readBlob = new String(objectReader.open(blobId).getBytes());
    //        System.out.println("readBlob = " + readBlob);
    //        final RevBlob revBlob = revWalk.lookupBlob(blobId);
    //        System.out.println("revBlob  = " + revBlob);
    //        final String blobName = revBlob.name();
    //        System.out.println("blobName = " + blobName);
    //        //tree
    //        final RevTree revTree = revWalk.lookupTree(treeId);
    //        System.out.println("revTree  = " + revTree);
    //        final String treeName = revTree.name();
    //        System.out.println("treeName = " + treeName);
    //        //commit
    //        System.out.println("revCommit= " + revCommit);
    //        final String commitName = revCommit.getName();
    //        System.out.println("commitName= " + commitName);
    //        //branch
    //        final Ref branchRef = repository.getRefDatabase().getRef(branch.getValue());
    //        System.out.println("branchRef = " + branchRef.getName());
    //
    ////        final TreeWalk treeWalk = new TreeWalk(repository);
    ////        treeWalk.addTree(treeId);
    ////        treeWalk.next();
    ////        final String nameString = treeWalk.getNameString();
    ////        System.out.println("name     = " + nameString);
    ////        final ObjectId objectId = treeWalk.getObjectId(0);
    ////        System.out.println("objectId = " + objectId);
    //
    ////        final ObjectLoader openTree = repository.newObjectReader().open(treeId);
    ////        final int type = openTree.openStream().getType();
    ////        final long size = openTree.openStream().getSize();
    ////        final String readTree = new String(openTree.getBytes());
    //
    ////
    ////        //commit
    ////        final CommitBuilder commitBuilder = new CommitBuilder();
    ////        commitBuilder.setAuthor(new PersonIdent(author.getName(), author.getEmail()));
    ////        commitBuilder.setCommitter(new PersonIdent(author.getName(), author.getEmail()));
    ////        commitBuilder.setMessage(message.getValue());
    ////        findParentCommit(branch)
    ////                .ifPresent(commitBuilder::setParentId);
    ////        commitBuilder.setTreeId(treeId);
    ////        final ObjectId commitId = repository.newObjectInserter().insert(commitBuilder);
    ////
    ////        //branch
    ////        repository.updateRef(branch.getValue()).setNewObjectId(commitId);
    ////
    ////        //get
    ////        return get(branch, document.getKey());
    //        return document.getValue();
    //    }

}
