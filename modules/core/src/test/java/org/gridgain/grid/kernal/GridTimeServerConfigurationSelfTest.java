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
import org.gridgain.testframework.junits.common.*;

/**
 * Test that the configuration for the "time-server" is propagated it (GG-10090).
 */
@GridCommonTest(group = "Kernal Self")
public class GridTimeServerConfigurationSelfTest extends GridCommonAbstractTest {
    /**
     * @throws Exception If failed.
     */
    public void testTimeServerConfiguration() throws Exception {
        GridConfiguration cfg0 = new GridConfiguration();
        
        cfg0.setTimeServerPortBase(10999); // Change default.
        cfg0.setTimeServerPortRange(99); // Change default.
        
        try(Grid grid = GridGain.start(cfg0)) {
            GridConfiguration cfg1 = grid.configuration();
            
            assertEquals(cfg0.getTimeServerPortBase(), cfg1.getTimeServerPortBase());
            assertEquals(cfg0.getTimeServerPortRange(), cfg1.getTimeServerPortRange());
        }
    }
}
