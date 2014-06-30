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

import org.gridgain.grid.lang.*;
import org.jetbrains.annotations.*;

/**
 * Query execution bean.
 */
public class GridCacheQueryBean {
    /** */
    private final GridCacheQueryAdapter<?> qry;

    /** */
    private final GridReducer<Object, Object> rdc;

    /** */
    private final GridClosure<Object, Object> trans;

    /** */
    private final Object[] args;

    /**
     * @param qry Query.
     * @param rdc Optional reducer.
     * @param trans Optional transformer.
     * @param args Optional arguments.
     */
    public GridCacheQueryBean(GridCacheQueryAdapter<?> qry, @Nullable GridReducer<Object, Object> rdc,
        @Nullable GridClosure<Object, Object> trans, @Nullable Object[] args) {
        assert qry != null;

        this.qry = qry;
        this.rdc = rdc;
        this.trans = trans;
        this.args = args;
    }

    /**
     * @return Query.
     */
    public GridCacheQueryAdapter<?> query() {
        return qry;
    }

    /**
     * @return Reducer.
     */
    @Nullable public GridReducer<Object, Object> reducer() {
        return rdc;
    }

    /**
     * @return Transformer.
     */
    @Nullable public GridClosure<Object, Object> transform() {


        return trans;
    }

    /**
     * @return Arguments.
     */
    @Nullable public Object[] arguments() {
        return args;
    }
}
