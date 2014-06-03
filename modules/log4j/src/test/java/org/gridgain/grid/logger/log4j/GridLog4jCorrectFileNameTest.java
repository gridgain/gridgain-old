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

import junit.framework.*;
import org.apache.log4j.*;
import org.apache.log4j.varia.*;
import org.gridgain.grid.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.junits.common.*;

import java.io.*;
import java.util.*;

/**
 * Tests that several grids log to files with correct names.
 */
@GridCommonTest(group = "Logger")
public class GridLog4jCorrectFileNameTest extends TestCase {
    /** Appender */
    private GridLog4jRollingFileAppender appender;

    /** {@inheritDoc} */
    @Override protected void setUp() throws Exception {
        Logger root = Logger.getRootLogger();

        for (Enumeration appenders = root.getAllAppenders(); appenders.hasMoreElements(); ) {
            if (appenders.nextElement() instanceof GridLog4jRollingFileAppender)
                return;
        }

        appender = createAppender();

        root.addAppender(appender);
    }

    /** {@inheritDoc} */
    @Override public void tearDown() {
        if (appender != null) {
            Logger.getRootLogger().removeAppender(GridLog4jRollingFileAppender.class.getSimpleName());

            GridLog4jLogger.removeAppender(appender);
        }
    }

    /**
     * Tests correct behaviour in case 2 local nodes are started.
     *
     * @throws Exception If error occurs.
     */
    public void testLogFilesTwoNodes() throws Exception {
        checkOneNode(0);
        checkOneNode(1);
    }

    /**
     * Starts the local node and checks for presence of log file.
     * Also checks that this is really a log of a started node.
     *
     * @param id Test-local node ID.
     * @throws Exception If error occurred.
     */
    private void checkOneNode(int id) throws Exception {
        try (Grid grid = G.start(getConfiguration("grid" + id))) {
            String id8 = U.id8(grid.localNode().id());
            String logPath = "work/log/gridgain-" + id8 + ".log";
            File logFile = U.resolveGridGainPath(logPath);

            assertNotNull("Failed to resolve path: " + logPath, logFile);
            assertTrue("Log file does not exist: " + logFile, logFile.exists());

            String logContent = U.readFileToString(logFile.getAbsolutePath(), "UTF-8");

            assertTrue("Log file does not contain it's node ID: " + logFile,
                logContent.contains(">>> Local node [ID=" + id8.toUpperCase()));
        }
    }

    /**
     * Creates grid configuration.
     *
     * @param gridName Grid name.
     * @return Grid configuration.
     * @throws Exception If error occurred.
     */
    private static GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = new GridConfiguration();

        cfg.setGridName(gridName);
        cfg.setGridLogger(new GridLog4jLogger());
        cfg.setRestEnabled(false);

        return cfg;
    }

    /**
     * Creates new GridLog4jRollingFileAppender.
     *
     * @return GridLog4jRollingFileAppender.
     * @throws Exception If error occurred.
     */
    private static GridLog4jRollingFileAppender createAppender() throws Exception {
        GridLog4jRollingFileAppender appender = new GridLog4jRollingFileAppender();

        appender.setLayout(new PatternLayout("[%d{ABSOLUTE}][%-5p][%t][%c{1}] %m%n"));
        appender.setFile("work/log/gridgain.log");
        appender.setName(GridLog4jRollingFileAppender.class.getSimpleName());

        LevelRangeFilter lvlFilter = new LevelRangeFilter();

        lvlFilter.setLevelMin(Level.DEBUG);
        lvlFilter.setLevelMax(Level.INFO);

        appender.addFilter(lvlFilter);

        return appender;
    }
}

