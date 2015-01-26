/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal.processors.cache;

import org.gridgain.grid.*;
import org.gridgain.grid.marshaller.optimized.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.common.*;

import java.util.concurrent.*;

/**
 *
 */
public class GridCacheVersionSelfTest extends GridCommonAbstractTest {
    /**
     * @throws Exception If failed.
     */
    public void testTopologyVersionDrId() throws Exception {
        GridCacheVersion ver = version(10, 0);

        assertEquals(10, ver.nodeOrder());
        assertEquals(0, ver.dataCenterId());

        // Check with max topology version and some dr IDs.
        ver = version(0x7FFFFFF, 0);
        assertEquals(0x7FFFFFF, ver.nodeOrder());
        assertEquals(0, ver.dataCenterId());

        ver = version(0x7FFFFFF, 15);
        assertEquals(0x7FFFFFF, ver.nodeOrder());
        assertEquals(15, ver.dataCenterId());

        ver = version(0x7FFFFFF, 31);
        assertEquals(0x7FFFFFF, ver.nodeOrder());
        assertEquals(31, ver.dataCenterId());

        // Check max dr ID with some topology versions.
        ver = version(11, 31);
        assertEquals(11, ver.nodeOrder());
        assertEquals(31, ver.dataCenterId());

        ver = version(256, 31);
        assertEquals(256, ver.nodeOrder());
        assertEquals(31, ver.dataCenterId());

        ver = version(1025, 31);
        assertEquals(1025, ver.nodeOrder());
        assertEquals(31, ver.dataCenterId());

        // Check overflow exception.
        GridTestUtils.assertThrows(log, new Callable<Object>() {
            @Override public Object call() throws Exception {
                return version(0x7FFFFFF + 1, 1);
            }
        }, IllegalArgumentException.class, null);
    }

    /**
     * Test versions marshalling.
     *
     * @throws GridException If failed.
     */
    public void testMarshalling() throws GridException {
        GridCacheVersion ver = version(1, 1);
        GridCacheVersionEx verEx = new GridCacheVersionEx(2, 2, 0, 0, ver);

        GridOptimizedMarshaller marsh = new GridOptimizedMarshaller(false);

        byte[] verBytes = marsh.marshal(ver);
        byte[] verExBytes = marsh.marshal(verEx);

        GridCacheVersion verNew = marsh.unmarshal(verBytes, Thread.currentThread().getContextClassLoader());
        GridCacheVersionEx verExNew = marsh.unmarshal(verExBytes, Thread.currentThread().getContextClassLoader());

        assert ver.equals(verNew);
        assert verEx.equals(verExNew);
    }

    /**
     * @param nodeOrder Node order.
     * @param drId Data center ID.
     * @return Cache version.
     */
    private GridCacheVersion version(int nodeOrder, int drId) {
        return new GridCacheVersion(0, 0, 0, nodeOrder, drId);
    }
}
