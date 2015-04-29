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

package org.gridgain.grid.spi.discovery.tcp.ipfinder;

import org.gridgain.grid.spi.*;
import org.gridgain.grid.util.tostring.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.net.*;
import java.util.*;

/**
 * IP finder interface implementation adapter.
 */
public abstract class GridTcpDiscoveryIpFinderAdapter implements GridTcpDiscoveryIpFinder {
    /** Shared flag. */
    private boolean shared;

    /** SPI context. */
    @GridToStringExclude
    private volatile GridSpiContext spiCtx;

    /** {@inheritDoc} */
    @Override public void onSpiContextInitialized(GridSpiContext spiCtx) throws GridSpiException {
        this.spiCtx = spiCtx;
    }

    /** {@inheritDoc} */
    @Override public void onSpiContextDestroyed() {
        spiCtx = null;
    }

    /** {@inheritDoc} */
    @Override public void initializeLocalAddresses(Collection<InetSocketAddress> addrs) throws GridSpiException {
        registerAddresses(addrs);
    }

    /** {@inheritDoc} */
    @Override public boolean isShared() {
        return shared;
    }

    /**
     * Sets shared flag. If {@code true} then it is expected that IP addresses registered
     * with IP finder will be seen by IP finders on all other nodes.
     *
     * @param shared {@code true} if this IP finder is shared.
     */
    @GridSpiConfiguration(optional = true)
    public void setShared(boolean shared) {
        this.shared = shared;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridTcpDiscoveryIpFinderAdapter.class, this);
    }

    /** {@inheritDoc} */
    @Override public void close() {
        // No-op.
    }

    /**
     * @return SPI context.
     */
    protected GridSpiContext spiContext() {
        return spiCtx;
    }
}
