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
import org.gridgain.grid.lang.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.junits.common.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;

/**
 * This test checks that GridTaskListener is only called once per task.
 */
@SuppressWarnings("deprecation")
@GridCommonTest(group = "Kernal Self")
public class GridTaskListenerSelfTest extends GridCommonAbstractTest {
    /** */
    public GridTaskListenerSelfTest() {
        super(/*start grid*/true);
    }

    /**
     * Checks that GridTaskListener is only called once per task.
     *
     * @throws Exception If failed.
     */
    @SuppressWarnings({"BusyWait", "unchecked"})
    public void testGridTaskListener() throws Exception {
        final AtomicInteger cnt = new AtomicInteger(0);

        GridInClosure<GridFuture<?>> lsnr = new CI1<GridFuture<?>>() {
            @Override public void apply(GridFuture<?> fut) {
                assert fut != null;

                cnt.incrementAndGet();
            }
        };

        Grid grid = G.grid(getTestGridName());

        assert grid != null;

        grid.compute().localDeployTask(TestTask.class, TestTask.class.getClassLoader());

        GridComputeTaskFuture<?> fut = grid.compute().execute(TestTask.class.getName(), null);

        fut.listenAsync(lsnr);

        fut.get();

        while (cnt.get() == 0) {
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException e) {
                error("Got interrupted while sleep.", e);

                break;
            }
        }

        assert cnt.get() == 1 : "Unexpected GridTaskListener apply count [count=" + cnt.get() + ", expected=1]";
    }

    /** Test task. */
    private static class TestTask extends GridComputeTaskSplitAdapter<Serializable, Object> {
        /** {@inheritDoc} */
        @Override protected Collection<? extends GridComputeJob> split(int gridSize, Serializable arg) throws GridException {
            Collection<GridComputeJobAdapter> jobs = new ArrayList<>();

            for (int i = 0; i < 5; i++) {
                jobs.add(new GridComputeJobAdapter() {
                    @Override public Serializable execute() {
                        return 1;
                    }
                });
            }

            return jobs;
        }

        /** {@inheritDoc} */
        @Override public Object reduce(List<GridComputeJobResult> results) {
            return null;
        }
    }
}
