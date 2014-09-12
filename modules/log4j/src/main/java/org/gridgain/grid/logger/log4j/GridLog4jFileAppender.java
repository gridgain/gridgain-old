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

package org.gridgain.grid.logger.log4j;

import org.apache.log4j.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.io.*;

/**
 * Log4J {@link FileAppender} with added support for grid node IDs.
 */
public class GridLog4jFileAppender extends FileAppender implements GridLog4jFileAware {
    /** Basic log file name. */
    private String baseFileName;

    /**
     * Default constructor (does not do anything).
     */
    public GridLog4jFileAppender() {
        init();
    }

    /**
     * Instantiate a FileAppender with given parameters.
     *
     * @param layout Layout.
     * @param filename File name.
     * @throws IOException If failed.
     */
    public GridLog4jFileAppender(Layout layout, String filename) throws IOException {
        super(layout, filename);

        init();
    }

    /**
     * Instantiate a FileAppender with given parameters.
     *
     * @param layout Layout.
     * @param filename File name.
     * @param append Append flag.
     * @throws IOException If failed.
     */
    public GridLog4jFileAppender(Layout layout, String filename, boolean append) throws IOException {
        super(layout, filename, append);

        init();
    }

    /**
     * Instantiate a FileAppender with given parameters.
     *
     * @param layout Layout.
     * @param filename File name.
     * @param append Append flag.
     * @param bufIO Buffered IO flag.
     * @param bufSize Buffer size.
     * @throws IOException If failed.
     */
    public GridLog4jFileAppender(Layout layout, String filename, boolean append, boolean bufIO, int bufSize)
        throws IOException {
        super(layout, filename, append, bufIO, bufSize);

        init();
    }

    /**
     *
     */
    private void init() {
        GridLog4jLogger.addAppender(this);
    }

    /** {@inheritDoc} */
    @Override public synchronized void setFile(String fileName, boolean fileAppend, boolean bufIO, int bufSize)
        throws IOException {
        if (baseFileName != null)
            super.setFile(fileName, fileAppend, bufIO, bufSize);
    }

    /** {@inheritDoc} */
    @Override public synchronized void updateFilePath(GridClosure<String, String> filePathClos) {
        A.notNull(filePathClos, "filePathClos");

        if (baseFileName == null)
            baseFileName = fileName;

        fileName = filePathClos.apply(baseFileName);
    }
}
