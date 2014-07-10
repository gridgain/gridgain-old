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

package org.gridgain.grid.startup.cmdline;

import org.gridgain.grid.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.junits.common.*;
import java.util.concurrent.*;

import static org.gridgain.grid.GridGainState.*;

/**
 * Command line loader test.
 */
@GridCommonTest(group = "Loaders")
public class GridCommandLineLoaderTest extends GridCommonAbstractTest {
    /** */
    private static final String GRID_CFG_PATH = "/modules/core/src/test/config/loaders/grid-cfg.xml";

    /** */
    private final CountDownLatch latch = new CountDownLatch(2);

    /** */
    public GridCommandLineLoaderTest() {
        super(false);
    }

    /**
     * @throws Exception If failed.
     */
    public void testLoader() throws Exception {
        String path = U.getGridGainHome() + GRID_CFG_PATH;

        info("Loading Grid from configuration file: " + path);

        G.addListener(new GridGainListener() {
            @Override public void onStateChange(String name, GridGainState state) {
                if (state == STARTED) {
                    info("Received started notification from grid: " + name);

                    latch.countDown();

                    G.stop(name, true);
                }
            }
        });

        GridCommandLineStartup.main(new String[] {path});
    }
}
