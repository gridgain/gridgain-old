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

package org.gridgain.grid.spi.discovery;

import org.gridgain.grid.*;
import org.gridgain.grid.events.*;
import org.gridgain.grid.kernal.managers.eventstorage.*;
import org.gridgain.testframework.junits.spi.*;

import javax.swing.*;
import java.io.*;
import java.util.*;

/**
 * Base discovery test class.
 * @param <T> SPI implementation class.
 */
@SuppressWarnings({"JUnitAbstractTestClassNamingConvention"})
public abstract class GridAbstractDiscoveryTest<T extends GridDiscoverySpi> extends GridSpiAbstractTest<T> {
    /** */
    @SuppressWarnings({"ClassExplicitlyExtendsThread"})
    private class Pinger extends Thread {
        /** */
        private final Object mux = new Object();

        /** */
        @SuppressWarnings({"FieldAccessedSynchronizedAndUnsynchronized"})
        private boolean isCanceled;

        /** {@inheritDoc} */
        @SuppressWarnings({"UnusedCatchParameter"})
        @Override public void run() {
            Random rnd = new Random();

            while (isCanceled) {
                try {
                    Collection<GridNode> nodes = getSpi().getRemoteNodes();

                    pingNode(UUID.randomUUID(), false);

                    for (GridNode item : nodes) {
                        pingNode(item.id(), true);
                    }

                    pingNode(UUID.randomUUID(), false);
                }
                catch (Exception e) {
                    error("Can't get SPI.", e);
                }

                synchronized (mux) {
                    if (isCanceled) {
                        try {
                            mux.wait(getPingFrequency() * (1 + rnd.nextInt(10)));
                        }
                        catch (InterruptedException e) {
                            //No-op.
                        }
                    }
                }
            }
        }

        /**
         * @param nodeId Node UUID.
         * @param exists Exists flag.
         * @throws Exception If failed.
         */
        private void pingNode(UUID nodeId, boolean exists) throws Exception {
            boolean flag = getSpi().pingNode(nodeId);

            info((flag != exists ? "***Error*** " : "") + "Ping " + (exists ? "exist" : "random") +
                " node [nodeId=" + nodeId + ", pingResult=" + flag + ']');
        }

        /** {@inheritDoc} */
        @Override public void interrupt() {
            synchronized (mux) {
                isCanceled = true;

                mux.notifyAll();
            }

            super.interrupt();
        }
    }

    /**
     * @return Ping frequency.
     */
    public abstract long getPingFrequency();

    /**
     * @return Pinger start flag.
     */
    public boolean isPingerStart() {
        return true;
    }

    /** */
    private class DiscoveryListener implements GridLocalEventListener {
        /** {@inheritDoc} */
        @Override public void onEvent(GridEvent evt) {
            info("Discovery event [event=" + evt + ']');
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testDiscovery() throws Exception {
        GridLocalEventListener discoLsnr = new DiscoveryListener();

        getSpiContext().addLocalEventListener(discoLsnr);

        Pinger pinger = null;

        if (isPingerStart()) {
            pinger = new Pinger();

            pinger.start();
        }

        JOptionPane.showMessageDialog(null, "Press OK to end test.");

        if (pinger != null)
            pinger.interrupt();
    }

    /** {@inheritDoc} */
    @Override protected Map<String, Serializable> getNodeAttributes() {
        Map<String, Serializable> attrs = new HashMap<>(1);

        attrs.put("testDiscoveryAttribute", new Date());

        return attrs;
    }
}
