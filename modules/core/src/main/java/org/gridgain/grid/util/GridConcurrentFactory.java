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

package org.gridgain.grid.util;

import org.gridgain.grid.util.typedef.*;
import org.jdk8.backport.*;

import java.util.concurrent.*;

import static org.gridgain.grid.GridSystemProperties.*;

/**
 * Concurrent map factory.
 */
public class GridConcurrentFactory {
    /** Default concurrency level. */
    private static final int CONCURRENCY_LEVEL;

    /**
     * Initializes concurrency level.
     */
    static {
        int dfltLevel = 256;

        String s = X.getSystemOrEnv(GG_MAP_CONCURRENCY_LEVEL, Integer.toString(dfltLevel));

        int level;

        try {
            level = Integer.parseInt(s);
        }
        catch (NumberFormatException ignore) {
            level = dfltLevel;
        }

        CONCURRENCY_LEVEL = level;
    }

    /**
     * Ensure singleton.
     */
    private GridConcurrentFactory() {
        // No-op.
    }

    /**
     * Creates concurrent map with default concurrency level.
     *
     * @return New concurrent map.
     */
    public static <K, V> ConcurrentMap<K, V> newMap() {
        return new ConcurrentHashMap8<>(16 * CONCURRENCY_LEVEL, 0.75f, CONCURRENCY_LEVEL);
    }

    /**
     * Creates concurrent map with default concurrency level and given {@code initCap}.
     *
     * @param initCap Initial capacity.
     * @return New concurrent map.
     */
    public static <K, V> ConcurrentMap<K, V> newMap(int initCap) {
        return new ConcurrentHashMap8<>(initialSize(initCap, CONCURRENCY_LEVEL), 0.75f, CONCURRENCY_LEVEL);
    }

    /**
     * Creates concurrent map with given concurrency level and initCap.
     *
     * @param initCap Initial capacity.
     * @param concurrencyLevel Concurrency level.
     * @return New concurrent map.
     */
    public static <K, V> ConcurrentMap<K, V> newMap(int initCap, int concurrencyLevel) {
        return new ConcurrentHashMap8<>(initialSize(initCap, concurrencyLevel), 0.75f, concurrencyLevel);
    }

    /**
     * Creates concurrent set with default concurrency level.
     *
     * @return New concurrent map.
     */
    public static <V> GridConcurrentHashSet<V> newSet() {
        return new GridConcurrentHashSet<>(16 * CONCURRENCY_LEVEL, 0.75f, CONCURRENCY_LEVEL);
    }

    /**
     * Creates concurrent set with default concurrency level and given {@code initCap}.
     *
     * @param initCap Initial capacity.
     * @return New concurrent map.
     */
    public static <V> GridConcurrentHashSet<V> newSet(int initCap) {
        return new GridConcurrentHashSet<>(initialSize(initCap, CONCURRENCY_LEVEL), 0.75f, CONCURRENCY_LEVEL);
    }

    /**
     * Creates concurrent set with given concurrency level and initCap.
     *
     * @param initCap Initial capacity.
     * @param concurrencyLevel Concurrency level.
     * @return New concurrent map.
     */
    public static <V> GridConcurrentHashSet<V> newSet(int initCap, int concurrencyLevel) {
        return new GridConcurrentHashSet<>(initialSize(initCap, concurrencyLevel), 0.75f, concurrencyLevel);
    }

    /**
     * @param cap Capacity.
     * @param concurrencyLevel Concurrency level.
     * @return Calculated size.
     */
    private static int initialSize(int cap, int concurrencyLevel) {
        return cap / concurrencyLevel < 16 ? 16 * concurrencyLevel : cap;
    }
}
