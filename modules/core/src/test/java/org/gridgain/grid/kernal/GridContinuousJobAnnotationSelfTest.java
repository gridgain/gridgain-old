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
import org.gridgain.grid.compute.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.junits.common.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;

/**
 * Test for various job callback annotations.
 */
@GridCommonTest(group = "Kernal Self")
public class GridContinuousJobAnnotationSelfTest extends GridCommonAbstractTest {
    /** */
    private static final AtomicBoolean fail = new AtomicBoolean();

    /** */
    private static final AtomicInteger afterSendCnt = new AtomicInteger();

    /** */
    private static final AtomicInteger beforeFailoverCnt = new AtomicInteger();

    /** */
    private static final AtomicReference<Exception> err = new AtomicReference<>();

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration c = super.getConfiguration(gridName);

        c.setMarshalLocalJobs(false);

        return c;
    }

    /**
     * @throws Exception If test failed.
     */
    public void testJobAnnotation() throws Exception {
        testContinuousJobAnnotation(TestJob.class);
    }

    /**
     * @throws Exception If test failed.
     */
    public void testJobChildAnnotation() throws Exception {
        testContinuousJobAnnotation(TestJobChild.class);
    }

    /**
     * @param jobCls Job class.
     * @throws Exception If test failed.
     */
    public void testContinuousJobAnnotation(Class<?> jobCls) throws Exception {
        try {
            Grid grid = startGrid(0);
            startGrid(1);

            fail.set(true);

            grid.compute().execute(TestTask.class, jobCls).get();

            Exception e = err.get();

            if (e != null)
                throw e;
        }
        finally {
            stopGrid(0);
            stopGrid(1);
        }

        assertEquals(2, afterSendCnt.getAndSet(0));
        assertEquals(1, beforeFailoverCnt.getAndSet(0));
    }

    /** */
    @SuppressWarnings({"PublicInnerClass", "unused"})
    public static class TestTask implements GridComputeTask<Object, Object> {
        /** */
        @GridTaskContinuousMapperResource
        private GridComputeTaskContinuousMapper mapper;

        /** {@inheritDoc} */
        @Override public Map<? extends GridComputeJob, GridNode> map(List<GridNode> subgrid, Object arg) throws GridException {
            try {
                mapper.send(((Class<GridComputeJob>)arg).newInstance());
            }
            catch (Exception e) {
                throw new GridException("Job instantination failed.", e);
            }

            return null;
        }

        /** {@inheritDoc} */
        @Override public GridComputeJobResultPolicy result(GridComputeJobResult res, List<GridComputeJobResult> received)
            throws GridException {
            if (res.getException() != null) {
                if (res.getException() instanceof GridComputeUserUndeclaredException)
                    throw new GridException("Job threw unexpected exception.", res.getException());

                return GridComputeJobResultPolicy.FAILOVER;
            }

            return GridComputeJobResultPolicy.WAIT;
        }

        /** {@inheritDoc} */
        @Override public Object reduce(List<GridComputeJobResult> results) throws GridException {
            assert results.size() == 1 : "Unexpected result count: " + results.size();

            return null;
        }
    }

    /**
     *
     */
    private static class TestJob extends GridComputeJobAdapter {
        /** */
        private boolean flag = true;

        /** */
        TestJob() {
            X.println("Constructing TestJob [this=" + this + ", identity=" + System.identityHashCode(this) + "]");
        }


        /** */
        @GridComputeJobAfterSend
        private void afterSend() {
            X.println("AfterSend start TestJob [this=" + this + ", identity=" + System.identityHashCode(this) +
                ", flag=" + flag + "]");

            afterSendCnt.incrementAndGet();

            flag = false;

            X.println("AfterSend end TestJob [this=" + this + ", identity=" + System.identityHashCode(this) +
                ", flag=" + flag + "]");
        }

        /** */
        @GridComputeJobBeforeFailover
        private void beforeFailover() {
            X.println("BeforeFailover start TestJob [this=" + this + ", identity=" + System.identityHashCode(this) +
                ", flag=" + flag + "]");

            beforeFailoverCnt.incrementAndGet();

            flag = true;

            X.println("BeforeFailover end TestJob [this=" + this + ", identity=" + System.identityHashCode(this) +
                ", flag=" + flag + "]");
        }

        /** {@inheritDoc} */
        @Override public Serializable execute() throws GridException {
            X.println("Execute TestJob [this=" + this + ", identity=" + System.identityHashCode(this) +
                ", flag=" + flag + "]");

            if (!flag) {
                String msg = "Flag is false on execute [this=" + this + ", identity=" + System.identityHashCode(this) +
                    ", flag=" + flag + "]";

                X.println(msg);

                err.compareAndSet(null, new Exception(msg));
            }

            if (fail.get()) {
                fail.set(false);

                throw new GridException("Expected test exception.");
            }

            return null;
        }
    }

    /**
     *
     */
    private static class TestJobChild extends TestJob {
        /**
         * Required for reflectional creation.
         */
        TestJobChild() {
            // No-op.
        }
    }
}
