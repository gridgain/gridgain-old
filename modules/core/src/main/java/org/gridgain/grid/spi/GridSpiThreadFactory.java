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

package org.gridgain.grid.spi;

import org.gridgain.grid.logger.*;
import java.util.concurrent.*;

/**
 * This class provides implementation of {@link ThreadFactory}  factory
 * for creating grid SPI threads.
 */
public class GridSpiThreadFactory implements ThreadFactory {
    /** */
    private final GridLogger log;

    /** */
    private final String gridName;

    /** */
    private final String threadName;

    /**
     * @param gridName Grid name, possibly {@code null} for default grid.
     * @param threadName Name for threads created by this factory.
     * @param log Grid logger.
     */
    public GridSpiThreadFactory(String gridName, String threadName, GridLogger log) {
        assert log != null;
        assert threadName != null;

        this.gridName = gridName;
        this.threadName = threadName;
        this.log = log;
    }

    /** {@inheritDoc} */
    @Override public Thread newThread(final Runnable r) {
        return new GridSpiThread(gridName, threadName, log) {
            /** {@inheritDoc} */
            @Override protected void body() throws InterruptedException {
                r.run();
            }
        };
    }
}
