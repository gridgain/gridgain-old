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

package org.gridgain.loadtests.discovery;

import org.gridgain.grid.*;
import org.gridgain.grid.dataload.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.util.*;

/**
 *
 */
public class GridGcTimeoutTest {
    /** */
    public static final String CFG_PATH = "modules/core/src/test/config/discovery-stress.xml";

    /** */
    public static final int VALUE_SIZE = 1024;

    /**
     * @param args Args.
     * @throws GridException If failed.
     */
    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) throws GridException {
        Grid g = G.start(U.resolveGridGainUrl(CFG_PATH));

        GridDataLoader<Long, String> ldr = g.dataLoader(null);

        ldr.perNodeBufferSize(16 * 1024);

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < VALUE_SIZE - 42; i++)
            sb.append('a');

        String str = sb.toString();
        long cntr = 0;

        while (true) {
            ldr.addData(cntr++, UUID.randomUUID() + str);

            if (cntr % 1000000 == 0)
                X.println("!!! Entries added: " + cntr);
        }
    }
}
