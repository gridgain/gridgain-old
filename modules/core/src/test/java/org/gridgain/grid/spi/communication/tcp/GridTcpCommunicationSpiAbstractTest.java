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

package org.gridgain.grid.spi.communication.tcp;

import org.gridgain.grid.spi.communication.*;
import org.gridgain.grid.util.direct.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.nio.*;
import org.gridgain.testframework.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * Test for {@link GridTcpCommunicationSpi}
 */
abstract class GridTcpCommunicationSpiAbstractTest extends GridAbstractCommunicationSelfTest<GridCommunicationSpi> {
    /** */
    private static final int SPI_COUNT = 3;

    /** */
    public static final int IDLE_CONN_TIMEOUT = 2000;

    /** */
    private final boolean useShmem;

    /**
     * @param useShmem Use shared mem flag.
     */
    protected GridTcpCommunicationSpiAbstractTest(boolean useShmem) {
        this.useShmem = useShmem;
    }

    /** {@inheritDoc} */
    @Override protected GridCommunicationSpi<GridTcpCommunicationMessageAdapter> getSpi(int idx) {
        GridTcpCommunicationSpi spi = new GridTcpCommunicationSpi();

        if (!useShmem)
            spi.setSharedMemoryPort(-1);

        spi.setLocalPort(GridTestUtils.getNextCommPort(getClass()));
        spi.setIdleConnectionTimeout(IDLE_CONN_TIMEOUT);
        spi.setTcpNoDelay(tcpNoDelay());

        return spi;
    }

    /**
     * @return Value of property '{@link GridTcpCommunicationSpi#isTcpNoDelay()}'.
     */
    protected boolean tcpNoDelay() {
        return true;
    }

    /** {@inheritDoc} */
    @Override protected int getSpiCount() {
        return SPI_COUNT;
    }

    /** {@inheritDoc} */
    @Override public void testSendToManyNodes() throws Exception {
        super.testSendToManyNodes();

        // Test idle clients remove.
        for (GridCommunicationSpi spi : spis.values()) {
            ConcurrentMap<UUID, GridTcpCommunicationClient> clients = U.field(spi, "clients");

            assertEquals(2, clients.size());

            clients.put(UUID.randomUUID(), F.first(clients.values()));
        }
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        super.afterTest();

        for (GridCommunicationSpi spi : spis.values()) {
            ConcurrentMap<UUID, GridTcpCommunicationClient> clients = U.field(spi, "clients");

            for (int i = 0; i < 10 && !clients.isEmpty(); i++) {
                info("Check failed for SPI [grid=" + GridTestUtils.getFieldValue(spi, "gridName") +
                    ", spi=" + spi + ']');

                U.sleep(1000);
            }

            assert clients.isEmpty() : "Clients: " + clients;
        }
    }
}
