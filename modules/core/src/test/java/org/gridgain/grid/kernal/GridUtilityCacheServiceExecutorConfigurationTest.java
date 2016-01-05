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
import org.gridgain.grid.thread.*;
import org.gridgain.testframework.junits.common.*;

import java.util.concurrent.*;

import static org.gridgain.grid.GridConfiguration.*;

/**
 * Tests utility cache service executor customisation via {@link GridConfiguration} (GG-10094).
 */
@GridCommonTest(group = "Kernal Self")
public class GridUtilityCacheServiceExecutorConfigurationTest extends GridCommonAbstractTest {
    /**
     * @throws Exception If failed.
     */
    public void testUtilityCacheExecutorCustomisation() throws Exception {
        GridConfiguration cfg = new GridConfiguration();

        int customCoreCnt = DFLT_SYSTEM_CORE_THREAD_CNT / 4;
        int customMaxCnt = DFLT_SYSTEM_MAX_THREAD_CNT / 2;
        
        cfg.setUtilityCacheExecutorService(new GridThreadPoolExecutor(
            "utility-" + cfg.getGridName(),
            customCoreCnt,
            customMaxCnt,
            DFLT_SYSTEM_KEEP_ALIVE_TIME,
            new LinkedBlockingQueue<Runnable>(DFLT_SYSTEM_THREADPOOL_QUEUE_CAP)));
        
        try(Grid grid = GridGain.start(cfg)) {
            assertEquals(customCoreCnt, 
                ((GridThreadPoolExecutor)grid.configuration().getUtilityCacheExecutorService()).getCorePoolSize());
            
            assertEquals(customMaxCnt,
                ((GridThreadPoolExecutor)grid.configuration().getUtilityCacheExecutorService()).getMaximumPoolSize());
        }
    }
}
