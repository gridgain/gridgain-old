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
import org.gridgain.grid.messaging.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Tests for {@link GridMessagingListenActor}.
 */
public class GridListenActorSelfTest extends GridCommonAbstractTest {
    /** */
    private static final int MSG_QTY = 10;

    /** */
    private static final int PING_PONG_STEPS = 10;

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        startGrid();
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        stopGrid();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("deprecation")
    @Override protected void afterTest() throws Exception {
        ((GridKernal)grid()).context().io().
            removeMessageListener(GridTopic.TOPIC_COMM_USER.name());
    }

    /**
     *
     * @throws Exception Thrown if failed.
     */
    public void testBasicFlow() throws Exception {
        final AtomicInteger cnt = new AtomicInteger(0);

        grid().message().localListen(null, new GridMessagingListenActor<String>() {
            @Override
            public void receive(UUID uuid, String rcvMsg) {
                if ("TEST".equals(rcvMsg)) {
                    cnt.incrementAndGet();

                    // "Exit" after 1st message.
                    // Should never receive any more messages.
                    stop();
                } else {
                    assert false : "Unknown message: " + rcvMsg;

                    stop();
                }
            }
        });

        grid().message().send(null, "TEST"); // This message we should receive.

        // Flood it.
        for (int i = 0; i < 100; i++)
           grid().message().send(null, "TEST"); // This message should be lost...

        Thread.sleep(2000);

        assert cnt.get() == 1 : "Count is " + cnt.get();
    }

    /**
     * @throws Exception If failed.
     */
    public void testImmediateStop() throws Exception {
        doSendReceive(MSG_QTY, 1);
    }

    /**
     * @throws Exception If failed.
     */
    public void testReceiveAll() throws Exception {
        doSendReceive(MSG_QTY, MSG_QTY);
    }

    /**
     * Testing {@link GridMessagingListenActor#respond(UUID, Object)} method.
     *
     * @throws Exception If failed.
     */
    public void testRespondToRemote() throws Exception {
        startGrid(1);

        try {
            final GridNode rmt = grid(1).localNode();

            grid().message().localListen(null, new GridMessagingListenActor<String>() {
                @Override protected void receive(UUID nodeId, String rcvMsg) throws GridException {
                    System.out.println("Local node received message: '" + rcvMsg + "'");

                    respond(rmt.id(), "RESPONSE");
                }
            });

            final AtomicInteger cnt = new AtomicInteger();

            // Response listener
            grid(1).message().localListen(null, new GridMessagingListenActor<String>() {
                @Override public void receive(UUID nodeId, String rcvMsg) {
                    if ("RESPONSE".equals(rcvMsg)) {
                        System.out.println("Remote node received message: '" + rcvMsg + "'");

                        cnt.incrementAndGet();
                    }
                }
            });

            grid().message().send(null, "REQUEST");

            assert GridTestUtils.waitForCondition(new PA() {
                @Override public boolean apply() {
                    return cnt.intValue() == 1;
                }
            }, getTestTimeout());
        }
        finally {
            stopGrid(1);
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testPingPong() throws Exception {
        final AtomicInteger pingCnt = new AtomicInteger();
        final AtomicInteger pongCnt = new AtomicInteger();

        final CountDownLatch latch = new CountDownLatch(PING_PONG_STEPS);

        grid().message().localListen(null, new GridMessagingListenActor<String>() {
            @Override
            protected void receive(UUID nodeId, String rcvMsg) throws GridException {
                System.out.println("Received message: '" + rcvMsg + "'");

                if ("PING".equals(rcvMsg)) {
                    pingCnt.incrementAndGet();

                    respond("PONG");
                } else if ("PONG".equals(rcvMsg)) {
                    pongCnt.incrementAndGet();

                    latch.countDown();

                    if (latch.getCount() > 0)
                        respond("PING");
                    else
                        stop();
                }
            }
        });

        grid().message().send(null, "PING");

        latch.await();

        assert pingCnt.intValue() == PING_PONG_STEPS;
        assert pongCnt.intValue() == PING_PONG_STEPS;
    }

    /**
     * @param snd Sent messages quantity.
     * @param rcv Max quantity of received messages before listener is removed.
     * @throws Exception IF failed.
     */
    private void doSendReceive(int snd, final int rcv) throws Exception {
        assert rcv > 0;
        assert snd >= 0;

        final AtomicInteger cnt = new AtomicInteger(0);

        grid().message().localListen(null, new GridMessagingListenActor<String>() {
            @Override
            protected void receive(UUID nodeId, String rcvMsg) {
                System.out.println(Thread.currentThread().getName() + "# Received message: '" + rcvMsg + "'");

                cnt.incrementAndGet();

                if (cnt.intValue() == rcv) {
                    System.out.println(Thread.currentThread().getName() + "Calling stop...");

                    stop();
                } else if (cnt.intValue() < rcv)
                    skip();
                else
                    assert false;
            }
        });

        for (int i = 1; i <= snd; i++) {
            String msg = "MESSAGE " + i;

            grid().message().send(null, msg);

            System.out.println(Thread.currentThread().getName() + "# Sent message: '" + msg + "'");
        }

        Thread.sleep(2000);

        assert cnt.intValue() == rcv;
    }
}
