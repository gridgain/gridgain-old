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

import org.gridgain.grid.*;
import org.gridgain.grid.events.*;
import org.gridgain.grid.kernal.managers.communication.*;
import org.gridgain.grid.kernal.managers.eventstorage.*;
import org.gridgain.grid.security.*;
import org.gridgain.grid.spi.communication.*;
import org.gridgain.grid.spi.discovery.*;
import org.gridgain.grid.spi.swapspace.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.nio.*;
import java.util.*;

/**
 * SPI context provides common functionality for all SPI implementations.
 */
public interface GridSpiContext {
    /**
     * Gets a collection of remote grid nodes. Remote nodes are discovered via underlying
     * {@link GridDiscoverySpi} implementation used. Unlike {@link #nodes()},
     * this method does not include local grid node.
     *
     * @return Collection of remote grid nodes.
     * @see #localNode()
     * @see #nodes()
     * @see GridDiscoverySpi
     */
    public Collection<GridNode> remoteNodes();

    /**
     * Gets a collection of all grid nodes. Remote nodes are discovered via underlying
     * {@link GridDiscoverySpi} implementation used. Unlike {@link #remoteNodes()},
     * this method does include local grid node.
     *
     * @return Collection of remote grid nodes.
     * @see #localNode()
     * @see #remoteNodes()
     * @see GridDiscoverySpi
     */
    public Collection<GridNode> nodes();

    /**
     * Gets local grid node. Instance of local node is provided by underlying {@link GridDiscoverySpi}
     * implementation used.
     *
     * @return Local grid node.
     * @see GridDiscoverySpi
     */
    public GridNode localNode();

    /**
     * Gets a collection of all remote daemon nodes in topology. The daemon nodes are discovered via
     * underlying {@link GridDiscoverySpi} implementation used.
     *
     * @return Collection of all daemon nodes.
     * @see #localNode()
     * @see #remoteNodes()
     * @see #nodes()
     * @see GridDiscoverySpi
     */
    public Collection<GridNode> remoteDaemonNodes();

    /**
     * Gets a node instance based on its ID.
     *
     * @param nodeId ID of a node to get.
     * @return Node for a given ID or {@code null} is such not has not been discovered.
     * @see GridDiscoverySpi
     */
    @Nullable public GridNode node(UUID nodeId);

    /**
     * Pings a remote node. The underlying communication is provided via
     * {@link GridDiscoverySpi#pingNode(UUID)} implementation.
     * <p>
     * Discovery SPIs usually have some latency in discovering failed nodes. Hence,
     * communication to remote nodes may fail at times if an attempt was made to
     * establish communication with a failed node. This method can be used to check
     * if communication has failed due to node failure or due to some other reason.
     *
     * @param nodeId ID of a node to ping.
     * @return {@code true} if node for a given ID is alive, {@code false} otherwise.
     * @see GridDiscoverySpi
     */
    public boolean pingNode(UUID nodeId);

    /**
     * Sends a message to a remote node. The underlying communication mechanism is defined by
     * {@link GridCommunicationSpi} implementation used.
     *
     * @param node Node to send a message to.
     * @param msg Message to send.
     * @param topic Topic to send message to.
     * @throws GridSpiException If failed to send a message to remote node.
     */
    public void send(GridNode node, Serializable msg, String topic) throws GridSpiException;

    /**
     * Register a message listener to receive messages sent by remote nodes. The underlying
     * communication mechanism is defined by {@link GridCommunicationSpi} implementation used.
     * <p>
     * This method can be used by jobs to communicate with other nodes in the grid. Remote nodes
     * can send messages by calling {@link #send(GridNode, Serializable, String)} method.
     *
     * @param lsnr Message listener to register.
     * @param topic Topic to register listener for.
     */
    public void addMessageListener(GridMessageListener lsnr, String topic);

