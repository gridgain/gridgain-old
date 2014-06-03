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

package org.gridgain.grid.kernal.processors.cache.query.reducefields;

import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.query.*;
import org.gridgain.grid.lang.*;

import java.util.*;

import static org.gridgain.grid.cache.GridCacheMode.*;

/**
 * Reduce fields queries tests for partitioned cache.
 */
public class GridCacheReduceFieldsQueryPartitionedSelfTest extends GridCacheAbstractReduceFieldsQuerySelfTest {
    /** {@inheritDoc} */
    @Override protected GridCacheMode cacheMode() {
        return PARTITIONED;
    }

    /** {@inheritDoc} */
    @Override protected int gridCount() {
        return 3;
    }

    /**
     * @throws Exception If failed.
     */
    public void testIncludeBackups() throws Exception {
        GridCacheQuery<List<?>> qry = grid(0).cache(null).queries().createSqlFieldsQuery("select age from Person");

        qry.includeBackups(true);

        int sum = 0;

        for (GridBiTuple<Integer, Integer> tuple : qry.execute(new AverageRemoteReducer()).get())
            sum += tuple.get1();

        // One backup, so sum is two times greater
        assertEquals("Sum", 200, sum);
    }
}
