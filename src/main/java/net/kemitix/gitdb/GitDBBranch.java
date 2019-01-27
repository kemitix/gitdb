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
import net.kemitix.mon.maybe.Maybe;
import net.kemitix.mon.result.Result;

/**
 * API for interacting with a branch in a GirDB.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
public interface GitDBBranch {

    /**
     * The name of the branch.
     *
     * @return the branch name
     */
    Result<String> name();

    /**
     * Lookup a value for the key.
     *
     * @param key the key to lookup
     * @return an Optional containing the value, if it exists, or empty if not
     */
    Result<Maybe<String>> get(String key);

    /**
     * Put a value into the store for the key.
     *
     * @param key   the key to place the value under
     * @param value the value (must be Serializable)
     * @return an updated branch containing the new key/value
     */
    Result<GitDBBranch> put(String key, String value);

    /**
     * Removes a key and its value from the store.
     *
     * @param key the key to remove
     * @return an updated branch without the key, or the original if the key was not found
     */
    Result<GitDBBranch> remove(String key);

    /**
     * Returns the GitDB format for the current branch.
     *
     * <p>Different branches can have different versions.</p>
     *
     * @return the format as per semantic versioning, i.e. "x.y.z" within an Optional
     */
    Result<Maybe<Version>> getFormatVersion();

}
