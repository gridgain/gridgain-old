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

package org.gridgain.client.integration;

import org.gridgain.client.*;
import org.gridgain.client.marshaller.*;
import org.gridgain.client.ssl.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.*;

/**
 * Tests TCP binary protocol with client when SSL is enabled.
 */
public class GridClientTcpSslSelfTest extends GridClientAbstractSelfTest {
    /** {@inheritDoc} */
    @Override protected GridClientProtocol protocol() {
        return GridClientProtocol.TCP;
    }

    /** {@inheritDoc} */
    @Override protected String serverAddress() {
        return HOST + ":" + BINARY_PORT;
    }

    /** {@inheritDoc} */
    @Override protected boolean useSsl() {
        return true;
    }

    /** {@inheritDoc} */
    @Override protected GridSslContextFactory sslContextFactory() {
        return GridTestUtils.sslContextFactory();
    }

    /**
     * Checks if incorrect marshaller configuration leads to
     * handshake error.
     *
     * @throws Exception If failed.
     */
    public void testHandshakeFailed() throws Exception {
        GridClientConfiguration cfg = clientConfiguration();

        cfg.setMarshaller(new GridClientMarshaller() {
            @Override public byte[] marshal(Object obj) {
                throw new UnsupportedOperationException();
            }

            @Override public <T> T unmarshal(byte[] bytes) {
                throw new UnsupportedOperationException();
            }

            @Override public byte getProtocolId() {
                return 42; // Non-existent marshaller ID.
            }
        });

        GridClient c = GridClientFactory.start(cfg);

        Exception err = null;

        try {
            c.compute().refreshTopology(false, false);
        }
        catch (Exception e) {
            err = e;
        }

        assertNotNull(err);
        assertTrue(X.hasCause(err, GridClientHandshakeException.class));
    }
}
