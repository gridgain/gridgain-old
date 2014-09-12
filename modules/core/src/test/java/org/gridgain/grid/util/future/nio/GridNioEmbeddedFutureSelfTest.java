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

package org.gridgain.grid.util.future.nio;

import org.gridgain.grid.util.nio.*;
import org.gridgain.testframework.junits.common.*;

import static java.util.concurrent.TimeUnit.*;

/**
 * Test for NIO embedded future.
 */
public class GridNioEmbeddedFutureSelfTest extends GridCommonAbstractTest {
    /**
     * @throws Exception If failed.
     */
    public void testNioEmbeddedFuture() throws Exception {
        // Original future.
        final GridNioFutureImpl<Integer> origFut = new GridNioFutureImpl<>();

        // Embedded future to test.
        final GridNioEmbeddedFuture<Integer> embFut = new GridNioEmbeddedFuture<>();

        embFut.onDone(origFut, null);

        assertFalse("Expect original future is not complete.", origFut.isDone());

        // Finish original future in separate thread.
        Thread t = new Thread() {
            @Override public void run() {
                origFut.onDone(100);
            }
        };

        t.start();
        t.join();

        assertTrue("Expect original future is complete.", origFut.isDone());
        assertTrue("Expect embedded future is complete.", embFut.isDone());

        // Wait for embedded future completes.
        assertEquals(new Integer(100), embFut.get(1, SECONDS));
    }
}
