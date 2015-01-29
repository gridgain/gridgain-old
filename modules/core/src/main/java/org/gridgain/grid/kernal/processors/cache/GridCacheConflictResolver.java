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

package org.gridgain.grid.kernal.processors.cache;

import org.gridgain.grid.*;
import org.gridgain.grid.dr.cache.receiver.*;
import org.gridgain.grid.kernal.processors.cache.dr.*;
import org.gridgain.grid.kernal.processors.dr.*;

import static org.gridgain.grid.dr.cache.receiver.GridDrReceiverCacheConflictResolverMode.*;

/**
 * Real conflict resolver.
 */
public class GridCacheConflictResolver {
    /** Mode. */
    private final GridDrReceiverCacheConflictResolverMode mode;

    /** Resolver. */
    private final GridDrReceiverCacheConflictResolver rslvr;

    /**
     * Constructor.
     *
     * @param mode Mode.
     * @param rslvr Resolver.
     */
    public GridCacheConflictResolver(GridDrReceiverCacheConflictResolverMode mode,
        GridDrReceiverCacheConflictResolver rslvr) {
        assert mode != null;

        this.mode = mode;
        this.rslvr = rslvr;
    }

    /**
     * Resolve the conflict.
     *
     * @param oldEntry Old entry.
     * @param newEntry New entry.
     * @param atomicVerComparator Whether to use atomic version comparator.
     * @return Conflict resolution context.
     * @throws GridException If failed.
     */
    @SuppressWarnings("unchecked")
    public <K, V> GridDrReceiverConflictContextImpl<K, V> resolve(GridDrEntryEx<K, V> oldEntry,
        GridDrEntryEx<K, V> newEntry, boolean atomicVerComparator) throws GridException {
        GridDrReceiverConflictContextImpl<K, V> ctx = new GridDrReceiverConflictContextImpl<>(oldEntry, newEntry);

        if (newEntry.dataCenterId() != oldEntry.dataCenterId() || mode == DR_ALWAYS) {
            assert mode == DR_ALWAYS && rslvr != null || mode == DR_AUTO :
                "Invalid resolver configuration (must be checked on startup) [mode=" + mode + ", rslvr=" + rslvr + ']';

            if (rslvr != null) {
                // Try falling back to user resolver.
                rslvr.resolve(ctx);

                ctx.manualResolve();
            }
            else
                // No other option, but to use new entry.
                ctx.useNew();
        }
        else {
            // Resolve the conflict automatically.
            if (oldEntry.isStartVersion())
                ctx.useNew();
            else {
                if (atomicVerComparator) {
                    // Handle special case when version check using ATOMIC cache comparator is required.
                    if (GridCacheMapEntry.ATOMIC_VER_COMPARATOR.compare(oldEntry.version(), newEntry.version()) >= 0)
                        ctx.useOld();
                    else
                        ctx.useNew();
                }
                else {
                    long topVerDiff = newEntry.topologyVersion() - oldEntry.topologyVersion();

                    if (topVerDiff > 0)
                        ctx.useNew();
                    else if (topVerDiff < 0)
                        ctx.useOld();
                    else if (newEntry.order() > oldEntry.order())
                        ctx.useNew();
                    else
                        ctx.useOld();
                }
            }
        }

        return ctx;
    }
}
