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

package org.gridgain.grid.util;

import junit.framework.*;
import org.gridgain.grid.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.logger.java.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.io.*;
import java.util.*;

import static org.gridgain.grid.GridSystemProperties.*;
import static org.gridgain.grid.util.GridUtils.*;

/**
 * Checks creation of work folder.
 */
public class GridStartupWithSpecifiedWorkDirectorySelfTest extends TestCase {
    /** */
    private static final GridTcpDiscoveryIpFinder IP_FINDER = new GridTcpDiscoveryVmIpFinder(true);

    /** */
    private static final int GRID_COUNT = 2;

    /** System temp directory. */
    private static final String TMP_DIR = System.getProperty("java.io.tmpdir");

    /** {@inheritDoc} */
    @Override protected void setUp() throws Exception {
        // Protection against previously cached values.
        nullifyHomeDirectory();
        nullifyWorkDirectory();
    }

    /** {@inheritDoc} */
    @Override protected void tearDown() throws Exception {
        // Next grid in the same VM shouldn't use cached values produced by these tests.
        nullifyHomeDirectory();
        nullifyWorkDirectory();

        U.setWorkDirectory(null, U.getGridGainHome());
    }

    /**
     * @param log Grid logger.
     * @return Grid configuration.
     */
    private GridConfiguration getConfiguration(GridLogger log) {
        // We can't use U.getGridGainHome() here because
        // it will initialize cached value which is forbidden to override.
        String ggHome = GridSystemProperties.getString(GG_HOME);

        assert ggHome != null;

        U.setGridGainHome(null);

        String ggHome0 = U.getGridGainHome();

        assert ggHome0 == null;

        GridTcpDiscoverySpi disc = new GridTcpDiscoverySpi();

        disc.setIpFinder(IP_FINDER);

        GridConfiguration cfg = new GridConfiguration();

        cfg.setGridLogger(log);
        cfg.setDiscoverySpi(disc);

        return cfg;
    }

    /**
     * @throws Exception If failed.
     */
    public void testStartStopWithUndefinedHomeAndWorkDirs() throws Exception {
        GridLogger log = new GridJavaLogger();

        log.info(">>> Test started: " + getName());
        log.info("Grid start-stop test count: " + GRID_COUNT);

        File testWorkDir = null;

        try {
            for (int i = 0; i < GRID_COUNT; i++) {
                try (Grid g = G.start(getConfiguration(log))) {
                    assert g != null;

                    testWorkDir = U.resolveWorkDirectory(getName(), true);

                    assertTrue("Work directory wasn't created", testWorkDir.exists());

                    assertTrue("Work directory must be located in OS temp directory",
                        testWorkDir.getAbsolutePath().startsWith(TMP_DIR));

                    System.out.println(testWorkDir);

                    X.println("Stopping grid " + g.localNode().id());
                }
            }
        }
        finally {
            if (testWorkDir != null && testWorkDir.getAbsolutePath().startsWith(TMP_DIR))
                U.delete(testWorkDir);
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testStartStopWithUndefinedHomeAndConfiguredWorkDirs() throws Exception {
        GridLogger log = new GridJavaLogger();

        log.info(">>> Test started: " + getName());
        log.info("Grid start-stop test count: " + GRID_COUNT);

        String tmpWorkDir = new File(TMP_DIR, getName() + "_" + UUID.randomUUID()).getAbsolutePath();

        try {
            for (int i = 0; i < GRID_COUNT; i++) {
                GridConfiguration cfg = getConfiguration(log);

                cfg.setWorkDirectory(tmpWorkDir);

                try (Grid g = G.start(cfg)) {
                    assert g != null;

                    File testWorkDir = U.resolveWorkDirectory(getName(), true);

                    assertTrue("Work directory wasn't created", testWorkDir.exists());

                    assertTrue("Work directory must be located in configured directory",
                        testWorkDir.getAbsolutePath().startsWith(tmpWorkDir));

                    X.println("Stopping grid " + g.localNode().id());
                }
            }
        } finally {
            U.delete(new File(tmpWorkDir));
        }
    }
}
