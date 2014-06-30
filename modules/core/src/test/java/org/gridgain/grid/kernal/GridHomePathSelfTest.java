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
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.junits.common.*;

import static org.gridgain.grid.GridSystemProperties.*;

/**
 *
 */
public class GridHomePathSelfTest extends GridCommonAbstractTest {
    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        cfg.setLocalHost(getTestResources().getLocalHost());

        return cfg;
    }

    /**
     * @throws Exception If failed.
     */
    public void testHomeOverride() throws Exception {
        try {
            startGrid(0);

            // Test home override.
            GridConfiguration c = getConfiguration(getTestGridName(1));

            c.setGridGainHome("/new/path");

            try {
                G.start(c);

                assert false : "Exception should have been thrown.";
            }
            catch (Exception e) {
                if (X.hasCause(e, GridRuntimeException.class))
                    info("Caught expected exception: " + e);
                else
                    throw e;
            }

            // Test no override.
            GridConfiguration c1 = getConfiguration(getTestGridName(1));

            c1.setGridGainHome(System.getProperty(GG_HOME));

            G.start(c1);
        }
        finally {
            stopAllGrids();
        }
    }
}