    /**
     * Removes a previously registered message listener.
     *
     * @param lsnr Message listener to remove.
     * @param topic Topic to unregister listener for.
     * @return {@code true} of message listener was removed, {@code false} if it was not
     *      previously registered.
     */
    public boolean removeMessageListener(GridMessageListener lsnr, String topic);

    /**
     * Adds an event listener for local events.
     *
     * @param lsnr Event listener for local events.
     * @param types Optional types for which this listener will be notified. If no types are provided
     *      this listener will be notified for all local events.
     * @see GridEvent
     */
    public void addLocalEventListener(GridLocalEventListener lsnr, int... types);

    /**
     * Removes local event listener.
     *
     * @param lsnr Local event listener to remove.
     * @return {@code true} if listener was removed, {@code false} otherwise.
     */
    public boolean removeLocalEventListener(GridLocalEventListener lsnr);

    /**
     * Checks whether all provided event types are recordable.
     *
     * @param types Event types to check.
     * @return Whether or not all provided event types are recordable..
     */
    public boolean isEventRecordable(int... types);

    /**
     * Records local event.
     *
     * @param evt Local grid event to record.
     */
    public void recordEvent(GridEvent evt);

    /**
     * Registers open port.
     *
     * @param port Port.
     * @param proto Protocol.
     */
    public void registerPort(int port, GridPortProtocol proto);

    /**
     * Deregisters closed port.
     *
     * @param port Port.
     * @param proto Protocol.
     */
    public void deregisterPort(int port, GridPortProtocol proto);

    /**
     * Deregisters all closed ports.
     */
    public void deregisterPorts();

    /**
     * Gets object from cache.
     *
     * @param cacheName Cache name.
     * @param key Object key.
     * @return Cached object.
     * @throws GridException Thrown if any exception occurs.
     */
    @Nullable public <K, V> V get(String cacheName, K key) throws GridException;

    /**
     * Puts object in cache.
     *
     * @param cacheName Cache name.
     * @param key Object key.
     * @param val Cached object.
     * @param ttl Time to live, {@code 0} means the entry will never expire.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return Previous value associated with specified key, possibly {@code null}.
     * @throws GridException Thrown if any exception occurs.
     */
    @Nullable public <K, V> V put(String cacheName, K key, V val, long ttl) throws GridException;

    /**
     * Puts object into cache if there was no previous object associated with
     * given key.
     *
     * @param cacheName Cache name.
     * @param key Cache key.
     * @param val Cache value.
     * @param ttl Time to live.
     * @param <K> Cache key type.
     * @param <V> Cache value type.
     * @return Either existing value or {@code null} if there was no value for given key.
     * @throws GridException If put failed.
     */
    @Nullable public <K, V> V putIfAbsent(String cacheName, K key, V val, long ttl) throws GridException;

    /**
     * Removes object from cache.
     *
     * @param cacheName Cache name.
     * @param key Object key.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return Previous value associated with specified key, possibly {@code null}.
     * @throws GridException Thrown if any exception occurs.
     */
    @Nullable public <K, V> V remove(String cacheName, K key) throws GridException;

    /**
     * Returns {@code true} if this cache contains a mapping for the specified key.
     *
     * @param cacheName Cache name.
     * @param key Object key.
     * @param <K> Key type.
     * @return {@code true} if this cache contains a mapping for the specified key.
     */
    public <K> boolean containsKey(String cacheName, K key);

    /**
     * Writes object to swap.
     *
     * @param spaceName Swap space name.
     * @param key Key.
     * @param val Value.
     * @param ldr Class loader (optional).
     * @throws GridException If any exception occurs.
     */
    public void writeToSwap(String spaceName, Object key, @Nullable Object val, @Nullable ClassLoader ldr)
        throws GridException;

    /**
     * Reads object from swap.
     *
     * @param spaceName Swap space name.
     * @param key Key.
     * @param ldr Class loader (optional).
     * @return Swapped value.
     * @throws GridException If any exception occurs.
     */
    @Nullable public <T> T readFromSwap(String spaceName, GridSwapKey key, @Nullable ClassLoader ldr)
        throws GridException;


