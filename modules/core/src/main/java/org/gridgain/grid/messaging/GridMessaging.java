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

package org.gridgain.grid.messaging;

import org.gridgain.grid.*;
import org.gridgain.grid.lang.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Provides functionality for topic-based message exchange among nodes defined by {@link #projection()}.
 * Users can send ordered and unordered messages to various topics. Note that same topic name
 * cannot be reused between ordered and unordered messages. Instance of {@code GridMessaging}
 * is obtained from grid projection as follows:
 * <pre name="code" class="java">
 * GridMessaging m = GridGain.grid().message();
 * </pre>
 * <p>
 * There are {@code 2} ways to subscribe to message listening, {@code local} and {@code remote}.
 * <p>
 * Local subscription, defined by {@link #localListen(Object, GridBiPredicate)} method, will add
 * a listener for a given topic on local node only. This listener will be notified whenever any
 * node within grid projection will send a message for a given topic to this node. Local listen
 * subscription will happen regardless of whether local node belongs to this grid projection or not.
 * <p>
 * Remote subscription, defined by {@link #remoteListen(Object, GridBiPredicate)}, will add a
 * message listener for a given topic to all nodes in the projection (possibly including this node if
 * it belongs to the projection as well). This means that any node within this grid projection can send
 * a message for a given topic and all nodes within projection will receive listener notification.
 * <h1 class="header">Ordered vs Unordered</h1>
 * GridGain allows for sending ordered messages (see {@link #sendOrdered(Object, Object, long)}), i.e.
 * messages will be received in the same order they were sent. Ordered messages have a {@code timeout}
 * parameter associated with them which specifies how long an out-of-order message will stay in a queue,
 * waiting for messages that are ordered ahead of it to arrive. If timeout expires, then all ordered
 * messages for a given topic that have not arrived yet will be skipped. When (and if) expired messages
 * actually do arrive, they will be ignored.
 */
public interface GridMessaging {
    /**
     * Gets grid projection to which this {@code GridMessaging} instance belongs.
     *
     * @return Grid projection to which this {@code GridMessaging} instance belongs.
     */
    public GridProjection projection();

    /**
     * Sends given message with specified topic to the nodes in this projection.
     *
     * @param topic Topic to send to, {@code null} for default topic.
     * @param msg Message to send.
     * @throws GridException If failed to send a message to any of the nodes.
     * @throws GridEmptyProjectionException Thrown in case when this projection is empty.
     */
    public void send(@Nullable Object topic, Object msg) throws GridException;

    /**
     * Sends given messages with specified topic to the nodes in this projection.
     *
     * @param topic Topic to send to, {@code null} for default topic.
     * @param msgs Messages to send. Order of the sending is undefined. If the method produces
     *      the exception none or some messages could have been sent already.
     * @throws GridException If failed to send a message to any of the nodes.
     * @throws GridEmptyProjectionException Thrown in case when this projection is empty.
     */
    public void send(@Nullable Object topic, Collection<?> msgs) throws GridException;

    /**
     * Sends given message with specified topic to the nodes in this projection. Messages sent with
     * this method will arrive in the same order they were sent. Note that if a topic is used
     * for ordered messages, then it cannot be reused for non-ordered messages.
     * <p>
     * The {@code timeout} parameter specifies how long an out-of-order message will stay in a queue,
     * waiting for messages that are ordered ahead of it to arrive. If timeout expires, then all ordered
     * messages that have not arrived before this message will be skipped. When (and if) expired messages
     * actually do arrive, they will be ignored.
     *
     * @param topic Topic to send to, {@code null} for default topic.
     * @param msg Message to send.
     * @param timeout Message timeout in milliseconds, {@code 0} for default
     *      which is {@link GridConfiguration#getNetworkTimeout()}.
     * @throws GridException If failed to send a message to any of the nodes.
     * @throws GridEmptyProjectionException Thrown in case when this projection is empty.
     */
    public void sendOrdered(@Nullable Object topic, Object msg, long timeout) throws GridException;

    /**
     * Adds local listener for given topic on local node only. This listener will be notified whenever any
     * node within grid projection will send a message for a given topic to this node. Local listen
     * subscription will happen regardless of whether local node belongs to this grid projection or not.
     *
     * @param topic Topic to subscribe to.
     * @param p Predicate that is called on each received message. If predicate returns {@code false},
     *      then it will be unsubscribed from any further notifications.
     */
    public void localListen(@Nullable Object topic, GridBiPredicate<UUID, ?> p);

    /**
     * Unregisters local listener for given topic on local node only.
     *
     * @param topic Topic to unsubscribe from.
     * @param p Listener predicate.
     */
    public void stopLocalListen(@Nullable Object topic, GridBiPredicate<UUID, ?> p);

    /**
     * Adds a message listener for a given topic to all nodes in the projection (possibly including
     * this node if it belongs to the projection as well). This means that any node within this grid
     * projection can send a message for a given topic and all nodes within projection will receive
     * listener notification.
     *
     * @param topic Topic to subscribe to, {@code null} means default topic.
     * @param p Predicate that is called on each node for each received message. If predicate returns {@code false},
     *      then it will be unsubscribed from any further notifications.
     * @return Future that finishes when all listeners are registered. It returns {@code operation ID}
     *      that can be passed to {@link #stopRemoteListen(UUID)} method to stop listening.
     */
    public GridFuture<UUID> remoteListen(@Nullable Object topic, GridBiPredicate<UUID, ?> p);

    /**
     * Unregisters all listeners identified with provided operation ID on all nodes in this projection.
     *
     * @param opId Listen ID that was returned from {@link #remoteListen(Object, GridBiPredicate)} method.
     * @return Future that finishes when all listeners are unregistered.
     */
    public GridFuture<?> stopRemoteListen(UUID opId);
}
