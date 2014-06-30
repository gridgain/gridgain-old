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

package org.gridgain.grid.kernal.processors.cache.query;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.spi.indexing.*;
import org.gridgain.grid.util.future.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;

/**
* Local fields query future.
*/
public class GridCacheLocalFieldsQueryFuture
    extends GridCacheLocalQueryFuture<Object, Object, List<Object>>
    implements GridCacheQueryMetadataAware {
    /** */
    private static final long serialVersionUID = 0L;

    /** Meta data future. */
    private final GridFutureAdapter<List<GridIndexingFieldMetadata>> metaFut;

    /**
     * Required by {@link Externalizable}.
     */
    public GridCacheLocalFieldsQueryFuture() {
        metaFut = null;
    }

    /**
     * @param ctx Cache context.
     * @param qry Query.
     */
    public GridCacheLocalFieldsQueryFuture(GridCacheContext<?, ?> ctx, GridCacheQueryBean qry) {
        super((GridCacheContext<Object, Object>)ctx, qry);

        metaFut = new GridFutureAdapter<>(ctx.kernalContext());

        if (!qry.query().includeMetadata())
            metaFut.onDone();
    }

    /**
     * @param nodeId Sender node ID.
     * @param metaData Meta data.
     * @param data Page data.
     * @param err Error.
     * @param finished Finished or not.
     */
    public void onPage(@Nullable UUID nodeId, @Nullable List<GridIndexingFieldMetadata> metaData,
        @Nullable Collection<?> data, @Nullable Throwable err, boolean finished) {
        onPage(nodeId, data, err, finished);

        if (!metaFut.isDone())
            metaFut.onDone(metaData);
    }

    /** {@inheritDoc} */
    @Override public GridFuture<List<GridIndexingFieldMetadata>> metadata() {
        return metaFut;
    }

    /** {@inheritDoc} */
    @Override boolean fields() {
        return true;
    }
}
