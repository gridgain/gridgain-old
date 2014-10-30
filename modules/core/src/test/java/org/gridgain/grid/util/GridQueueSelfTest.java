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

import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.junits.common.*;

/**
 * Grid utils tests.
 */
@GridCommonTest(group = "Utils")
public class GridQueueSelfTest extends GridCommonAbstractTest {
    /**
     *
     */
    public void testQueue() {
        GridQueue<String> q = new GridQueue<>();
        for (char c = 'a'; c <= 'z'; c++)
            q.offer(Character.toString(c));

        assertEquals('z' - 'a' + 1, q.size());

        char ch = 'a';

        for (String c = q.poll(); c != null; c = q.poll()) {
            X.println(c);

            assertEquals(Character.toString(ch++), c);
        }

        assert q.isEmpty();

        for (char c = 'A'; c <= 'Z'; c++)
            q.offer(Character.toString(c));

        assertEquals('Z' - 'A' + 1, q.size());

        ch = 'A';

        for (String s : q) {
            X.println(s);

            assertEquals(Character.toString(ch++), s);
        }

        q.remove("O");

        assertEquals('Z' - 'A', q.size());

        for (String c = q.poll(); c != null; c = q.poll())
            assert !"O".equals(c);

        assert q.isEmpty();
    }
}
