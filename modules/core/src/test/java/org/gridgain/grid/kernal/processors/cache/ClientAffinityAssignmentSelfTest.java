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

package org.gridgain.grid.kernal.processors.cache;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.affinity.*;
import org.gridgain.grid.cache.affinity.consistenthash.*;
import org.gridgain.grid.cache.affinity.fair.*;
import org.gridgain.grid.cache.affinity.rendezvous.*;
import org.gridgain.testframework.junits.common.*;

import static org.gridgain.grid.cache.GridCacheDistributionMode.*;

/**
 * Tests affinity assignment for different affinity types.
 */
public class ClientAffinityAssignmentSelfTest extends GridCommonAbstractTest {
    /** */
    public static final int PARTS = 256;

    /** */
    private boolean client;

    /** */
    private boolean cache;

    /** */
    private int aff;

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        if (cache) {
            GridCacheConfiguration ccfg = new GridCacheConfiguration();

            ccfg.setCacheMode(GridCacheMode.PARTITIONED);
            ccfg.setBackups(1);
            ccfg.setAtomicityMode(GridCacheAtomicityMode.TRANSACTIONAL);
            ccfg.setDistributionMode(client ? CLIENT_ONLY : PARTITIONED_ONLY);

            if (aff == 0)
                ccfg.setAffinity(new GridCacheConsistentHashAffinityFunction(false, PARTS));
            else if (aff == 1)
                ccfg.setAffinity(new GridCacheRendezvousAffinityFunction(false, PARTS));
            else
                ccfg.setAffinity(new GridCachePartitionFairAffinity(PARTS));

            cfg.setCacheConfiguration(ccfg);
        }

        return cfg;
    }

    /**
     * @throws Exception If failed.
     */
    public void testConsistentHashAssignment() throws Exception {
        aff = 0;

        checkAffinityFunction();
    }

    /**
     * @throws Exception If failed.
     */
    public void testRendezvousAssignment() throws Exception {
        aff = 1;

        checkAffinityFunction();
    }

    /**
     * @throws Exception If failed.
     */
    public void testFairAssignment() throws Exception {
        aff = 2;

        checkAffinityFunction();
    }

    /**
     * @throws Exception If failed.
     */
    private void checkAffinityFunction() throws Exception {
        cache = true;

        startGrids(3);

        try {
            checkAffinity();

            client = true;

            startGrid(3);

            checkAffinity();

            startGrid(4);

            checkAffinity();

            cache = false;

            startGrid(5);

            checkAffinity();

            stopGrid(5);

            checkAffinity();

            stopGrid(4);

            checkAffinity();

            stopGrid(3);

            checkAffinity();
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @throws Exception If failed.
     */
    private void checkAffinity() throws Exception {
        GridCacheAffinity<Object> aff = grid(0).cache(null).affinity();

        for (Grid grid : GridGain.allGrids()) {
            try {
                if (grid.localNode().id().equals(grid(0).localNode().id()))
                    continue;

                GridCacheAffinity<Object> checkAff = grid.cache(null).affinity();

                for (int p = 0; p < PARTS; p++)
                    assertEquals(aff.mapPartitionToPrimaryAndBackups(p), checkAff.mapPartitionToPrimaryAndBackups(p));
            }
            catch (IllegalArgumentException ignored) {
                // Skip the node without cache.
            }
        }
    }
}
