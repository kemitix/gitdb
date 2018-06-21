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

import java.nio.charset.StandardCharsets;

/**
 * Defines the version of the GitDB repository format.
 *
 * @author Paul Campbell (pcampbell@kemitix.net)
 */
public final class FormatVersion {

    private final int major = 1;
    private final int minor = 0;
    private final int patch = 0;

    /**
     * Formats the version as a UTF 8 byte array.
     *
     * @return a bytes array
     */
    public byte[] toBytes() {
        return getVersion().toString().getBytes(StandardCharsets.UTF_8);
    }

    public Version getVersion() {
        return Version.forIntegers(major, minor, patch);
    }
}
