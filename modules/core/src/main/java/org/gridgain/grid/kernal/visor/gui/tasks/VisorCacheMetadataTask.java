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

package org.gridgain.grid.kernal.visor.gui.tasks;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.kernal.processors.cache.query.*;
import org.gridgain.grid.kernal.processors.task.*;
import org.gridgain.grid.kernal.visor.cmd.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;

/**
 * Task to get cache SQL metadata.
 */
@GridInternal
public class VisorCacheMetadataTask extends VisorOneNodeTask<String, GridCacheSqlMetadata> {
    /** */
    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @Override protected VisorCacheMetadataJob job(String arg) {
        return new VisorCacheMetadataJob(arg);
    }

    /**
     * Job to get cache SQL metadata.
     */
    private static class VisorCacheMetadataJob extends VisorJob<String, GridCacheSqlMetadata> {
        /** */
        private static final long serialVersionUID = 0L;

        /**
         * @param arg Cache name to take metadata.
         */
        private VisorCacheMetadataJob(String arg) {
            super(arg);
        }

        /** {@inheritDoc} */
        @Override protected GridCacheSqlMetadata run(String cacheName) throws GridException {
            GridCache<Object, Object> cache = g.cachex(cacheName);

            if (cache != null) {
                GridCacheQueriesEx<Object, Object> queries = (GridCacheQueriesEx<Object, Object>) cache.queries();

                return F.first(queries.sqlMetadata());
            }

            throw new GridException("Cache not found: " + cacheName);
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(VisorCacheMetadataJob.class, this);
        }
    }
}
