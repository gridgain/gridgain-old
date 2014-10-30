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

package org.gridgain.grid.tools.javadoc;

import com.sun.tools.doclets.*;
import com.sun.javadoc.*;

import java.io.*;
import java.util.*;

/**
 * Represents {@gglink ...} tag. This tag can
 * be used as replacement of {@link ...} tag that references to the GridGain class that is not in classpath.
 * Class and its arguments should have fully qualified names.
 */
public class GridLinkTaglet implements Taglet {
    /** */
    private static final String NAME = "gglink";

    /**
     * Return the name of this custom tag.
     */
    public String getName() {
        return NAME;
    }

    /**
     * @return true since this tag can be used in a field doc comment.
     */
    public boolean inField() {
        return true;
    }

    /**
     * @return true since this tag can be used in a constructor doc comment.
     */
    public boolean inConstructor() {
        return true;
    }

    /**
     * @return true since this tag can be used in a method doc comment.
     */
    public boolean inMethod() {
        return true;
    }

    /**
     * @return true since this tag can be used in an overview doc comment.
     */
    public boolean inOverview() {
        return true;
    }

    /**
     * @return true since this tag can be used in a package doc comment.
     */
    public boolean inPackage() {
        return true;
    }

    /**
     * @return true since this.
     */
    public boolean inType() {
        return true;
    }

    /**
     * Will return true since this is an inline tag.
     *
     * @return true since this is an inline tag.
     */
    public boolean isInlineTag() {
        return true;
    }

    /**
     * Register this Taglet.
     *
     * @param tagletMap the map to register this tag to.
     */
    public static void register(Map<String, GridLinkTaglet> tagletMap) {
        GridLinkTaglet tag = new GridLinkTaglet();

        Taglet t = tagletMap.get(tag.getName());

        if (t != null)
            tagletMap.remove(tag.getName());

        tagletMap.put(tag.getName(), tag);
    }

    /**
     * Given the <code>Tag</code> representation of this custom tag, return its string representation.
     * <p>
     * Input: org.gridgain.grid.spi.indexing.h2.GridH2IndexingSpi#setIndexCustomFunctionClasses(Class[])
     * <p>
     * Output: <a href="../../../../../org/gridgain/grid/spi/indexing/h2/GridH2IndexingSpi.html#
     * setIndexCustomFunctionClasses(java.lang.Class...)">
     * <code>GridH2IndexingSpi.setIndexCustomFunctionClasses(java.lang.Class[])</code></a>
     *
     * @param tag <code>Tag</code> representation of this custom tag.
     */
    public String toString(Tag tag) {
        if (tag.text() == null || tag.text().isEmpty())
            return "";

        File f = tag.position().file();

        String curClass = f == null ? "" : f.getAbsolutePath().replace(File.separator, ".");

        String packPref = "src.main.java.";

        int idx = curClass.indexOf(packPref);

        StringBuilder path = new StringBuilder();

        if (idx != -1) {
            curClass = curClass.substring(idx + packPref.length());

            for (int i = 0, n = curClass.split("\\.").length - 2; i < n; i++)
                path.append("../");
        }

        String[] tokens = tag.text().split("#");

        int lastIdx = tokens[0].lastIndexOf('.');

        String simpleClsName = lastIdx != -1 && lastIdx + 1 < tokens[0].length() ?
            tokens[0].substring(lastIdx + 1) : tokens[0];

        String fullyQClsName = tokens[0].replace(".", "/");

        return "<a href=\"" + path.toString() + fullyQClsName + ".html" +
            (tokens.length > 1 ? ("#" + tokens[1].replace("[]", "...")) : "") +
            "\"><code>" + simpleClsName + (tokens.length > 1 ? ("." + tokens[1]) : "") + "</code></a>";
    }

    /**
     * This method should not be called since arrays of inline tags do not
     * exist.  Method {@link #toString(Tag)} should be used to convert this
     * inline tag to a string.
     *
     * @param tags the array of <code>Tag</code>s representing of this custom tag.
     */
    public String toString(Tag[] tags) {
        return null;
    }
}
