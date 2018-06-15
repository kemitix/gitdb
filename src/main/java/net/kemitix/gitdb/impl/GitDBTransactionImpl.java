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

import com.github.zafarkhaja.semver.Version;
import net.kemitix.gitdb.GitDBBranch;
import net.kemitix.gitdb.GitDBTransaction;

import java.io.IOException;
import java.util.Optional;

/**
 * An anonymous transaction.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
class GitDBTransactionImpl implements GitDBTransaction {

    private final GitDBBranch base;
    private final GitDBBranch branch;

    GitDBTransactionImpl(final GitDBBranch base, final GitDBBranch branch) {
        this.base = base;
        this.branch = branch;
    }

    @Override
    public Optional<String> get(String key) throws IOException {
        return branch.get(key);
    }

    @Override
    public GitDBBranch put(String key, String value) throws IOException {
        return branch.put(key, value);
    }

    @Override
    public GitDBBranch remove(String key) throws IOException {
        return branch.remove(key);
    }

    @Override
    public Optional<Version> getFormatVersion() throws IOException {
        return branch.getFormatVersion();
    }

    @Override
    public GitDBTransaction transaction() {
        return null;
    }

    @Override
    public GitDBTransaction transaction(String name) {
        return null;
    }

    @Override
    public String name() {
        return branch.name();
    }

    @Override
    public String getCommitId() {
        return branch.getCommitId();
    }
}
