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

package org.gridgain.grid.spi.discovery.tcp.messages;

import org.gridgain.grid.util.typedef.internal.*;

import java.io.*;
import java.util.*;

/**
 * Sent by node that has detected node failure to coordinator across the ring,
 * then sent by coordinator across the ring.
 */
@GridTcpDiscoveryEnsureDelivery
public class GridTcpDiscoveryNodeFailedMessage extends GridTcpDiscoveryAbstractMessage {
    /** */
    private static final long serialVersionUID = 0L;

    /** ID of the failed node. */
    private UUID failedNodeId;

    /** Internal order of the failed node. */
    private long order;

    /**
     * Public default no-arg constructor for {@link Externalizable} interface.
     */
    public GridTcpDiscoveryNodeFailedMessage() {
        // No-op.
    }

    /**
     * Constructor.
     *
     * @param creatorNodeId ID of the node that detects nodes failure.
     * @param failedNodeId ID of the failed nodes.
     * @param order Order of the failed node.
     */
    public GridTcpDiscoveryNodeFailedMessage(UUID creatorNodeId, UUID failedNodeId, long order) {
        super(creatorNodeId);

        assert failedNodeId != null;
        assert order > 0;

        this.failedNodeId = failedNodeId;
        this.order = order;
    }

    /**
     * Gets ID of the failed node.
     *
     * @return ID of the failed node.
     */
    public UUID failedNodeId() {
        return failedNodeId;
    }

    /**
     * @return Internal order of the failed node.
     */
    public long order() {
        return order;
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);

        U.writeUuid(out, failedNodeId);
        out.writeLong(order);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);

        failedNodeId = U.readUuid(in);
        order = in.readLong();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridTcpDiscoveryNodeFailedMessage.class, this, "super", super.toString());
    }
}
