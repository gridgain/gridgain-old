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

import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.junits.common.*;

/**
 * String builder factory test.
 */
@GridCommonTest(group = "Utils")
public class GridStringBuilderFactorySelfTest extends GridCommonAbstractTest {
    /** */
    public GridStringBuilderFactorySelfTest() {
        super(/*start grid*/false);
    }

    /**
     * Tests string builder factory.
     */
    public void testStringBuilderFactory() {
        SB b1 = GridStringBuilderFactory.acquire();

        assert b1.length() == 0;

        b1.a("B1 Test String");

        SB b2 = GridStringBuilderFactory.acquire();

        assert b2.length() == 0;

        assert b1 != b2;

        b1.a("B2 Test String");

        assert !b1.toString().equals(b2.toString());

        GridStringBuilderFactory.release(b2);
        GridStringBuilderFactory.release(b1);

        assert b1.length() == 0;
        assert b2.length() == 0;

        SB b3 = GridStringBuilderFactory.acquire();

        assert b1 == b3;

        assert b3.length() == 0;

        b3.a("B3 Test String");

        GridStringBuilderFactory.release(b3);

        assert b3.length() == 0;
    }
}
