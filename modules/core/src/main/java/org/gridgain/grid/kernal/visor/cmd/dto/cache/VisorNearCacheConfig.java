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

import org.gridgain.grid.cache.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.io.*;

import static org.gridgain.grid.kernal.visor.cmd.VisorTaskUtils.*;

/**
 * Data transfer object for near cache configuration properties.
 */
public class VisorNearCacheConfig implements Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Flag to enable/disable near cache eviction policy. */
    private boolean nearEnabled;

    /** Near cache start size. */
    private int nearStartSize;

    /** Near cache eviction policy. */
    private String nearEvictPlc;

    /**
     * @param ccfg Cache configuration.
     * @return Data transfer object for near cache configuration properties.
     */
    public static VisorNearCacheConfig from(GridCacheConfiguration ccfg) {
        VisorNearCacheConfig cfg = new VisorNearCacheConfig();

        cfg.nearEnabled(GridCacheUtils.isNearEnabled(ccfg));
        cfg.nearStartSize(ccfg.getNearStartSize());
        cfg.nearEvictPolicy(compactClass(ccfg.getNearEvictionPolicy()));

        return cfg;
    }

    /**
     * @return Flag to enable/disable near cache eviction policy.
     */
    public boolean nearEnabled() {
        return nearEnabled;
    }

    /**
     * @param nearEnabled New flag to enable/disable near cache eviction policy.
     */
    public void nearEnabled(boolean nearEnabled) {
        this.nearEnabled = nearEnabled;
    }

    /**
     * @return Near cache start size.
     */
    public int nearStartSize() {
        return nearStartSize;
    }

    /**
     * @param nearStartSize New near cache start size.
     */
    public void nearStartSize(int nearStartSize) {
        this.nearStartSize = nearStartSize;
    }

    /**
     * @return Near cache eviction policy.
     */
    @Nullable public String nearEvictPolicy() {
        return nearEvictPlc;
    }

    /**
     * @param nearEvictPlc New near cache eviction policy.
     */
    public void nearEvictPolicy(String nearEvictPlc) {
        this.nearEvictPlc = nearEvictPlc;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(VisorNearCacheConfig.class, this);
    }
}
