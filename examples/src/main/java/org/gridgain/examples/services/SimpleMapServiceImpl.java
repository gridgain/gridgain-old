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

package org.gridgain.examples.services;

import org.gridgain.grid.service.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * Simple service which loops infinitely and prints out a counter.
 */
public class SimpleMapServiceImpl<K, V> implements GridService, SimpleMapService<K, V> {
    /** Serial version UID. */
    private static final long serialVersionUID = 0L;

    /** Underlying cache map. */
    private Map<K, V> map;

    /** {@inheritDoc} */
    @Override public void put(K key, V val) {
        map.put(key, val);
    }

    /** {@inheritDoc} */
    @Override public V get(K key) {
        return map.get(key);
    }

    /** {@inheritDoc} */
    @Override public void clear() {
        map.clear();
    }

    @Override public int size() {
        return map.size();
    }

    /** {@inheritDoc} */
    @Override public void cancel(GridServiceContext ctx) {
        System.out.println("Service was cancelled: " + ctx.name());
    }

    /** {@inheritDoc} */
    @Override public void init(GridServiceContext ctx) throws Exception {
        System.out.println("Service was initialized: " + ctx.name());

        map = new ConcurrentHashMap<>();
    }

    /** {@inheritDoc} */
    @Override public void execute(GridServiceContext ctx) throws Exception {
        System.out.println("Executing distributed service: " + ctx.name());
    }
}
