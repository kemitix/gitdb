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

import com.github.zafarkhaja.semver.Version;

import java.io.IOException;
import java.util.Optional;

/**
 * API for interacting with a branch in a GirDB.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
public interface GitDBBranch {

    /**
     * Lookup a value for the key.
     *
     * @param key the key to lookup
     * @return an Optional containing the value, if it exists, or empty if not
     * @throws IOException if there was an error reading the value
     */
    Optional<String> get(String key) throws IOException;

    /**
     * Put a value into the store for the key.
     *
     * @param key   the key to place the value under
     * @param value the value (must be Serializable)
     * @return an updated branch containing the new key/value
     * @throws IOException if there was an error writing the key/value
     */
    GitDBBranch put(String key, String value) throws IOException;

    /**
     * Removes a key and its value from the store.
     *
     * @param key the key to remove
     * @return an updated branch without the key, or the original if the key was not found
     * @throws IOException if there was an error removing the key/value
     */
    GitDBBranch remove(String key) throws IOException;

    /**
     * Returns the GitDB format for the current branch.
     *
     * <p>Different branches can have different versions.</p>
     *
     * @return the format as per semantic versioning, i.e. "x.y.z" within an Optional
     * @throws IOException error reading version
     */
    Optional<Version> getFormatVersion() throws IOException;

    /**
     * Begins a new anonymous transaction.
     *
     * @return a new Transaction
     * @throws IOException error writing transaction branch
     */
    GitDBTransaction transaction() throws IOException;

    /**
     * Begins a new named transaction.
     *
     * @param transactionName the transaction name
     * @return a new Transaction
     * @throws IOException error writing transaction branch
     */
    GitDBTransaction transaction(String transactionName) throws IOException;

    /**
     * Gets the name of the branch.
     *
     * @return the branch name
     */
    String name();

    /**
     * Gets the commit id for the head of the branch.
     *
     * @return an Object Id.
     */
    String getCommitId();
}
