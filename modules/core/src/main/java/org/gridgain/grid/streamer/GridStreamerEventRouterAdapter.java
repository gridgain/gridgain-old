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

package org.gridgain.grid.streamer;

import org.gridgain.grid.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.typedef.*;

import java.util.*;

/**
 * Streamer adapter for event routers.
 */
public abstract class GridStreamerEventRouterAdapter implements GridStreamerEventRouter {
    /** {@inheritDoc} */
    @Override public <T> Map<GridNode, Collection<T>> route(GridStreamerContext ctx, String stageName,
        Collection<T> evts) {
        if (evts.size() == 1) {
            GridNode route = route(ctx, stageName, F.first(evts));

            if (route == null)
                return null;

            return Collections.singletonMap(route, evts);
        }

        Map<GridNode, Collection<T>> map = new GridLeanMap<>();

        for (T e : evts) {
            GridNode n = route(ctx, stageName, e);

            if (n == null)
                return null;

            Collection<T> mapped = map.get(n);

            if (mapped == null)
                map.put(n, mapped = new ArrayList<>());

            mapped.add(e);
        }

        return map;
    }
}
