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

package org.gridgain.examples.misc.client.router;

import org.gridgain.client.router.*;
import org.gridgain.grid.*;

import javax.swing.*;

/**
 * This example shows how to configure and run TCP and HTTP routers from java API.
 * <p>
 * Refer to {@link GridRouterFactory} documentation for more details on
 * how to manage routers' lifecycle. Also see {@link GridTcpRouterConfiguration}
 * for more configuration options.
 * <p>
 * Note that to start the example, {@code GRIDGAIN_HOME} system property or environment variable
 * must be set.
 */
public class RouterStartup {
    /** Change to {@code false} to disable {@code TCP_NODELAY}. */
    private static final boolean TCP_NODELAY = true;

    /**
     * Starts up a router with default configuration.
     *
     * @param args Command line arguments, none required.
     * @throws GridException If router failed to start.
     */
    public static void main(String[] args) throws GridException {
        try {
            GridRouterFactory.startTcpRouter(tcpRouterConfiguration());

            // Wait until Ok is pressed.
            JOptionPane.showMessageDialog(
                null,
                new JComponent[]{
                    new JLabel("GridGain router started."),
                    new JLabel("Press OK to stop GridGain router.")
                },
                "GridGain router",
                JOptionPane.INFORMATION_MESSAGE
            );
        }
        finally {
            GridRouterFactory.stopAllRouters();
        }
    }

    /**
     * Creates a default TCP router configuration.
     *
     * @return TCP router configuration
     */
    private static GridTcpRouterConfiguration tcpRouterConfiguration() {
        GridTcpRouterConfiguration cfg = new GridTcpRouterConfiguration();

        cfg.setNoDelay(TCP_NODELAY);

        return cfg;
    }
}
