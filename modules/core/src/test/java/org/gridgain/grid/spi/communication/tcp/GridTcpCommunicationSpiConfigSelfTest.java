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

import org.gridgain.testframework.junits.spi.*;

/**
 * TCP communication SPI config test.
 */
@GridSpiTest(spi = GridTcpCommunicationSpi.class, group = "Communication SPI")
public class GridTcpCommunicationSpiConfigSelfTest extends GridSpiAbstractConfigTest<GridTcpCommunicationSpi> {
    /**
     * @throws Exception If failed.
     */
    public void testNegativeConfig() throws Exception {
        checkNegativeSpiProperty(new GridTcpCommunicationSpi(), "localPort", 1023);
        checkNegativeSpiProperty(new GridTcpCommunicationSpi(), "localPort", 65636);
        checkNegativeSpiProperty(new GridTcpCommunicationSpi(), "localPortRange", -1);
        checkNegativeSpiProperty(new GridTcpCommunicationSpi(), "idleConnectionTimeout", 0);
        checkNegativeSpiProperty(new GridTcpCommunicationSpi(), "connectionBufferSize", -1);
        checkNegativeSpiProperty(new GridTcpCommunicationSpi(), "connectionBufferFlushFrequency", 0);
        checkNegativeSpiProperty(new GridTcpCommunicationSpi(), "socketReceiveBuffer", -1);
        checkNegativeSpiProperty(new GridTcpCommunicationSpi(), "socketSendBuffer", -1);
        checkNegativeSpiProperty(new GridTcpCommunicationSpi(), "messageQueueLimit", -1);
        checkNegativeSpiProperty(new GridTcpCommunicationSpi(), "sharedMemoryPort", 0);
        checkNegativeSpiProperty(new GridTcpCommunicationSpi(), "sharedMemoryPort", -2);
        checkNegativeSpiProperty(new GridTcpCommunicationSpi(), "reconnectCount", 0);
        checkNegativeSpiProperty(new GridTcpCommunicationSpi(), "selectorsCount", 0);
        checkNegativeSpiProperty(new GridTcpCommunicationSpi(), "minimumBufferedMessageCount", -1);
        checkNegativeSpiProperty(new GridTcpCommunicationSpi(), "bufferSizeRatio", 0);
        checkNegativeSpiProperty(new GridTcpCommunicationSpi(), "connectTimeout", -1);
        checkNegativeSpiProperty(new GridTcpCommunicationSpi(), "maxConnectTimeout", -1);
    }
}
