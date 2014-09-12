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

package org.gridgain.grid.spi.discovery;

import org.gridgain.grid.*;
import org.gridgain.grid.events.*;
import org.gridgain.grid.product.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Grid discovery SPI allows to discover remote nodes in grid.
 * <p>
 * The default discovery SPI is {@link GridTcpDiscoverySpi}
 * with default configuration which allows all nodes in local network
 * (with enabled multicast) to discover each other.
 * <p>
 * Gridgain provides the following {@code GridDeploymentSpi} implementation:
 * <ul>
 * <li>{@link GridTcpDiscoverySpi}</li>
 * </ul>
 * <b>NOTE:</b> this SPI (i.e. methods in this interface) should never be used directly. SPIs provide
 * internal view on the subsystem and is used internally by GridGain kernal. In rare use cases when
 * access to a specific implementation of this SPI is required - an instance of this SPI can be obtained
 * via {@link Grid#configuration()} method to check its configuration properties or call other non-SPI
 * methods. Note again that calling methods from this interface on the obtained instance can lead
 * to undefined behavior and explicitly not supported.
 */
public interface GridDiscoverySpi extends GridSpi {
    /**
     * Gets collection of remote nodes in grid or empty collection if no remote nodes found.
     *
     * @return Collection of remote nodes.
     */
    public Collection<GridNode> getRemoteNodes();

    /**
     * Gets local node.
     *
     * @return Local node.
     */
    public GridNode getLocalNode();

    /**
     * Gets node by ID.
     *
     * @param nodeId Node ID.
     * @return Node with given ID or {@code null} if node is not found.
     */
    @Nullable public GridNode getNode(UUID nodeId);

    /**
     * Pings the remote node to see if it's alive.
     *
     * @param nodeId Node Id.
     * @return {@code true} if node alive, {@code false} otherwise.
     */
    public boolean pingNode(UUID nodeId);

    /**
     * Sets node attributes and node version which will be distributed in grid during
     * join process. Note that these attributes cannot be changed and set only once.
     *
     * @param attrs Map of node attributes.
     * @param ver Product version.
     */
    public void setNodeAttributes(Map<String, Object> attrs, GridProductVersion ver);

    /**
     * Sets a listener for discovery events. Refer to
     * {@link GridDiscoveryEvent} for a set of all possible
     * discovery events.
     * <p>
     * Note that as of GridGain 3.0.2 this method is called <b>before</b>
     * method {@link #spiStart(String)} is called. This is done to
     * avoid potential window when SPI is started but the listener is
     * not registered yet.
     *
     * @param lsnr Listener to discovery events or {@code null} to unset the listener.
     */
    public void setListener(@Nullable GridDiscoverySpiListener lsnr);

    /**
     * Sets a handler for initial data exchange between GridGain nodes.
     *
     * @param exchange Discovery data exchange handler.
     */
    public void setDataExchange(GridDiscoverySpiDataExchange exchange);

    /**
     * Sets discovery metrics provider. Use metrics provided by
     * {@link GridDiscoveryMetricsProvider#getMetrics()} method to exchange
     * dynamic metrics between nodes.
     *
     * @param metricsProvider Provider of metrics data.
     */
    public void setMetricsProvider(GridDiscoveryMetricsProvider metricsProvider);

    /**
     * Tells discovery SPI to disconnect from topology. This is very close to calling
     * {@link #spiStop()} with accounting that it is not a full stop,
     * but disconnect before reconnect (i.e. some internal threads and
     * resources may be preserved on disconnect and reused on reconnect again).
     *
     * @throws GridSpiException If any error occurs.
     * @see #reconnect()
     */
    @Deprecated
    public void disconnect() throws GridSpiException;

    /**
     * Tells discovery SPI to reconnect to topology. This is very close to calling
     * {@link #spiStart(String)} with accounting that it is not a full start,
     * but reconnect (i.e. some internal threads and resources may be preserved
     * on disconnect and reused on reconnect again).
     * <p>
     * Prior to calling this method manager will ensure, that it is in the correct
     * network segment.
     *
     * @throws GridSpiException If any error occurs.
     * @see #disconnect()
     * @deprecated This method is unsupported in current version of GridGain and
     *      will be removed in the next major release.
     */
    @Deprecated
    public void reconnect() throws GridSpiException;

    /**
     * Sets discovery SPI node authenticator. This method is called before SPI start() method.
     *
     * @param auth Discovery SPI authenticator.
     */
    public void setAuthenticator(GridDiscoverySpiNodeAuthenticator auth);

    /**
     * Gets start time of the very first node in the grid. This value should be the same
     * on all nodes in the grid and it should not change even if very first node fails
     * of leaves grid.
     *
     * @return Start time of the first node in grid or {@code 0} if SPI implementation
     *         does not support this method.
     */
    public long getGridStartTime();
}
