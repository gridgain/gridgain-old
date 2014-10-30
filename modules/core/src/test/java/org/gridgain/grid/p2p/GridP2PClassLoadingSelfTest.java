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

package org.gridgain.grid.p2p;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.product.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.lang.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.common.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import static org.gridgain.grid.product.GridProductVersion.*;

/**
 * P2P test.
 */
@GridCommonTest(group = "P2P")
public class GridP2PClassLoadingSelfTest extends GridCommonAbstractTest {
    /** */
    private final ClassLoader tstClsLdr;

    /** */
    public GridP2PClassLoadingSelfTest() {
        super(/*start grid*/false);

        tstClsLdr = new GridTestClassLoader(
            Collections.singletonMap("org/gridgain/grid/p2p/p2p.properties", "resource=loaded"),
            GridP2PTestTask.class.getName(),
            GridP2PTestJob.class.getName()
        );
    }

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings({"serial", "ConstantConditions"})
    public void testClassLoading() throws Exception {
        GridComputeTask<?, ?> task = (GridComputeTask<?, ?>)tstClsLdr.loadClass(GridP2PTestTask.class.getName()).newInstance();

        byte[] rawTask = GridTestIoUtils.serializeJdk(task);

        GridComputeTask<Object, Integer> res = GridTestIoUtils.deserializeJdk(rawTask, tstClsLdr);

        GridNode fakeNode = new TestGridNode();

        List<GridNode> nodes = Collections.singletonList(fakeNode);

        GridComputeJob p2pJob = res.map(nodes, 1).entrySet().iterator().next().getKey();

        assert p2pJob.getClass().getClassLoader() instanceof GridTestClassLoader : "Class loader = "
            + res.getClass().getClassLoader();
    }

    /** */
    private static class TestGridNode extends GridMetadataAwareAdapter implements GridNode {
        /** */
        private static AtomicInteger consistentIdCtr = new AtomicInteger();

        /** */
        private UUID nodeId = UUID.randomUUID();

        /** */
        private Object consistentId = consistentIdCtr.incrementAndGet();

        /** {@inheritDoc} */
        @Override public long order() {
            return -1;
        }

        /** {@inheritDoc} */
        @Override public GridProductVersion version() {
            return fromString("99.99.99");
        }

        /** {@inheritDoc} */
        @Override public UUID id() {
            return nodeId;
        }

        /** {@inheritDoc} */
        @Override public Object consistentId() {
            return consistentId;
        }

        /** {@inheritDoc} */
        @SuppressWarnings("unchecked")
        @Nullable @Override public <T> T attribute(String name) {
            return null;
        }

        /** {@inheritDoc} */
        @Nullable @Override public GridNodeMetrics metrics() {
            return null;
        }

        /** {@inheritDoc} */
        @Nullable @Override public Map<String, Object> attributes() {
            return null;
        }

        /** {@inheritDoc} */
        @Override public Collection<String> addresses() {
            return null;
        }

        /** {@inheritDoc} */
        @Override public boolean isLocal() {
            return false;
        }

        /** {@inheritDoc} */
        @Override public boolean isDaemon() {
            return false;
        }

        /** {@inheritDoc} */
        @Override public Collection<String> hostNames() {
            return null;
        }

        /** {@inheritDoc} */
        @Override public boolean equals(Object o) {
            return F.eqNodes(this, o);
        }

        /** {@inheritDoc} */
        @Override public int hashCode() {
            return id().hashCode();
        }
    }
}
