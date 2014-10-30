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
import org.gridgain.grid.lang.*;
import org.gridgain.grid.portables.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.*;

import java.util.*;
import java.util.concurrent.*;

import static org.gridgain.grid.cache.GridCacheAtomicWriteOrderMode.*;
import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheMemoryMode.*;

/**
 * Tests that offheap entry is not evicted while cache entry is in use.
 */
public abstract class GridCacheOffHeapTieredEvictionAbstractSelfTest extends GridCacheAbstractSelfTest {
    /** */
    private static final int VALS = 100;

    /** */
    private static final int VAL_SIZE = 128;

    /** */
    private static final int KEYS = 100;

    /** */
    private List<TestValue> vals = new ArrayList<>(VALS);

    /** {@inheritDoc} */
    @Override protected int gridCount() {
        return 1;
    }

    /** {@inheritDoc} */
    @Override protected long getTestTimeout() {
        return 60 * 1000;
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        if (portableEnabled()) {
            GridPortableConfiguration pCfg = new GridPortableConfiguration();

            pCfg.setClassNames(Arrays.asList(TestValue.class.getName()));

            cfg.setPortableConfiguration(pCfg);
        }

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected GridCacheConfiguration cacheConfiguration(String gridName) throws Exception {
        GridCacheConfiguration ccfg = super.cacheConfiguration(gridName);

        ccfg.setAtomicWriteOrderMode(PRIMARY);

        ccfg.setMemoryMode(OFFHEAP_TIERED);
        ccfg.setOffHeapMaxMemory(0);

        return ccfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        super.beforeTest();

        final GridCache<Integer, Object> cache = grid(0).cache(null);

        vals = new ArrayList<>(VALS);

        for (int i = 0; i < VALS; i++) {
            SB sb = new SB(VAL_SIZE);

            char c = Character.forDigit(i, 10);

            for (int j = 0; j < VAL_SIZE; j++)
                sb.a(c);

            vals.add(new TestValue(sb.toString()));
        }

        for (int i = 0; i < KEYS; i++)
            cache.put(i, vals.get(i % vals.size()));
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        super.afterTest();

        vals = null;
    }

    /**
     * @return Number of iterations per thread.
     */
    private int iterations() {
        return atomicityMode() == ATOMIC ? 100_000 : 50_000;
    }

    /**
     * @throws Exception If failed.
     */
    public void testPut() throws Exception {
        final GridCache<Integer, Object> cache = grid(0).cache(null);

        GridTestUtils.runMultiThreaded(new Callable<Void>() {
            @Override public Void call() throws Exception {
                ThreadLocalRandom rnd = ThreadLocalRandom.current();

                for (int i = 0; i < iterations(); i++) {
                    int key = rnd.nextInt(KEYS);

                    final TestValue val = vals.get(key % VAL_SIZE);

                    TestPredicate p = testPredicate(val.val, false);

                    cache.putx(key, val, p);
                }

                return null;
            }
        }, 16, "test");
    }

    /**
     * @throws Exception If failed.
     */
    public void testRemove() throws Exception {
        final GridCache<Integer, Object> cache = grid(0).cache(null);

        GridTestUtils.runMultiThreaded(new Callable<Void>() {
            @Override public Void call() throws Exception {
                ThreadLocalRandom rnd = ThreadLocalRandom.current();

                for (int i = 0; i < iterations(); i++) {
                    int key = rnd.nextInt(KEYS);

                    final TestValue val = vals.get(key % VAL_SIZE);

                    TestPredicate p = testPredicate(val.val, true);

                    if (rnd.nextBoolean())
                        cache.removex(key, p);
                    else
                        cache.putx(key, val, p);
                }

                return null;
            }
        }, 16, "test");
    }

    /**
     * @throws Exception If failed.
     */
    public void testTransform() throws Exception {
        final GridCache<Integer, Object> cache = grid(0).cache(null);

        GridTestUtils.runMultiThreaded(new Callable<Void>() {
            @Override public Void call() throws Exception {
                ThreadLocalRandom rnd = ThreadLocalRandom.current();

                for (int i = 0; i < iterations(); i++) {
                    int key = rnd.nextInt(KEYS);

                    final TestValue val = vals.get(key % VAL_SIZE);

                    TestClosure c = testClosure(val.val, false);

                    cache.transform(key, c);
                }

                return null;
            }
        }, 16, "test");
    }

    /**
     * @param expVal Expected cache value.
     * @param acceptNull If {@code true} value can be null;
     * @return Predicate.
     */
    private TestPredicate testPredicate(String expVal, boolean acceptNull) {
        return portableEnabled() ?
            new PortableValuePredicate(expVal, acceptNull) :
            new TestValuePredicate(expVal, acceptNull);
    }

    /**
     * @param expVal Expected cache value.
     * @param acceptNull If {@code true} value can be null;
     * @return Predicate.
     */
    private TestClosure testClosure(String expVal, boolean acceptNull) {
        return portableEnabled() ?
            new PortableValueClosure(expVal, acceptNull) :
            new TestValueClosure(expVal, acceptNull);
    }

    /**
     *
     */
    @SuppressWarnings("PublicInnerClass")
    public static class TestValue {
        /** */
        @SuppressWarnings("PublicField")
        public String val;

        /**
         *
         */
        public TestValue() {
            // No-op.
        }

        /**
         * @param val Value.
         */
        public TestValue(String val) {
            this.val = val;
        }
    }

    /**
     *
     */
    protected abstract static class TestPredicate implements P1<GridCacheEntry<Integer, Object>> {
        /** */
        protected String expVal;

        /** */
        protected boolean acceptNull;

        /**
         * @param expVal Expected value.
         * @param acceptNull If {@code true} value can be null;
         */
        protected TestPredicate(String expVal, boolean acceptNull) {
            this.expVal = expVal;
            this.acceptNull = acceptNull;
        }

        /** {@inheritDoc} */
        @Override public final boolean apply(GridCacheEntry<Integer, Object> e) {
            assertNotNull(e);

            Object val = e.peek();

            if (val == null) {
                if (!acceptNull)
                    assertNotNull(val);

                return true;
            }

            checkValue(val);

            return true;
        }

        /**
         * @param val Value.
         */
        abstract void checkValue(Object val);
    }

    /**
     *
     */
    @SuppressWarnings("PackageVisibleInnerClass")
    static class PortableValuePredicate extends TestPredicate {
        /**
         * @param expVal Expected value.
         * @param acceptNull If {@code true} value can be null;
         */
        PortableValuePredicate(String expVal, boolean acceptNull) {
            super(expVal, acceptNull);
        }

        /** {@inheritDoc} */
        @Override void checkValue(Object val) {
            GridPortableObject obj = (GridPortableObject)val;

            assertEquals(expVal, obj.field("val"));
        }
    }

    /**
     *
     */
    @SuppressWarnings("PackageVisibleInnerClass")
    static class TestValuePredicate extends TestPredicate {
        /**
         * @param expVal Expected value.
         * @param acceptNull If {@code true} value can be null;
         */
        TestValuePredicate(String expVal, boolean acceptNull) {
            super(expVal, acceptNull);
        }

        /** {@inheritDoc} */
        @Override void checkValue(Object val) {
            TestValue obj = (TestValue)val;

            assertEquals(expVal, obj.val);
        }
    }

    /**
     *
     */
    protected abstract static class TestClosure implements GridClosure<Object, Object> {
        /** */
        protected String expVal;

        /** */
        protected boolean acceptNull;

        /**
         * @param expVal Expected value.
         * @param acceptNull If {@code true} value can be null;
         */
        protected TestClosure(String expVal, boolean acceptNull) {
            this.expVal = expVal;
            this.acceptNull = acceptNull;
        }

        /** {@inheritDoc} */
        @Override public final Object apply(Object val) {
            if (val == null) {
                if (!acceptNull)
                    assertNotNull(val);

                return true;
            }

            checkValue(val);

            return val;
        }

        /**
         * @param val Value.
         */
        abstract void checkValue(Object val);
    }

    /**
     *
     */
    @SuppressWarnings("PackageVisibleInnerClass")
    static class PortableValueClosure extends TestClosure {
        /**
         * @param expVal Expected value.
         * @param acceptNull If {@code true} value can be null;
         */
        PortableValueClosure(String expVal, boolean acceptNull) {
            super(expVal, acceptNull);
        }

        /** {@inheritDoc} */
        @Override void checkValue(Object val) {
            GridPortableObject obj = (GridPortableObject)val;

            assertEquals(expVal, obj.field("val"));
        }
    }

    /**
     *
     */
    @SuppressWarnings("PackageVisibleInnerClass")
    static class TestValueClosure extends TestClosure {
        /**
         * @param expVal Expected value.
         * @param acceptNull If {@code true} value can be null;
         */
        TestValueClosure(String expVal, boolean acceptNull) {
            super(expVal, acceptNull);
        }

        /** {@inheritDoc} */
        @Override void checkValue(Object val) {
            TestValue obj = (TestValue)val;

            assertEquals(expVal, obj.val);
        }
    }
}
