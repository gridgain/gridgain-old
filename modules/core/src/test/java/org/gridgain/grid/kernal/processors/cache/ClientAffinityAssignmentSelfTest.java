/* @java.file.header */

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
    private int aff;

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

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
        startGrids(3);

        try {
            checkAffinity();

            client = true;

            startGrid(3);

            checkAffinity();

            startGrid(4);

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
            if (grid.localNode().id().equals(grid(0).localNode().id()))
                continue;

            GridCacheAffinity<Object> checkAff = grid.cache(null).affinity();

            for (int p = 0; p < PARTS; p++)
                assertEquals(aff.mapPartitionToPrimaryAndBackups(p), checkAff.mapPartitionToPrimaryAndBackups(p));
        }
    }
}
