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
import org.gridgain.grid.product.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.junits.common.*;

import static org.gridgain.grid.GridSystemProperties.*;

/**
 * Tests version methods.
 */
public class GridVersionSelfTest extends GridCommonAbstractTest {
    /**
     * @throws Exception If failed.
     */
    public void testVersions() throws Exception {
        String propVal = System.getProperty(GG_UPDATE_NOTIFIER);

        System.setProperty(GG_UPDATE_NOTIFIER, "true");

        try {
            Grid grid = startGrid();

            GridProductVersion currVer = grid.product().version();

            String newVer = null;

            for (int i = 0; i < 30; i++) {
                newVer = grid.product().latestVersion();

                if (newVer != null)
                    break;

                U.sleep(100);
            }

            info("Versions [cur=" + currVer + ", latest=" + newVer + ']');

            assertNotNull(newVer);
            assertNotSame(currVer.toString(), newVer);
        }
        finally {
            stopGrid();

            if (propVal != null)
                System.setProperty(GG_UPDATE_NOTIFIER, propVal);
            else
                System.clearProperty(GG_UPDATE_NOTIFIER);
        }
    }
}
