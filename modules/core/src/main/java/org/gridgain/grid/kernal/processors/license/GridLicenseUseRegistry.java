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

package org.gridgain.grid.kernal.processors.license;

import org.gridgain.grid.product.*;
import org.gridgain.grid.util.*;
import org.jdk8.backport.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * Usage registry.
 */
public class GridLicenseUseRegistry {
    /** Usage map. */
    private static final ConcurrentMap<GridProductEdition, Collection<Class<?>>> useMap =
        new ConcurrentHashMap8<>();

    /**
     * Ensure singleton.
     */
    private GridLicenseUseRegistry() {
        // No-op.
    }

    /**
     * Callback for whenever component gets used.
     *
     * @param ed Edition.
     * @param cls Component.
     */
    public static void onUsage(GridProductEdition ed, Class<?> cls) {
        Collection<Class<?>> c = useMap.get(ed);

        if (c == null) {
            Collection<Class<?>> old = useMap.putIfAbsent(ed, c = new GridConcurrentHashSet<>());

            if (old != null)
                c = old;
        }

        c.add(cls);
    }

    /**
     * Gets used components for given edition.
     *
     * @param ed Edition.
     * @return Component.
     */
    public static Collection<Class<?>> usedClasses(GridProductEdition ed) {
        Collection<Class<?>> c = useMap.get(ed);

        return c == null ? Collections.<Class<?>>emptySet() : c;
    }

    /**
     * Checks if edition is used.
     *
     * @param ed Edition to check.
     * @return {@code True} if used.
     */
    public static boolean used(GridProductEdition ed) {
        return !usedClasses(ed).isEmpty();
    }

    /**
     * Clears usages.
     *
     * FOR TESTING PURPOSES ONLY!
     */
    public static void clear() {
        useMap.clear();
    }
}
