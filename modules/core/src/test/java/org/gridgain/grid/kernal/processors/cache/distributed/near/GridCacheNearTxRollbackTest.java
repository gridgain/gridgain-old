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

package org.gridgain.grid.kernal.processors.cache.distributed.near;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.kernal.managers.communication.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.communication.tcp.*;
import org.gridgain.grid.util.direct.*;
import org.gridgain.grid.util.typedef.*;

import java.util.*;

import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheDistributionMode.*;
import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheTxConcurrency.*;
import static org.gridgain.grid.cache.GridCacheTxIsolation.*;

/**
 *
 */
public class GridCacheNearTxRollbackTest extends GridCacheAbstractSelfTest {
    /** {@inheritDoc} */
    @Override protected int gridCount() {
        return 3;
    }

    /** {@inheritDoc} */
    @Override protected GridCacheMode cacheMode() {
        return PARTITIONED;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("RedundantMethodOverride")
    @Override protected GridCacheAtomicityMode atomicityMode() {
        return TRANSACTIONAL;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("RedundantMethodOverride")
    @Override protected GridCacheDistributionMode distributionMode() {
        return NEAR_PARTITIONED;
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        cfg.setCommunicationSpi(new TestCommunicationSpi());

        return cfg;
    }

    /**
     * @throws Exception If failed.
     */
    public void testPutAllRollback() throws Exception {
        GridCache<Integer, Integer> cache = grid(0).cache(null);

        Map<Integer, Integer> map = new LinkedHashMap<>();

        map.put(nearKey(cache), 1);
        map.put(primaryKey(cache), 1);

        TestCommunicationSpi spi = (TestCommunicationSpi)grid(0).configuration().getCommunicationSpi();

        spi.sndFail = true;

        try {
            try {
                try (GridCacheTx tx = cache.txStart(OPTIMISTIC, REPEATABLE_READ)) {
                    cache.putAll(map);

                    tx.commit();
                }

                fail("Put should fail.");
            }
            catch (GridException e) {
                log.info("Expected exception: " + e);

                assertFalse(X.hasCause(e, AssertionError.class));
            }

            for (int i = 0; i < gridCount(); i++) {
                for (Integer key : map.keySet()) {
                    GridCache<Integer, Integer> cache0 = grid(i).cache(null);

                    assertNull(cache0.peek(key));
                }
            }

            spi.sndFail = false;

            cache.putAll(map);

            for (Map.Entry<Integer, Integer> e : map.entrySet())
                assertEquals(e.getValue(), cache.get(e.getKey()));
        }
        finally {
            spi.sndFail = false;
        }
    }

    /**
     *
     */
    private static class TestCommunicationSpi extends GridTcpCommunicationSpi {
        /** */
        private volatile boolean sndFail;

        /** {@inheritDoc} */
        @Override public void sendMessage(GridNode node, GridTcpCommunicationMessageAdapter msg)
            throws GridSpiException
        {
            if (msg instanceof GridIoMessage) {
                Object msg0 = ((GridIoMessage)msg).message();

                if (sndFail && msg0 instanceof GridNearTxPrepareRequest)
                    throw new GridSpiException("Test error");
            }

            super.sendMessage(node, msg);
        }
    }
}
