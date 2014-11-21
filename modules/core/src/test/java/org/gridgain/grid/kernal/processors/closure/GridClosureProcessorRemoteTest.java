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

package org.gridgain.grid.kernal.processors.closure;

import org.gridgain.grid.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.junits.common.*;
import java.util.*;

/**
 * Tests execution of anonymous closures on remote nodes.
 */
@GridCommonTest(group = "Closure Processor")
public class GridClosureProcessorRemoteTest extends GridCommonAbstractTest {
    /**
     *
     */
    public GridClosureProcessorRemoteTest() {
        super(true); // Start grid.
    }

    /** {@inheritDoc} */
    @Override public String getTestGridName() {
        return null;
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration() throws Exception {
        GridConfiguration cfg = new GridConfiguration();

        cfg.setDiscoverySpi(new GridTcpDiscoverySpi());

        return cfg;
    }

    /**
     * @throws Exception Thrown in case of failure.
     */
    public void testAnonymousBroadcast() throws Exception {
        Grid g = grid();

        assert g.nodes().size() >= 2;

        g.compute().run(new CA() {
            @Override public void apply() {
                System.out.println("BROADCASTING....");
            }
        });

        Thread.sleep(2000);
    }

    /**
     * @throws Exception Thrown in case of failure.
     */
    public void testAnonymousUnicast() throws Exception {
        Grid g = grid();

        assert g.nodes().size() >= 2;

        GridNode rmt = F.first(g.forRemotes().nodes());

        g.forNode(rmt).compute().run(new CA() {
            @Override public void apply() {
                System.out.println("UNICASTING....");
            }
        }).get();

        Thread.sleep(2000);
    }

    /**
     *
     * @throws Exception Thrown in case of failure.
     */
    public void testAnonymousUnicastRequest() throws Exception {
        Grid g = grid();

        assert g.nodes().size() >= 2;

        GridNode rmt = F.first(g.forRemotes().nodes());
        final GridNode loc = g.localNode();

        g.forNode(rmt).compute().run(new CA() {
            @Override public void apply() {
                grid().forNode(loc).message().localListen(new GridBiPredicate<UUID, String>() {
                    @Override public boolean apply(UUID uuid, String s) {
                        System.out.println("Received test message [nodeId: " + uuid + ", s=" + s + ']');

                        return false;
                    }
                }, null);
            }
        }).get();

        g.forNode(rmt).message().send(null, "TESTING...");

        Thread.sleep(2000);
    }
}
