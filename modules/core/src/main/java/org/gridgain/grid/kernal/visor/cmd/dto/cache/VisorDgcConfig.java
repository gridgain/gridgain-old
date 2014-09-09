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
import org.gridgain.grid.util.typedef.internal.*;

import java.io.*;

/**
 * Data transfer object for DGC configuration properties.
 */
public class VisorDgcConfig implements Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    /** DGC check frequency. */
    private long freq;

    /** DGC remove locks flag. */
    private boolean rmvLocks;

    /** Timeout for considering lock to be suspicious. */
    private long suspectLockTimeout;

    /**
     * @param ccfg Cache configuration.
     * @return Data transfer object for DGC configuration properties.
     */
    public static VisorDgcConfig from(GridCacheConfiguration ccfg) {
        VisorDgcConfig cfg = new VisorDgcConfig();

        cfg.frequency(ccfg.getDgcFrequency());
        cfg.removedLocks(ccfg.isDgcRemoveLocks());
        cfg.suspectLockTimeout(ccfg.getDgcSuspectLockTimeout());

        return cfg;
    }

    /**
     * @return DGC check frequency.
     */
    public long frequency() {
        return freq;
    }

    /**
     * @param freq New dGC check frequency.
     */
    public void frequency(long freq) {
        this.freq = freq;
    }

    /**
     * @return DGC remove locks flag.
     */
    public boolean removedLocks() {
        return rmvLocks;
    }

    /**
     * @param rmvLocks New dGC remove locks flag.
     */
    public void removedLocks(boolean rmvLocks) {
        this.rmvLocks = rmvLocks;
    }

    /**
     * @return Timeout for considering lock to be suspicious.
     */
    public long suspectLockTimeout() {
        return suspectLockTimeout;
    }

    /**
     * @param suspectLockTimeout New timeout for considering lock to be suspicious.
     */
    public void suspectLockTimeout(long suspectLockTimeout) {
        this.suspectLockTimeout = suspectLockTimeout;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(VisorDgcConfig.class, this);
    }
}
