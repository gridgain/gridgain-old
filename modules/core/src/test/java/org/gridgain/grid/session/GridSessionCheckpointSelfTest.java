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

package org.gridgain.grid.session;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.spi.checkpoint.cache.*;
import org.gridgain.grid.spi.checkpoint.jdbc.*;
import org.gridgain.grid.spi.checkpoint.sharedfs.*;
import org.gridgain.testframework.junits.common.*;
import org.hsqldb.jdbc.*;

import java.io.*;
import java.util.*;

/**
 * Grid session checkpoint self test.
 */
@GridCommonTest(group = "Task Session")
public class GridSessionCheckpointSelfTest extends GridSessionCheckpointAbstractSelfTest {
    /**
     * @throws Exception If failed.
     */
    public void testSharedFsCheckpoint() throws Exception {
        GridConfiguration cfg = getConfiguration();

        cfg.setCheckpointSpi(spi = new GridSharedFsCheckpointSpi());

        checkCheckpoints(cfg);
    }

    /**
     * @throws Exception If failed.
     */
    public void testJdbcCheckpoint() throws Exception {
        GridConfiguration cfg = getConfiguration();

        jdbcDataSource ds = new jdbcDataSource();

        ds.setDatabase("jdbc:hsqldb:mem:gg_test");
        ds.setUser("sa");
        ds.setPassword("");

        GridJdbcCheckpointSpi spi = new GridJdbcCheckpointSpi();

        spi.setDataSource(ds);
        spi.setCheckpointTableName("checkpoints");
        spi.setKeyFieldName("key");
        spi.setValueFieldName("value");
        spi.setValueFieldType("longvarbinary");
        spi.setExpireDateFieldName("create_date");

        GridSessionCheckpointSelfTest.spi = spi;

        cfg.setCheckpointSpi(spi);

        checkCheckpoints(cfg);
    }

    /**
     * @throws Exception If failed.
     */
    public void testCacheCheckpoint() throws Exception {
        GridConfiguration cfg = getConfiguration();

        String cacheName = "test-checkpoints";

        GridCacheConfiguration cacheCfg = defaultCacheConfiguration();

        cacheCfg.setName(cacheName);

        GridCacheCheckpointSpi spi = new GridCacheCheckpointSpi();

        spi.setCacheName(cacheName);

        cfg.setCacheConfiguration(cacheCfg);

        cfg.setCheckpointSpi(spi);

        GridSessionCheckpointSelfTest.spi = spi;

        checkCheckpoints(cfg);
    }
}
