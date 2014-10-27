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

package org.gridgain.grid.logger.log4j;

import junit.framework.*;
import org.gridgain.grid.logger.*;
import org.gridgain.testframework.junits.common.*;

/**
 * Log4j not initialized test.
 */
@GridCommonTest(group = "Logger")
public class GridLog4jNotInitializedTest extends TestCase {
    /** */
    public void testLogInitialize() {
        GridLogger log = new GridLog4jLogger().getLogger(GridLog4jNotInitializedTest.class);

        if (log.isDebugEnabled())
            log.debug("This is 'debug' message.");
        else
            System.out.println("DEBUG level is not enabled.");

        if (log.isInfoEnabled())
            log.info("This is 'info' message.");
        else
            System.out.println("INFO level is not enabled.");

        log.warning("This is 'warning' message.");
        log.error("This is 'error' message.");
    }
}
