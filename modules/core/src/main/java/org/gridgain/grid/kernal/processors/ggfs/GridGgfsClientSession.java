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

import org.jdk8.backport.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * GGFS client session. Effectively used to manage lifecycle of opened resources and close them on
 * connection close.
 */
public class GridGgfsClientSession {
    /** Session resources. */
    private ConcurrentMap<Long, Closeable> rsrcMap = new ConcurrentHashMap8<>();

    /**
     * Registers resource within this session.
     *
     * @param rsrcId Resource id.
     * @param rsrc Resource to register.
     */
    public boolean registerResource(long rsrcId, Closeable rsrc) {
        Object old = rsrcMap.putIfAbsent(rsrcId, rsrc);

        return old == null;
    }

    /**
     * Gets registered resource by ID.
     *
     * @param rsrcId Resource ID.
     * @return Resource or {@code null} if resource was not found.
     */
    @Nullable public <T> T resource(Long rsrcId) {
        return (T)rsrcMap.get(rsrcId);
    }

    /**
     * Unregister previously registered resource.
     *
     * @param rsrcId Resource ID.
     * @param rsrc Resource to unregister.
     * @return {@code True} if resource was unregistered, {@code false} if no resource
     *      is associated with this ID or other resource is associated with this ID.
     */
    public boolean unregisterResource(Long rsrcId, Closeable rsrc) {
        return rsrcMap.remove(rsrcId, rsrc);
    }

    /**
     * @return Registered resources iterator.
     */
    public Iterator<Closeable> registeredResources() {
        return rsrcMap.values().iterator();
    }
}
