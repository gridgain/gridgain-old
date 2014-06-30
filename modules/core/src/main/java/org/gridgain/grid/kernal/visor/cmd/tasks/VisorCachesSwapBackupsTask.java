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

package org.gridgain.grid.kernal.visor.cmd.tasks;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.kernal.processors.task.*;
import org.gridgain.grid.kernal.visor.cmd.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.util.*;

/**
 * Task for swapping backup cache entries.
 */
@GridInternal
public class VisorCachesSwapBackupsTask extends VisorOneNodeTask<Set<String>, Map<String,
    GridBiTuple<Integer, Integer>>> {
    /** */
    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @Override protected VisorCachesSwapBackupsJob job(Set<String> names) {
        return new VisorCachesSwapBackupsJob(names);
    }

    /**
     * Job that swap backups.
     */
    private static class VisorCachesSwapBackupsJob extends VisorJob<Set<String>, Map<String,
        GridBiTuple<Integer, Integer>>> {
        /** */
        private static final long serialVersionUID = 0L;

        /**
         * Create job with specified argument.
         *
         * @param names Job argument.
         */
        private VisorCachesSwapBackupsJob(Set<String> names) {
            super(names);
        }

        /** {@inheritDoc} */
        @Override protected Map<String, GridBiTuple<Integer, Integer>> run(Set<String> names) throws GridException {
            Map<String, GridBiTuple<Integer, Integer>> total = new HashMap<>();

            for (GridCache c: g.cachesx()) {
                String cacheName = c.name();

                if (names.contains(cacheName)) {
                    Set<GridCacheEntry> entries = c.entrySet();

                    int before = entries.size(), after = before;

                    for (GridCacheEntry entry: entries) {
                        if (entry.backup() && entry.evict())
                            after--;
                    }

                    total.put(cacheName, new GridBiTuple<>(before, after));
                }
            }

            return total;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(VisorCachesSwapBackupsJob.class, this);
        }
    }
}
