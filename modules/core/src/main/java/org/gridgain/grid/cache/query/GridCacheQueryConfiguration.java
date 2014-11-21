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

package org.gridgain.grid.cache.query;

import java.io.*;
import java.util.*;

/**
 * Query configuration object.
 */
public class GridCacheQueryConfiguration implements Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Collection of query type metadata. */
    private Collection<GridCacheQueryTypeMetadata> typeMeta;

    /** Query type resolver. */
    private GridCacheQueryTypeResolver typeRslvr;

    /**
     * Default constructor.
     */
    public GridCacheQueryConfiguration() {
        // No-op.
    }

    /**
     * @param cfg Configuration to copy.
     */
    public GridCacheQueryConfiguration(GridCacheQueryConfiguration cfg) {
        typeMeta = cfg.getTypeMetadata();
        typeRslvr = cfg.getTypeResolver();
    }

    /**
     * Gets collection of query type metadata objects.
     *
     * @return Collection of query type metadata.
     */
    public Collection<GridCacheQueryTypeMetadata> getTypeMetadata() {
        return typeMeta;
    }

    /**
     * Sets collection of query type metadata objects.
     *
     * @param typeMeta Collection of query type metadata.
     */
    public void setTypeMetadata(Collection<GridCacheQueryTypeMetadata> typeMeta) {
        this.typeMeta = typeMeta;
    }

    /**
     * Gets query type resolver.
     *
     * @return Query type resolver.
     */
    public GridCacheQueryTypeResolver getTypeResolver() {
        return typeRslvr;
    }

    /**
     * Sets query type resolver.
     *
     * @param typeRslvr Query type resolver.
     */
    public void setTypeResolver(GridCacheQueryTypeResolver typeRslvr) {
        this.typeRslvr = typeRslvr;
    }
}
