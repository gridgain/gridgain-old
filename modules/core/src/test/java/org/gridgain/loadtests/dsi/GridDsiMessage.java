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

package org.gridgain.loadtests.dsi;

import java.io.*;
import java.util.*;

/**
 *
 */
public class GridDsiMessage implements Serializable {
    /** Terminal ID. */
    private String terminalId;

    /** Node ID. */
    private UUID nodeId;

    /**
     * Message constructor.
     *
     * @param terminalId Terminal ID.
     * @param nodeId Node ID.
     */
    public GridDsiMessage(String terminalId, UUID nodeId) {
        this.terminalId = terminalId;
        this.nodeId = nodeId;
    }

    /**
     * @return Terminal ID.
     */
    public String getTerminalId() {
        return terminalId;
    }

    /**
     * Sets terminal ID.
     * @param terminalId Terminal ID.
     */
    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    /**
     * @return Node ID.
     */
    public UUID getNodeId() {
        return nodeId;
    }

    /**
     * Sets node ID.
     *
     * @param nodeId Node ID.
     */
    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }
}
