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

package org.gridgain.grid.thread;

import org.gridgain.testframework.junits.common.*;

import java.util.*;

/**
 * Test for {@link GridThread}.
 */
@GridCommonTest(group = "Utils")
public class GridThreadTest extends GridCommonAbstractTest {
    /** Thread count. */
    private static final int THREAD_CNT = 3;

    /**
     * @throws Exception If failed.
     */
    public void testAssertion() throws Exception {
        Collection<GridThread> ts = new ArrayList<>();

        for (int i = 0; i < THREAD_CNT; i++) {
            ts.add(new GridThread("test-grid-" + i, "test-thread", new Runnable() {
                @Override public void run() {
                    assert false : "Expected assertion.";
                }
            }));
        }

        for (GridThread t : ts)
            t.start();

        for (GridThread t : ts)
            t.join();
    }
}
