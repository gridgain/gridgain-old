/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal.processors.hadoop;

import org.gridgain.grid.*;
import org.gridgain.testframework.*;

import java.util.concurrent.*;

/**
 * Configuration validation tests.
 */
public class GridHadoopValidationSelfTest extends GridHadoopAbstractSelfTest {
    /** Peer class loading enabled flag. */
    public boolean peerClassLoading;

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids(true);

        peerClassLoading = false;
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        cfg.setPeerClassLoadingEnabled(peerClassLoading);

        return cfg;
    }

    /**
     * Ensure that Grid starts when all configuration parameters are valid.
     *
     * @throws Exception If failed.
     */
    public void testValid() throws Exception {
        startGrids(1);
    }

    /**
     * Ensure that Grid cannot start when peer class loading is enabled.
     *
     * @throws Exception If failed.
     */
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void testInvalidPeerDeploymentEnabled() throws Exception {
        peerClassLoading = true;

        GridTestUtils.assertThrows(log, new Callable<Object>() {
            @Override public Object call() throws Exception {
                startGrids(1);

                return null;
            }
        }, GridException.class, null);
    }
}