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

package org.gridgain.grid.kernal.processors.cache.distributed.dht;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.junits.common.*;

import java.io.*;

/**
 *
 */
public class GridCacheDhtMultiBackupTest extends GridCommonAbstractTest {
    /**
     *
     */
    public GridCacheDhtMultiBackupTest() {
        super(false /* don't start grid. */);
    }

    /**
     * @throws Exception If failed
     */
    public void testPut() throws Exception {
        try {
            Grid g = G.start("examples/config/example-cache.xml");

            if (g.nodes().size() < 5)
                U.warn(log, "Topology is too small for this test. " +
                    "Run with 4 remote nodes or more having large number of backup nodes.");

            g.compute().run(new CAX() {
                    @GridInstanceResource
                    private Grid g;

                    @Override public void applyx() throws GridException {
                        X.println("Checking whether cache is empty.");

                        GridCache<SampleKey, SampleValue> cache = g.cache("partitioned");

                        assert cache.isEmpty();
                    }
                }
            ).get();

            GridCache<SampleKey, SampleValue> cache = g.cache("partitioned");

            int cnt = 0;

            for (int key = 0; key < 1000; key++) {
                SampleKey key1 = new SampleKey(key);

                if (!g.localNode().id().equals(g.mapKeyToNode("partitioned", key1))) {
                    cache.put(key1, new SampleValue(key));

                    cnt++;
                }
            }

            X.println(">>> Put count: " + cnt);
        }
        finally {
            G.stopAll(false);
        }
    }

    /**
     *
     */
    private static class SampleKey implements Serializable {
        /** */
        private int key;

        /**
         * @param key
         */
        private SampleKey(int key) {
            this.key = key;
        }

        /** {@inheritDoc} */
        @Override public int hashCode() {
            return key;
        }

        /** {@inheritDoc} */
        @Override public boolean equals(Object obj) {
            return obj instanceof SampleKey && ((SampleKey)obj).key == key;
        }
    }

    /**
     *
     */
    private static class SampleValue implements Serializable {
        /** */
        private int val;

        /**
         * @param val
         */
        private SampleValue(int val) {
            this.val = val;
        }

        /** {@inheritDoc} */
        @Override public int hashCode() {
            return val;
        }

        /** {@inheritDoc} */
        @Override public boolean equals(Object obj) {
            return obj instanceof SampleValue && ((SampleValue)obj).val == val;
        }
    }
}
