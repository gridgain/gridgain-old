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
package org.gridgain.grid.kernal.processors.rest.protocols.tcp;

import org.gridgain.grid.util.lang.*;
import org.gridgain.grid.util.nio.*;

import java.net.*;

/**
 * Mock nio session with disabled functionality for testing parser.
 */
public class GridMockNioSession extends GridMetadataAwareAdapter implements GridNioSession {
    /** Local address */
    private InetSocketAddress locAddr = new InetSocketAddress(0);

    /** Remote address. */
    private InetSocketAddress rmtAddr = new InetSocketAddress(0);

    /**
     * Creates empty mock session.
     */
    public GridMockNioSession() {
        // No-op.
    }

    /**
     * Creates new mock session with given addresses.
     *
     * @param locAddr Local address.
     * @param rmtAddr Remote address.
     */
    public GridMockNioSession(InetSocketAddress locAddr, InetSocketAddress rmtAddr) {
        this();

        this.locAddr = locAddr;
        this.rmtAddr = rmtAddr;
    }

    /** {@inheritDoc} */
    @Override public InetSocketAddress localAddress() {
        return locAddr;
    }

    /** {@inheritDoc} */
    @Override public InetSocketAddress remoteAddress() {
        return rmtAddr;
    }

    /** {@inheritDoc} */
    @Override public long bytesSent() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override public long bytesReceived() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override public long createTime() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override public long closeTime() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override public long lastReceiveTime() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override public long lastSendTime() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override public long lastSendScheduleTime() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override public GridNioFuture<Boolean> close() {
        return new GridNioFinishedFuture<>(true);
    }

    /** {@inheritDoc} */
    @Override public GridNioFuture<?> send(Object msg) {
        return new GridNioFinishedFuture<>(true);
    }

    /** {@inheritDoc} */
    @Override public <T> T meta(int key) {
        return meta(String.valueOf(key));
    }

    /** {@inheritDoc} */
    @Override public <T> T addMeta(int key, T val) {
        return addMeta(String.valueOf(key), val);
    }

    /** {@inheritDoc} */
    @Override public <T> T removeMeta(int key) {
        return removeMeta(String.valueOf(key));
    }

    /** {@inheritDoc} */
    @Override public GridNioFuture<Object> resumeReads() {
        return null;
    }

    /** {@inheritDoc} */
    @Override public GridNioFuture<Object> pauseReads() {
        return null;
    }

    /** {@inheritDoc} */
    @Override public boolean accepted() {
        return false;
    }

    /** {@inheritDoc} */
    @Override public boolean readsPaused() {
        return false;
    }
}
