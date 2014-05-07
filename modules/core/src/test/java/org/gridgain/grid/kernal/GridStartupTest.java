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

import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.junits.common.*;

import javax.swing.*;

/**
 * GridGain startup.
 */
@SuppressWarnings({"ProhibitedExceptionDeclared"})
@GridCommonTest(group = "Kernal")
public class GridStartupTest extends GridCommonAbstractTest {
    /** */
    public GridStartupTest() {
        super(false);
    }

    /** {@inheritDoc} */
    @Override protected long getTestTimeout() {
        return Long.MAX_VALUE;
    }

    /**
     * @throws Exception If failed.
     */
    public void testStartup() throws Exception {
        //resetLog4j("org.gridgain.grid.kernal.processors.cache.distributed.dht.preloader", Level.DEBUG, false, 0);

        //G.start("modules/tests/config/spring-multicache.xml");
        //G.start("examples/config/example-cache.xml");

        G.start();

        // Wait until Ok is pressed.
        JOptionPane.showMessageDialog(
            null,
            new JComponent[] {
                new JLabel("GridGain started."),
                new JLabel(
                    "<html>" +
                        "You can use JMX console at <u>http://localhost:1234</u>" +
                    "</html>"),
                new JLabel("Press OK to stop GridGain.")
            },
            "GridGain Startup JUnit",
            JOptionPane.INFORMATION_MESSAGE
        );

        G.stop(true);
    }
}
