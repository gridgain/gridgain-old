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

package org.gridgain.grid.kernal.managers.communication;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;
import java.util.concurrent.atomic.*;

/**
 * Grid communication manager self test.
 */
@GridCommonTest(group = "Kernal Self")
public class GridCommunicationManagerListenersSelfTest extends GridCommonAbstractTest {
    /** */
    public GridCommunicationManagerListenersSelfTest() {
        super(true);
    }

    /**
     * Works fine.
     */
    @SuppressWarnings({"deprecation"})
    public void testDifferentListeners() {
        Grid grid = G.grid(getTestGridName());

        for (int i = 0; i < 2000; i++) {
            P2<UUID, Object> l = new P2<UUID, Object>() {
                @Override public boolean apply(UUID uuid, Object o) {
                    return false;
                }
            };

            grid.message().localListen(null, l);
        }

        info(getName() + ": worked without exceptions.");
    }


    /**
     * Fails on the 1001st time.
     */
    public void testMultipleExecutionsWithoutListeners() {
        checkLoop(1001);
    }

    /**
     * This is the workaround- as long as we keep a message listener in
     * the stack, our FIFO bug isn't exposed.  Comment above out to see.
     */
    @SuppressWarnings({"deprecation"})
    public void testOneListener() {
        Grid grid = G.grid(getTestGridName());

        final AtomicBoolean stop = new AtomicBoolean();

        P2<UUID, Object> l = new P2<UUID, Object>() {
            @Override public boolean apply(UUID uuid, Object o) {
                return stop.get();
            }
        };

        try {
            grid.message().localListen(null, l);

            checkLoop(2000);
        } finally {
            stop.set(true);
        }
    }

    /**
     * Now, our test will fail on the first message added after our safety
     * message listener has been removed.
     */
    public void testSingleExecutionWithoutListeners() {
        checkLoop(1);
    }

    /**
     * @param cnt Iteration count.
     */
    private void checkLoop(int cnt) {
        for (int i = 1; i <= cnt; i++) {
            MessageListeningTask t = new MessageListeningTask();

            GridComputeTaskFuture<?> f = G.grid(getTestGridName()).compute().execute(t.getClass(), null);

            try {
                f.get();
            }
            catch (GridException e) {
                assert false : "Failed to execute task [iteration=" + i + ", err=" + e.getMessage() + ']';
            }

            if (i % 100 == 0)
                info(getName() + ": through " + i);
        }
    }

    /**
     *
     */
    private static class MessageListeningTask extends GridComputeTaskSplitAdapter<Object, Object>
        implements GridMessageListener {
        /** */
        @GridInstanceResource
        private transient Grid grid;

        /** */
        private AtomicBoolean stop = new AtomicBoolean();

        /** {@inheritDoc} */
        @Override public Collection<? extends GridComputeJob> split(int gridSize, Object arg) throws GridException {
            grid.message().localListen(null, new P2<UUID, Object>() {
                @Override public boolean apply(UUID uuid, Object o) {
                    return stop.get();
                }
            });

            return Arrays.asList(new GridComputeJobAdapter() {
                @Override public Object execute() {
                    return null;
                }
            });
        }

        /** {@inheritDoc} */
        @Override public Object reduce(List<GridComputeJobResult> results) throws GridException {
            stop.set(true);

            return null;
        }

        /** {@inheritDoc} */
        @Override public void onMessage(UUID nodeId, Object msg) {
            // No-op.
        }
    }
}
