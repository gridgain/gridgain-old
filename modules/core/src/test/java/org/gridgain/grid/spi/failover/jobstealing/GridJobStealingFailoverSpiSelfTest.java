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

package org.gridgain.grid.spi.failover.jobstealing;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.spi.collision.jobstealing.*;
import org.gridgain.grid.spi.failover.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.spi.*;

import java.util.*;

import static org.gridgain.grid.kernal.GridNodeAttributes.*;
import static org.gridgain.grid.spi.collision.jobstealing.GridJobStealingCollisionSpi.*;
import static org.gridgain.grid.spi.failover.jobstealing.GridJobStealingFailoverSpi.*;

/**
 * Self test for {@link GridJobStealingFailoverSpi} SPI.
 */
@GridSpiTest(spi = GridJobStealingFailoverSpi.class, group = "Failover SPI")
public class GridJobStealingFailoverSpiSelfTest extends GridSpiAbstractTest<GridJobStealingFailoverSpi> {
    /** {@inheritDoc} */
    @Override protected GridSpiTestContext initSpiContext() throws Exception {
        GridSpiTestContext ctx = super.initSpiContext();

        GridTestNode loc = new GridTestNode(UUID.randomUUID());

        addSpiDependency(loc);

        ctx.setLocalNode(loc);

        GridTestNode rmt = new GridTestNode(UUID.randomUUID());

        ctx.addNode(rmt);

        addSpiDependency(rmt);

        return ctx;
    }

    /**
     * Adds Collision SPI attribute.
     *
     * @param node Node to add attribute to.
     * @throws Exception If failed.
     */
    private void addSpiDependency(GridTestNode node) throws Exception {
        node.addAttribute(ATTR_SPI_CLASS, GridJobStealingCollisionSpi.class.getName());

        node.setAttribute(U.spiAttribute(getSpi(), ATTR_SPI_CLASS), getSpi().getClass().getName());
    }

    /**
     * @throws Exception If test failed.
     */
    public void testFailover() throws Exception {
        GridNode rmt = getSpiContext().remoteNodes().iterator().next();

        GridTestJobResult failed = new GridTestJobResult(rmt);

        failed.getJobContext().setAttribute(THIEF_NODE_ATTR,
            getSpiContext().localNode().id());

        GridNode other = getSpi().failover(new GridFailoverTestContext(new GridTestTaskSession(), failed),
            new ArrayList<>(getSpiContext().nodes()));

        assert other == getSpiContext().localNode();

        // This is not a failover but stealing.
        checkAttributes(failed.getJobContext(), null, 0);
    }

    /**
     * @throws Exception If test failed.
     */
    public void testMaxHopsExceeded() throws Exception {
        GridNode rmt = getSpiContext().remoteNodes().iterator().next();

        GridTestJobResult failed = new GridTestJobResult(rmt);

        failed.getJobContext().setAttribute(THIEF_NODE_ATTR,
            getSpiContext().localNode().id());
        failed.getJobContext().setAttribute(FAILOVER_ATTEMPT_COUNT_ATTR,
            getSpi().getMaximumFailoverAttempts());

        GridNode other = getSpi().failover(new GridFailoverTestContext(new GridTestTaskSession(), failed),
            new ArrayList<>(getSpiContext().nodes()));

        assert other == null;
    }

    /**
     * @throws Exception If test failed.
     */
    public void testMaxHopsExceededThiefNotSet() throws Exception {
        GridNode rmt = getSpiContext().remoteNodes().iterator().next();

        GridTestJobResult failed = new GridTestJobResult(rmt);

        failed.getJobContext().setAttribute(FAILOVER_ATTEMPT_COUNT_ATTR,
            getSpi().getMaximumFailoverAttempts());

        GridNode other = getSpi().failover(new GridFailoverTestContext(new GridTestTaskSession(), failed),
            new ArrayList<>(getSpiContext().nodes()));

        assert other == null;
    }

    /**
     * @throws Exception If test failed.
     */
    public void testNonZeroFailoverCount() throws Exception {
        GridNode rmt = getSpiContext().remoteNodes().iterator().next();

        GridTestJobResult failed = new GridTestJobResult(rmt);

        failed.getJobContext().setAttribute(FAILOVER_ATTEMPT_COUNT_ATTR,
            getSpi().getMaximumFailoverAttempts() - 1);

        GridNode other = getSpi().failover(new GridFailoverTestContext(new GridTestTaskSession(), failed),
            new ArrayList<>(getSpiContext().nodes()));

        assert other != null;
        assert other != rmt;

        assert other == getSpiContext().localNode();

        checkAttributes(failed.getJobContext(), rmt, getSpi().getMaximumFailoverAttempts());
    }

    /**
     * @throws Exception If test failed.
     */
    public void testThiefNotInTopology() throws Exception {
        GridNode rmt = new GridTestNode(UUID.randomUUID());

        GridTestJobResult failed = new GridTestJobResult(rmt);

        failed.getJobContext().setAttribute(THIEF_NODE_ATTR, rmt.id());

        GridNode other = getSpi().failover(new GridFailoverTestContext(new GridTestTaskSession(), failed),
            new ArrayList<>(getSpiContext().nodes()));

        assert other != null;
        assert other != rmt;

        assert getSpiContext().nodes().contains(other);

        checkAttributes(failed.getJobContext(), rmt, 1);
    }

    /**
     * @throws Exception If test failed.
     */
    public void testThiefEqualsVictim() throws Exception {
        GridNode rmt = getSpiContext().remoteNodes().iterator().next();

        GridTestJobResult failed = new GridTestJobResult(rmt);

        failed.getJobContext().setAttribute(THIEF_NODE_ATTR, rmt.id());

        GridNode other = getSpi().failover(new GridFailoverTestContext(new GridTestTaskSession(), failed),
            new ArrayList<>(getSpiContext().nodes()));

        assert other != null;
        assert other != rmt;

        assert other.equals(getSpiContext().localNode());

        checkAttributes(failed.getJobContext(), rmt, 1);
    }

    /**
     * @throws Exception If test failed.
     */
    public void testThiefIdNotSet() throws Exception {
        GridNode rmt = getSpiContext().remoteNodes().iterator().next();

        GridTestJobResult failed = new GridTestJobResult(rmt);

        GridNode other = getSpi().failover(new GridFailoverTestContext(new GridTestTaskSession(), failed),
            new ArrayList<>(getSpiContext().nodes()));

        assert other != null;
        assert other != rmt;

        assert other.equals(getSpiContext().localNode());

        checkAttributes(failed.getJobContext(), rmt, 1);
    }

    /**
     * @param ctx Failed job context.
     * @param failed Failed node.
     * @param failCnt Failover count.
     */
    @SuppressWarnings("unchecked")
    private void checkAttributes(GridComputeJobContext ctx, GridNode failed, int failCnt) {
        assert (Integer)ctx.getAttribute(FAILOVER_ATTEMPT_COUNT_ATTR) == failCnt;

        if (failed != null) {
            Collection<UUID> failedSet = (Collection<UUID>)ctx.getAttribute(FAILED_NODE_LIST_ATTR);

            assert failedSet.contains(failed.id());
        }
    }
}
