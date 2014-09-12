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

package org.gridgain.grid.kernal;

import org.gridgain.grid.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.junits.common.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import static org.gridgain.grid.GridLifecycleEventType.*;

/**
 * Lifecycle bean test.
 */
@GridCommonTest(group = "Kernal Self")
public class GridLifecycleBeanSelfTest extends GridCommonAbstractTest {
    /** */
    private LifeCycleBaseBean bean;

    /** */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration c = super.getConfiguration(gridName);

        c.setLifecycleBeans(bean);

        return c;
    }

    /**
     * @throws Exception If failed.
     */
    public void testNoErrors() throws Exception {
        bean = new LifeCycleBaseBean();

        startGrid();

        try {
            assertEquals(GridGainState.STARTED, G.state(getTestGridName()));

            assertEquals(1, bean.count(BEFORE_GRID_START));
            assertEquals(1, bean.count(AFTER_GRID_START));
            assertEquals(0, bean.count(BEFORE_GRID_STOP));
            assertEquals(0, bean.count(AFTER_GRID_STOP));
        }
        finally {
            stopAllGrids();
        }


        assertEquals(GridGainState.STOPPED, G.state(getTestGridName()));

        assertEquals(1, bean.count(BEFORE_GRID_START));
        assertEquals(1, bean.count(AFTER_GRID_START));
        assertEquals(1, bean.count(BEFORE_GRID_STOP));
        assertEquals(1, bean.count(AFTER_GRID_STOP));
    }

    /**
     * @throws Exception If failed.
     */
    public void testGridErrorBeforeStart() throws Exception {
        checkBeforeStart(true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testOtherErrorBeforeStart() throws Exception {
        checkBeforeStart(false);
    }

    /**
     * @throws Exception If failed.
     */
    public void testGridErrorAfterStart() throws Exception {
        checkAfterStart(true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testOtherErrorAfterStart() throws Exception {
        checkAfterStart(false);
    }

    /**
     * @param gridErr Grid error flag.
     * @throws Exception If failed.
     */
    private void checkBeforeStart(boolean gridErr) throws Exception {
        bean = new LifeCycleExceptionBean(BEFORE_GRID_START, gridErr);

        try {
            startGrid();

            assertTrue(false); // Should never get here.
        }
        catch (GridException expected) {
            info("Got expected exception: " + expected);

            assertEquals(GridGainState.STOPPED, G.state(getTestGridName()));
        }
        finally {
            stopAllGrids();
        }

        assertEquals(0, bean.count(BEFORE_GRID_START));
        assertEquals(0, bean.count(AFTER_GRID_START));
        assertEquals(0, bean.count(BEFORE_GRID_STOP));
        assertEquals(1, bean.count(AFTER_GRID_STOP));
    }

    /**
     * @param gridErr Grid error flag.
     * @throws Exception If failed.
     */
    private void checkAfterStart(boolean gridErr) throws Exception {
        bean = new LifeCycleExceptionBean(AFTER_GRID_START, gridErr);

        try {
            startGrid();

            assertTrue(false); // Should never get here.
        }
        catch (GridException expected) {
            info("Got expected exception: " + expected);

            assertEquals(GridGainState.STOPPED, G.state(getTestGridName()));
        }
        finally {
            stopAllGrids();
        }

        assertEquals(1, bean.count(BEFORE_GRID_START));
        assertEquals(0, bean.count(AFTER_GRID_START));
        assertEquals(1, bean.count(BEFORE_GRID_STOP));
        assertEquals(1, bean.count(AFTER_GRID_STOP));
    }

    /**
     * @throws Exception If failed.
     */
    public void testGridErrorBeforeStop() throws Exception {
        checkOnStop(BEFORE_GRID_STOP, true);

        assertEquals(1, bean.count(BEFORE_GRID_START));
        assertEquals(1, bean.count(AFTER_GRID_START));
        assertEquals(0, bean.count(BEFORE_GRID_STOP));
        assertEquals(1, bean.count(AFTER_GRID_STOP));
    }

    /**
     * @throws Exception If failed.
     */
    public void testOtherErrorBeforeStop() throws Exception {
        checkOnStop(BEFORE_GRID_STOP, false);

        assertEquals(1, bean.count(BEFORE_GRID_START));
        assertEquals(1, bean.count(AFTER_GRID_START));
        assertEquals(0, bean.count(BEFORE_GRID_STOP));
        assertEquals(1, bean.count(AFTER_GRID_STOP));
    }

    /**
     * @throws Exception If failed.
     */
    public void testGridErrorAfterStop() throws Exception {
        checkOnStop(AFTER_GRID_STOP, true);

        assertEquals(1, bean.count(BEFORE_GRID_START));
        assertEquals(1, bean.count(AFTER_GRID_START));
        assertEquals(1, bean.count(BEFORE_GRID_STOP));
        assertEquals(0, bean.count(AFTER_GRID_STOP));
    }

    /**
     * @throws Exception If failed.
     */
    public void testOtherErrorAfterStop() throws Exception {
        checkOnStop(AFTER_GRID_STOP, false);

        assertEquals(1, bean.count(BEFORE_GRID_START));
        assertEquals(1, bean.count(AFTER_GRID_START));
        assertEquals(1, bean.count(BEFORE_GRID_STOP));
        assertEquals(0, bean.count(AFTER_GRID_STOP));
    }

    /**
     * @param evt Error event.
     * @param gridErr Grid error flag.
     * @throws Exception If failed.
     */
    private void checkOnStop(GridLifecycleEventType evt, boolean gridErr) throws Exception {
        bean = new LifeCycleExceptionBean(evt, gridErr);

        try {
            startGrid();

            assertEquals(GridGainState.STARTED, G.state(getTestGridName()));
        }
        catch (GridException ignore) {
            assertTrue(false);
        }
        finally {
            try {
                stopAllGrids();

                assertEquals(GridGainState.STOPPED, G.state(getTestGridName()));
            }
            catch (Exception ignore) {
                assertTrue(false);
            }
        }
    }

    /**
     *
     */
    private static class LifeCycleBaseBean implements GridLifecycleBean {
        /** */
        private Map<GridLifecycleEventType, AtomicInteger> callsCntr =
            new EnumMap<>(GridLifecycleEventType.class);

        /**
         *
         */
        private LifeCycleBaseBean() {
            for (GridLifecycleEventType t : GridLifecycleEventType.values())
                callsCntr.put(t, new AtomicInteger());
        }

        /** {@inheritDoc} */
        @Override public void onLifecycleEvent(GridLifecycleEventType evt) throws GridException {
            callsCntr.get(evt).incrementAndGet();
        }

        /**
         * @param t Event type.
         * @return Number of calls.
         */
        public int count(GridLifecycleEventType t) {
            return callsCntr.get(t).get();
        }
    }

    /**
     *
     */
    private static class LifeCycleExceptionBean extends LifeCycleBaseBean {
        /** */
        private GridLifecycleEventType errType;

        private boolean gridErr;

        /**
         * @param errType type of event to throw error.
         * @param gridErr {@code True} if {@link GridException}.
         */
        private LifeCycleExceptionBean(GridLifecycleEventType errType, boolean gridErr) {
            this.errType = errType;
            this.gridErr = gridErr;
        }

        /** {@inheritDoc} */
        @Override public void onLifecycleEvent(GridLifecycleEventType evt) throws GridException {
            if (evt == errType) {
                if (gridErr)
                    throw new GridException("Expected exception for event: " + evt) {
                        @Override public void printStackTrace(PrintStream s) {
                            // No-op.
                        }

                        @Override public void printStackTrace(PrintWriter s) {
                            // No-op.
                        }
                    };
                else
                    throw new RuntimeException("Expected exception for event: " + evt) {
                        @Override public void printStackTrace(PrintStream s) {
                            // No-op.
                        }

                        @Override public void printStackTrace(PrintWriter s) {
                            // No-op.
                        }
                    };
            }

            super.onLifecycleEvent(evt);
        }
    }
}
