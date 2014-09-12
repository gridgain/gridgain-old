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

package org.gridgain.testframework.junits.common;

import org.gridgain.grid.*;
import org.gridgain.grid.resources.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;
import java.util.concurrent.atomic.*;

/**
 * Base class for tests against {@link org.gridgain.grid.GridLifecycleAware} support.
 */
public abstract class GridAbstractLifecycleAwareSelfTest extends GridCommonAbstractTest {
    /** */
    protected Collection<TestLifecycleAware> lifecycleAwares = new ArrayList<>();

    /**
     */
    @SuppressWarnings("PublicInnerClass")
    public static class TestLifecycleAware implements GridLifecycleAware {
        /** */
        private AtomicInteger startCnt = new AtomicInteger();

        /** */
        private AtomicInteger stopCnt = new AtomicInteger();

        /** */
        @GridCacheNameResource
        private String cacheName;

        /** */
        private final String expCacheName;

        /**
         * @param expCacheName Expected injected cache name.
         */
        public TestLifecycleAware(String expCacheName) {
            this.expCacheName = expCacheName;
        }

        /** {@inheritDoc} */
        @Override public void start() {
            startCnt.incrementAndGet();

            assertEquals(expCacheName, cacheName);
        }

        /** {@inheritDoc} */
        @Override public void stop() {
            stopCnt.incrementAndGet();
        }

        /**
         * @return Number of times {@link GridLifecycleAware#start} was called.
         */
        public int startCount() {
            return startCnt.get();
        }

        /**
         * @return Number of times {@link GridLifecycleAware#stop} was called.
         */
        public int stopCount() {
            return stopCnt.get();
        }
    }

    /**
     * After grid start callback.
     * @param grid Grid.
     */
    protected void afterGridStart(Grid grid) {
        // No-op.
    }

    /**
     * @throws Exception If failed.
     */
    public void testLifecycleAware() throws Exception {
        Grid grid = startGrid();

        afterGridStart(grid);

        assertFalse(lifecycleAwares.isEmpty());

        for (TestLifecycleAware lifecycleAware : lifecycleAwares) {
            assertEquals("Unexpected start count for " + lifecycleAware, 1, lifecycleAware.startCount());
            assertEquals("Unexpected stop count for " + lifecycleAware, 0, lifecycleAware.stopCount());
        }

        try {
            stopGrid();

            for (TestLifecycleAware lifecycleAware : lifecycleAwares) {
                assertEquals("Unexpected start count for " + lifecycleAware, 1, lifecycleAware.startCount());
                assertEquals("Unexpected stop count for " + lifecycleAware, 1, lifecycleAware.stopCount());
            }
        }
        finally {
            lifecycleAwares.clear();
        }
    }
}
