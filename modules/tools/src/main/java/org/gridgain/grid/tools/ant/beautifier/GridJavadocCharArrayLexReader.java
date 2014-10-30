/* 
 Copyright (C) GridGain Systems. All Rights Reserved.
 
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.tools.ant.beautifier;

/**
 * Character-based lexical token reader.
 */
class GridJavadocCharArrayLexReader {
    /** End Of File (EOF) constant. */
    public static final char EOF = (char)-1;

    /** Character-based buffer. */
    private char[] chars;

    /** Index in {@link #chars}. */
    private int index;

    /**
     * Creates reader with given buffer.
     *
     * @param chars Input character buffer.
     */
    GridJavadocCharArrayLexReader(char[] chars) {
        this.chars = chars;
    }

    /**
     * Gets length of the buffer.
     *
     * @return Length if the buffer.
     */
    int getLength() {
        return chars.length;
    }

    /**
     * Reads next character.
     *
     * @return Next character from the buffer.
     */
    int read() {
        return index == chars.length ? EOF : chars[index++];
    }

    /**
     * Peeks at the next character in the buffer.
     *
     * @return Next character that will be returned by next {@link #read()} apply.
     */
    int peek() {
        return index == chars.length ? EOF : chars[index];
    }

    /**
     * Skips next character in the buffer.
     */
    void skip() {
        if (index < chars.length)
            index++;
    }

    /**
     * Puts back last read character.
     */
    void back() {
        if (index > 0)
            index--;
    }

    /**
     * Tests whether buffer has more characters.
     *
     * @return {@code true} if buffer has at least one more character - {@code false} otherwise.
     */
    boolean hasMore() {
        return index < chars.length;
    }
}
