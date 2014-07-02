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
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.spi.indexing.*;
import org.gridgain.grid.util.future.*;

import java.util.*;

/**
* Error future for fields query.
*/
public class GridCacheFieldsQueryErrorFuture extends GridCacheQueryErrorFuture<List<?>> {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private boolean incMeta;

    /**
     * @param ctx Context.
     * @param th Error.
     * @param incMeta Include metadata flag.
     */
    public GridCacheFieldsQueryErrorFuture(GridKernalContext ctx, Throwable th, boolean incMeta) {
        super(ctx, th);

        this.incMeta = incMeta;
    }

    /**
     * @return Metadata.
     */
    public GridFuture<List<GridIndexingFieldMetadata>> metadata() {
        return new GridFinishedFuture<>(ctx, incMeta ? Collections.<GridIndexingFieldMetadata>emptyList() : null);
    }
}
