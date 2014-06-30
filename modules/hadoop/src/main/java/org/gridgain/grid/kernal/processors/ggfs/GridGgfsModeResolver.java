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

package org.gridgain.grid.kernal.processors.ggfs;

import org.gridgain.grid.ggfs.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.typedef.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 *
 */
public class GridGgfsModeResolver {
    /** Maximum size of map with cached path modes. */
    private static final int MAX_PATH_CACHE = 1000;

    /** Default mode. */
    private final GridGgfsMode dfltMode;

    /** Modes for particular paths. Ordered from longest to shortest. */
    private ArrayList<T2<GridGgfsPath, GridGgfsMode>> modes;

    /** Cached modes per path. */
    private Map<GridGgfsPath, GridGgfsMode> modesCache;

    /** Cached children modes per path. */
    private Map<GridGgfsPath, Set<GridGgfsMode>> childrenModesCache;

    /**
     * @param dfltMode Default GGFS mode.
     * @param modes List of configured modes.
     */
    public GridGgfsModeResolver(GridGgfsMode dfltMode, @Nullable List<T2<GridGgfsPath, GridGgfsMode>> modes) {
        assert dfltMode != null;

        this.dfltMode = dfltMode;

        if (modes != null) {
            ArrayList<T2<GridGgfsPath, GridGgfsMode>> modes0 = new ArrayList<>(modes);

            // Sort paths, longest first.
            Collections.sort(modes0, new Comparator<Map.Entry<GridGgfsPath, GridGgfsMode>>() {
                @Override public int compare(Map.Entry<GridGgfsPath, GridGgfsMode> o1,
                    Map.Entry<GridGgfsPath, GridGgfsMode> o2) {
                    return o2.getKey().components().size() - o1.getKey().components().size();
                }
            });

            this.modes = modes0;

            modesCache = new GridBoundedConcurrentLinkedHashMap<>(MAX_PATH_CACHE);
            childrenModesCache = new GridBoundedConcurrentLinkedHashMap<>(MAX_PATH_CACHE);
        }
    }

    /**
     * Resolves GGFS mode for the given path.
     *
     * @param path GGFS path.
     * @return GGFS mode.
     */
    public GridGgfsMode resolveMode(GridGgfsPath path) {
        assert path != null;

        if (modes == null)
            return dfltMode;
        else {
            GridGgfsMode mode = modesCache.get(path);

            if (mode == null) {
                for (T2<GridGgfsPath, GridGgfsMode> entry : modes) {
                    if (startsWith(path, entry.getKey())) {
                        // As modes ordered from most specific to least specific first mode found is ours.
                        mode = entry.getValue();

                        break;
                    }
                }

                if (mode == null)
                    mode = dfltMode;

                modesCache.put(path, mode);
            }

            return mode;
        }
    }

    /**
     * @param path Path.
     * @return Set of all modes that children paths could have.
     */
    public Set<GridGgfsMode> resolveChildrenModes(GridGgfsPath path) {
        assert path != null;

        if (modes == null)
            return Collections.singleton(dfltMode);
        else {
            Set<GridGgfsMode> children = childrenModesCache.get(path);

            if (children == null) {
                children = new HashSet<>(GridGgfsMode.values().length, 1.0f);

                GridGgfsMode pathDefault = dfltMode;

                for (T2<GridGgfsPath, GridGgfsMode> child : modes) {
                    if (startsWith(path, child.getKey())) {
                        pathDefault = child.getValue();

                        break;
                    }
                    else if (startsWith(child.getKey(), path))
                        children.add(child.getValue());
                }

                children.add(pathDefault);

                childrenModesCache.put(path, children);
            }

            return children;
        }
    }

    /**
     * @return Unmodifiable copy of properly ordered modes prefixes
     *  or {@code null} if no modes set.
     */
    @Nullable public List<T2<GridGgfsPath, GridGgfsMode>> modesOrdered() {
        return modes != null ? Collections.unmodifiableList(modes) : null;
    }

    /**
     * Check if path starts with prefix.
     *
     * @param path Path.
     * @param prefix Prefix.
     * @return {@code true} if path starts with prefix, {@code false} if not.
     */
    private static boolean startsWith(GridGgfsPath path, GridGgfsPath prefix) {
        List<String> p1Comps = path.components();
        List<String> p2Comps = prefix.components();

        if (p2Comps.size() > p1Comps.size())
            return false;

        for (int i = 0; i < p1Comps.size(); i++) {
            if (i >= p2Comps.size() || p2Comps.get(i) == null)
                // All prefix components already matched.
                return true;

            if (!p1Comps.get(i).equals(p2Comps.get(i)))
                return false;
        }

        // Path and prefix components had same length and all of them matched.
        return true;
    }
}
