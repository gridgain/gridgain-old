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

package org.gridgain.grid.spi.indexing.h2;

import org.gridgain.grid.spi.indexing.*;
import org.gridgain.testframework.junits.spi.*;

/**
 * Tests for H2 indexing SPI.
 */
@GridSpiTest(spi = GridH2IndexingSpi.class, group = "Indexing SPI")
public class GridH2IndexingSpiOffheapSelfTest extends GridIndexingSpiAbstractSelfTest<GridH2IndexingSpi> {
    /** */
    private static final long offheap = 10000000;

    private static GridH2IndexingSpi currentSpi;

    /** {@inheritDoc} */
    @Override protected void spiConfigure(GridH2IndexingSpi spi) throws Exception {
        super.spiConfigure(spi);

        spi.setMaxOffHeapMemory(offheap);

        currentSpi = spi;
    }

    /** {@inheritDoc} */
    @Override protected void afterSpiStopped() throws Exception {
        super.afterSpiStopped();

        assertEquals(0, currentSpi.getAllocatedOffHeapMemory());
    }
}
