/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal.processors.cache;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.processors.cache.dr.*;
import org.gridgain.grid.kernal.processors.dr.*;

/**
 * Conflict resolver which is used when neither DR, not local store are configured.
 */
public class GridCacheNoopConflictResolver implements GridCacheConflictResolver {
    /** {@inheritDoc} */
    @Override public boolean needResolve(GridCacheVersion oldVer, GridCacheVersion newVer) {
        return false;
    }

    /** {@inheritDoc} */
    @Override public <K, V> GridDrReceiverConflictContextImpl<K, V> resolve(GridDrEntryEx<K, V> oldEntry,
        GridDrEntryEx<K, V> newEntry, boolean atomicVerComparator) throws GridException {
        assert false : "Should not reach this place.";

        return null;
    }
}
