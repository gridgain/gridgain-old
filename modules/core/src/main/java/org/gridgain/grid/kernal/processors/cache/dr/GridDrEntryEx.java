/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal.processors.cache.dr;

import org.gridgain.grid.dr.*;
import org.gridgain.grid.kernal.processors.cache.*;

/**
 * Data center replication entry.
 */
public interface GridDrEntryEx<K, V> extends GridDrEntry<K, V> {
    /**
     * @return Version.
     */
    public GridCacheVersion version();

    /**
     * @return {@code True} entry is new.
     */
    public boolean isStartVersion();
}
