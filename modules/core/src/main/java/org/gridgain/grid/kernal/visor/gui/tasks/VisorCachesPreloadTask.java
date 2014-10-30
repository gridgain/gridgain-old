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
import org.gridgain.grid.kernal.processors.task.*;
import org.gridgain.grid.kernal.visor.cmd.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.util.*;

/**
 * Pre-loads caches. Made callable just to conform common pattern.
 */
@GridInternal
public class VisorCachesPreloadTask extends VisorOneNodeTask<Set<String>, Void> {
    /** */
    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @Override protected VisorCachesPreloadJob job(Set<String> arg) {
        return new VisorCachesPreloadJob(arg);
    }

    /**
     * Job that preload caches.
     */
    private static class VisorCachesPreloadJob extends VisorJob<Set<String>, Void> {
        /** */
        private static final long serialVersionUID = 0L;

        /**
         * @param arg Caches names.
         */
        private VisorCachesPreloadJob(Set<String> arg) {
            super(arg);
        }

        /** {@inheritDoc} */
        @Override protected Void run(Set<String> cacheNames) throws GridException {
            Collection<GridFuture<?>> futs = new ArrayList<>();

            for(GridCache c : g.cachesx()) {
                if (cacheNames.contains(c.name()))
                    futs.add(c.forceRepartition());
            }

            for (GridFuture f: futs)
                f.get();

            return null;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(VisorCachesPreloadJob.class, this);
        }
    }
}
