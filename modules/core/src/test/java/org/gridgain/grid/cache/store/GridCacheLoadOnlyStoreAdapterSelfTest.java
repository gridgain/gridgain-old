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

package org.gridgain.grid.cache.store;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.util.typedef.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 *
 */
public class GridCacheLoadOnlyStoreAdapterSelfTest extends GridCacheAbstractSelfTest {
    /** Expected loadAll arguments, hardcoded on call site for convenience. */
    private static final Integer[] EXP_ARGS = {1, 2, 3};

    /** Test input size. */
    private static final int INPUT_SIZE = 100;

    /** {@inheritDoc} */
    @Override protected int gridCount() {
        return 1;
    }

    /** {@inheritDoc} */
    @Override protected GridCacheConfiguration cacheConfiguration(String gridName) throws Exception {
        GridCacheConfiguration cfg = super.cacheConfiguration(gridName);

        cfg.setStore(new TestStore());

        return cfg;
    }

    /**
     * @throws Exception If failed.
     */
    public void testStore() throws Exception {
        cache().loadCache(null, 0, 1, 2, 3);

        int cnt = 0;

        for (int i = 0; i < gridCount(); i++)
            cnt += cache(i).size();

        assertEquals(INPUT_SIZE - (INPUT_SIZE/10), cnt);
    }

    private static class TestStore extends GridCacheLoadOnlyStoreAdapter<Integer, String, String> {
        /** {@inheritDoc} */
        @Override protected Iterator<String> inputIterator(@Nullable Object... args)
            throws GridException {
            assertNotNull(args);
            assertTrue(Arrays.equals(EXP_ARGS, args));

            return new Iterator<String>() {
                private int i = -1;

                @Override public boolean hasNext() {
                    return i < INPUT_SIZE;
                }

                @Override public String next() {
                    if (!hasNext())
                        throw new NoSuchElementException();

                    i++;

                    return i + "=str" + i;
                }

                @Override public void remove() {
                    // No-op.
                }
            };
        }

        /** {@inheritDoc} */
        @Override protected GridBiTuple<Integer, String> parse(String rec, @Nullable Object... args) {
            assertNotNull(args);
            assertTrue(Arrays.equals(EXP_ARGS, args));

            String[] p = rec.split("=");

            int i = Integer.parseInt(p[0]);

            return i % 10 == 0 ? null : new T2<>(i, p[1]);
        }
    }
}
