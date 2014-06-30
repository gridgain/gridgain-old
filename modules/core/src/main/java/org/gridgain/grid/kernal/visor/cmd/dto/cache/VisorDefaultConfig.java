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
 * Data transfer object for default cache configuration properties.
 */
public class VisorDefaultConfig implements Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Default transaction isolation. */
    private GridCacheTxIsolation txIsolation;

    /** Default transaction concurrency. */
    private GridCacheTxConcurrency txConcurrency;

    /** TTL value. */
    private long ttl;

    /** Default transaction concurrency. */
    private long txTimeout;

    /** Default transaction timeout. */
    private long txLockTimeout;

    /** Default query timeout. */
    private long queryTimeout;

    /**
     * @param ccfg Cache configuration.
     * @return Data transfer object for default cache configuration properties.
     */
    public static VisorDefaultConfig from(GridCacheConfiguration ccfg) {
        VisorDefaultConfig cfg = new VisorDefaultConfig();

        cfg.txIsolation(ccfg.getDefaultTxIsolation());
        cfg.txConcurrency(ccfg.getDefaultTxConcurrency());
        cfg.timeToLive(ccfg.getDefaultTimeToLive());
        cfg.txTimeout(ccfg.getDefaultTxTimeout());
        cfg.txLockTimeout(ccfg.getDefaultLockTimeout());
        cfg.queryTimeout(ccfg.getDefaultQueryTimeout());

        return cfg;
    }

    /**
     * @return Default transaction isolation.
     */
    public GridCacheTxIsolation txIsolation() {
        return txIsolation;
    }

    /**
     * @param txIsolation New default transaction isolation.
     */
    public void txIsolation(GridCacheTxIsolation txIsolation) {
        this.txIsolation = txIsolation;
    }

    /**
     * @return Default transaction concurrency.
     */
    public GridCacheTxConcurrency txConcurrency() {
        return txConcurrency;
    }

    /**
     * @param txConcurrency New default transaction concurrency.
     */
    public void txConcurrency(GridCacheTxConcurrency txConcurrency) {
        this.txConcurrency = txConcurrency;
    }

    /**
     * @return TTL value.
     */
    public long timeToLive() {
        return ttl;
    }

    /**
     * @param ttl New tTL value.
     */
    public void timeToLive(long ttl) {
        this.ttl = ttl;
    }

    /**
     * @return Default transaction concurrency.
     */
    public long txTimeout() {
        return txTimeout;
    }

    /**
     * @param txTimeout New default transaction concurrency.
     */
    public void txTimeout(long txTimeout) {
        this.txTimeout = txTimeout;
    }

    /**
     * @return Default transaction timeout.
     */
    public long txLockTimeout() {
        return txLockTimeout;
    }

    /**
     * @param txLockTimeout New default transaction timeout.
     */
    public void txLockTimeout(long txLockTimeout) {
        this.txLockTimeout = txLockTimeout;
    }

    /**
     * @return Default query timeout.
     */
    public long queryTimeout() {
        return queryTimeout;
    }

    /**
     * @param qryTimeout New default query timeout.
     */
    public void queryTimeout(long qryTimeout) {
        queryTimeout = qryTimeout;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(VisorDefaultConfig.class, this);
    }
}
