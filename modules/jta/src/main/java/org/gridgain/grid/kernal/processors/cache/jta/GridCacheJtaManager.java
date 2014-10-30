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

package org.gridgain.grid.kernal.processors.cache.jta;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.jta.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.jetbrains.annotations.*;

import javax.transaction.*;

import static org.gridgain.grid.cache.GridCacheFlag.*;

/**
 * Implementation of {@link GridCacheJtaManagerAdapter}.
 */
public class GridCacheJtaManager<K, V> extends GridCacheJtaManagerAdapter<K, V> {
    /** */
    private final ThreadLocal<GridCacheXAResource> xaRsrc = new ThreadLocal<>();

    /** */
    private TransactionManager jtaTm;

    /** */
    private GridCacheTmLookup tmLookup;

    /** {@inheritDoc} */
    @Override public void createTmLookup(GridCacheConfiguration ccfg) throws GridException {
        assert ccfg.getTransactionManagerLookupClassName() != null;

        try {
            Class<?> cls = Class.forName(ccfg.getTransactionManagerLookupClassName());

            tmLookup = (GridCacheTmLookup)cls.newInstance();
        }
        catch (Exception e) {
            throw new GridException("Failed to instantiate transaction manager lookup.", e);
        }
    }

    /** {@inheritDoc} */
    @Override public void checkJta() throws GridException {
        if (jtaTm == null)
            jtaTm = tmLookup.getTm();

        if (jtaTm != null) {
            GridCacheXAResource rsrc = xaRsrc.get();

            if (rsrc == null || rsrc.isFinished()) {
                try {
                    Transaction jtaTx = jtaTm.getTransaction();

                    if (jtaTx != null) {
                        GridCacheTx tx = cctx.tm().userTx();

                        if (tx == null) {
                            // Start with default concurrency and isolation.
                            GridCacheConfiguration cfg = cctx.config();

                            tx = cctx.tm().onCreated(
                                cctx.cache().newTx(
                                    false,
                                    false,
                                    cfg.getDefaultTxConcurrency(),
                                    cfg.getDefaultTxIsolation(),
                                    cfg.getDefaultTxTimeout(),
                                    cfg.isInvalidate() || cctx.hasFlag(INVALIDATE),
                                    cctx.syncCommit(),
                                    cctx.syncRollback(),
                                    cctx.isSwapOrOffheapEnabled(),
                                    cctx.isStoreEnabled(),
                                    0,
                                    /** group lock keys */null,
                                    /** partition lock */false
                                )
                            );
                        }

                        rsrc = new GridCacheXAResource((GridCacheTxEx)tx, cctx);

                        if (!jtaTx.enlistResource(rsrc))
                            throw new GridException("Failed to enlist XA resource to JTA user transaction.");

                        xaRsrc.set(rsrc);
                    }
                }
                catch (SystemException e) {
                    throw new GridException("Failed to obtain JTA transaction.", e);
                }
                catch (RollbackException e) {
                    throw new GridException("Failed to enlist XAResource to JTA transaction.", e);
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Nullable @Override public Object tmLookup() {
        return tmLookup;
    }
}
