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
import org.apache.log4j.*;
import org.gridgain.grid.logger.*;
import org.gridgain.testframework.junits.common.*;

/**
 * Log4j initialized test.
 */
@GridCommonTest(group = "Logger")
public class GridLog4jInitializedTest extends TestCase {

    /**
     * @throws Exception If failed.
     */
    @Override protected void setUp() throws Exception {
        BasicConfigurator.configure();
    }

    /** */
    public void testLogInitialize() {
        GridLogger log = new GridLog4jLogger();

        assert log.isInfoEnabled() == true;

        if (log.isDebugEnabled())
            log.debug("This is 'debug' message.");

        log.info("This is 'info' message.");
        log.warning("This is 'warning' message.");
        log.warning("This is 'warning' message.", new Exception("It's a test warning exception"));
        log.error("This is 'error' message.");
        log.error("This is 'error' message.", new Exception("It's a test error exception"));

        assert log.getLogger(GridLog4jInitializedTest.class.getName()) instanceof GridLog4jLogger;
    }
}
