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

package org.gridgain.grid.lang.utils;

import org.gridgain.grid.util.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;

/**
 * Test for {@link GridBoundedConcurrentLinkedHashMap}.
 */
public class GridBoundedConcurrentLinkedHashMapSelfTest extends GridCommonAbstractTest {
    /** Bound. */
    private static final int MAX = 3;

    /**
     * @throws Exception If failed.
     */
    public void testBound() throws Exception {
        Map<Integer, Integer> map = new GridBoundedConcurrentLinkedHashMap<>(MAX);

        for  (int i = 1; i <= 10; i++) {
            map.put(i, i);

            if (i <= MAX)
                assert map.size() == i;
            else
                assert map.size() == MAX;
        }

        assert map.size() == MAX;

        Iterator<Integer> it = map.values().iterator();

        assert it.next() == 8;
        assert it.next() == 9;
        assert it.next() == 10;
    }
}
