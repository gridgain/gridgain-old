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
 * Cache conflict resolver.
 */
public interface GridCacheConflictResolver {
    /**
     * Whether conflict resolving is needed.
     *
     * @param oldVer Old version.
     * @param newVer New version.
     * @return {@code True} if needed.
     */
    public boolean needResolve(GridCacheVersion oldVer, GridCacheVersion newVer);

    /**
     * Resolve the conflict.
     *
     * @param oldEntry Old entry.
     * @param newEntry New entry.
     * @param atomicVerComparator Whether to use atomic version comparator.
     * @return Conflict resolution context.
     * @throws GridException If failed.
     */
    public <K, V> GridDrReceiverConflictContextImpl<K, V> resolve(GridDrEntryEx<K, V> oldEntry,
        GridDrEntryEx<K, V> newEntry, boolean atomicVerComparator) throws GridException;
}
