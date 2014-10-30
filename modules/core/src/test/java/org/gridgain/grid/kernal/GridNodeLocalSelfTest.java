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

package org.gridgain.grid.kernal;

import org.gridgain.grid.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.lang.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;

/**
 * This test will test node local storage.
 */
@GridCommonTest(group = "Kernal Self")
public class GridNodeLocalSelfTest extends GridCommonAbstractTest {
    /** Create test. */
    public GridNodeLocalSelfTest() {
        super(/* Start grid. */true);
    }

    /**
     * Test node-local values operations.
     *
     * @throws Exception If test failed.
     */
    public void testNodeLocal() throws Exception {
        Grid g = G.grid(getTestGridName());

        String keyStr = "key";
        int keyNum = 1;
        Date keyDate = new Date();

        GridTuple3 key = F.t(keyNum, keyStr, keyDate);

        GridNodeLocalMap<Object, Object> nl = g.nodeLocalMap();

        nl.put(keyStr, "Hello world!");
        nl.put(key, 12);

        assert nl.containsKey(keyStr);
        assert nl.containsKey(key);
        assert !nl.containsKey(keyNum);
        assert !nl.containsKey(F.t(keyNum, keyStr));

        assert "Hello world!".equals(nl.get(keyStr));
        assert (Integer)nl.get(key) == 12;
    }
}
