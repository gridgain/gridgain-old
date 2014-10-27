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

package org.gridgain.grid.kernal.visor.cmd.dto.cache;

import org.gridgain.grid.dr.cache.receiver.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.io.*;

import static org.gridgain.grid.kernal.visor.cmd.VisorTaskUtils.*;

/**
 * Data transfer object for DR receiver cache configuration properties.
 */
public class VisorDrReceiverConfig implements Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Conflict resolver */
    private String conflictResolver;

    /** Conflict resolver mode. */
    private GridDrReceiverCacheConflictResolverMode conflictResolverMode;

    /**
     * @param rcvCfg Data center replication receiver cache configuration.
     * @return Data transfer object for DR receiver cache configuration properties.
     */
    public static VisorDrReceiverConfig from(GridDrReceiverCacheConfiguration rcvCfg) {
        VisorDrReceiverConfig cfg = new VisorDrReceiverConfig();

        cfg.conflictResolver(compactClass(rcvCfg.getConflictResolver()));
        cfg.conflictResolverMode(rcvCfg.getConflictResolverMode());

        return cfg;
    }

    /**
     * @return Conflict resolver
     */
    public String conflictResolver() {
        return conflictResolver;
    }

    /**
     * @param conflictRslvr New conflict resolver
     */
    public void conflictResolver(String conflictRslvr) {
        conflictResolver = conflictRslvr;
    }

    /**
     * @return Conflict resolver mode.
     */
    public GridDrReceiverCacheConflictResolverMode conflictResolverMode() {
        return conflictResolverMode;
    }

    /**
     * @param conflictRslvrMode New conflict resolver mode.
     */
    public void conflictResolverMode(GridDrReceiverCacheConflictResolverMode conflictRslvrMode) {
        conflictResolverMode = conflictRslvrMode;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(VisorDrReceiverConfig.class, this);
    }
}
