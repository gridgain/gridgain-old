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
 * Result of hash id resolvers validation.
 */
public class GridHashIdResolversValidationResult {
    /** Offending cache name. */
    private String cacheName;

    /** Hash ID resolver class name. */
    private String rslvrCls;

    /** Offending node ID. */
    private UUID nodeId;

    /**
     * @param cacheName Offending cache name.
     * @param rslvrCls Hash ID resolver class name.
     * @param nodeId Offending node ID.
     */
    public GridHashIdResolversValidationResult(String cacheName, String rslvrCls, UUID nodeId) {
        this.cacheName = cacheName;
        this.rslvrCls = rslvrCls;
        this.nodeId = nodeId;
    }


    /**
     * @return Offending cache name.
     */
    public String cacheName() {
        return cacheName;
    }

    /**
     * @return Hash ID resolver class name.
     */
    public String resolverClass() {
        return rslvrCls;
    }

    /**
     * @return Offending node ID.
     */
    public UUID nodeId() {
        return nodeId;
    }
}
