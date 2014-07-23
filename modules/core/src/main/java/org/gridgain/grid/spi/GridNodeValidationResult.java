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

import java.util.*;

/**
 * Result of joining node validation.
 */
public class GridNodeValidationResult {
    /** Offending node ID. */
    private final UUID nodeId;

    /** Error message to be logged locally. */
    private final String msg;

    /** Error message to be sent to joining node. */
    private final String sndMsg;

    /**
     * @param nodeId Offending node ID.
     * @param msg Message logged locally.
     * @param sndMsg Message sent to joining node.
     */
    public GridNodeValidationResult(UUID nodeId, String msg, String sndMsg) {
        this.nodeId = nodeId;
        this.msg = msg;
        this.sndMsg = sndMsg;
    }

    /**
     * @return Offending node ID.
     */
    public UUID nodeId() {
        return nodeId;
    }

    /**
     * @return Message to be logged locally.
     */
    public String message() {
        return msg;
    }

    /**
     * @return Message to be sent to joining node.
     */
    public String sendMessage() {
        return sndMsg;
    }
}