    /**
     * Reads object from off-heap.
     *
     * @param spaceName Off-heap space name.
     * @param part Partition.
     * @param key Key.
     * @param keyBytes Key bytes.
     * @param ldr Class loader for unmarshalling.
     * @return Value.
     * @throws GridException If failed.
     */
    @Nullable public <T> T readFromOffheap(@Nullable String spaceName, int part, Object key, @Nullable byte[] keyBytes,
        @Nullable ClassLoader ldr) throws GridException;

    /**
     * Writes data to off-heap memory.
     *
     * @param spaceName Off-heap space name.
     * @param part Partition.
     * @param key Key.
     * @param keyBytes Optional key bytes.
     * @param val Value.
     * @param valBytes Optional value bytes.
     * @param ldr Class loader.
     * @throws GridException If failed.
     */
    public void writeToOffheap(@Nullable String spaceName, int part, Object key, @Nullable byte[] keyBytes, Object val,
        @Nullable byte[] valBytes, @Nullable ClassLoader ldr) throws GridException;

    /**
     * Removes data from off-heap memory.
     *
     * @param spaceName Off-heap space name.
     * @param part Partition.
     * @param key Key.
     * @param keyBytes Optional key bytes.
     * @return {@code true} If succeeded.
     * @throws GridException If failed.
     */
    public boolean removeFromOffheap(@Nullable String spaceName, int part, Object key, @Nullable byte[] keyBytes)
        throws GridException;

    /**
     * Calculates partition number for given key.
     *
     * @param cacheName Cache name.
     * @param key Key.
     * @return Partition.
     */
    public int partition(String cacheName, Object key);

    /**
     * Removes object from swap.
     *
     * @param spaceName Swap space name.
     * @param key Key.
     * @param ldr Class loader (optional).
     * @throws GridException If any exception occurs.
     */
    public void removeFromSwap(String spaceName, Object key, @Nullable ClassLoader ldr) throws GridException;

    /**
     * Validates that new node can join grid topology, this method is called on coordinator
     * node before new node joins topology.
     *
     * @param node Joining node.
     * @return Validation result or {@code null} in case of success.
     */
    @Nullable public GridNodeValidationResult validateNode(GridNode node);

    /**
     * Writes delta for provided node and message type.
     *
     * @param nodeId Node ID.
     * @param msgCls Message type.
     * @param buf Buffer to write to.
     * @return Whether delta was fully written.
     */
    public boolean writeDelta(UUID nodeId, Class<?> msgCls, ByteBuffer buf);

    /**
     * Reads delta for provided node and message type.
     *
     * @param nodeId Node ID.
     * @param msgCls Message type.
     * @param buf Buffer to read from.
     * @return Whether delta was fully read.
     */
    public boolean readDelta(UUID nodeId, Class<?> msgCls, ByteBuffer buf);

    /**
     * Gets collection of authenticated subjects together with their permissions.
     *
     * @return Collection of authenticated subjects.
     * @throws GridException If any exception occurs.
     */
    public Collection<GridSecuritySubject> authenticatedSubjects() throws GridException;

    /**
     * Gets security subject based on subject ID.
     *
     * @param subjId Subject ID.
     * @return Authorized security subject.
     * @throws GridException If any exception occurs.
     */
    public GridSecuritySubject authenticatedSubject(UUID subjId) throws GridException;

    /**
     * Reads swapped cache value from off-heap and swap.
     *
     * @param spaceName Off-heap space name.
     * @param key Key.
     * @param ldr Class loader for unmarshalling.
     * @return Value.
     * @throws GridException If any exception occurs.
     */
    @Nullable public <T> T readValueFromOffheapAndSwap(@Nullable String spaceName, Object key,
        @Nullable ClassLoader ldr) throws GridException;
}
