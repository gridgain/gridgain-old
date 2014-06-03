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

package org.gridgain.grid.spi.collision.jobstealing;

import org.gridgain.grid.*;
import org.gridgain.grid.spi.collision.*;
import org.gridgain.grid.spi.discovery.*;
import org.gridgain.grid.spi.failover.jobstealing.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.spi.*;

import java.io.*;
import java.util.*;

import static org.gridgain.grid.kernal.GridNodeAttributes.*;
import static org.gridgain.grid.spi.collision.jobstealing.GridJobStealingCollisionSpi.*;

/**
 * Job stealing attributes test.
 */
@GridSpiTest(spi = GridJobStealingCollisionSpi.class, group = "Collision SPI")
public class GridJobStealingCollisionSpiAttributesSelfTest extends GridSpiAbstractTest<GridJobStealingCollisionSpi> {
    /** */
    private static GridTestNode rmtNode;

    /** */
    public GridJobStealingCollisionSpiAttributesSelfTest() {
        super(true /*start spi*/);
    }

    /**
     * @return Wait jobs threshold.
     */
    @GridSpiTestConfig
    public int getWaitJobsThreshold() {
        return 0;
    }

    /**
     * @return Message expiration time.
     */
    @GridSpiTestConfig
    public long getMessageExpireTime() {
        return 1;
    }

    /**
     * @return Active jobs threshold.
     */
    @GridSpiTestConfig
    public int getActiveJobsThreshold() {
        return 1;
    }

    /** {@inheritDoc} */
    @Override protected GridSpiTestContext initSpiContext() throws Exception {
        GridSpiTestContext ctx = super.initSpiContext();

        GridTestNode locNode = new GridTestNode(UUID.randomUUID());

        addSpiDependency(locNode);

        ctx.setLocalNode(locNode);

        return ctx;
    }

    /**
     * Adds Failover SPI attribute.
     *
     * @param node Node to add attribute to.
     * @throws Exception If failed.
     */
    private void addSpiDependency(GridTestNode node) throws Exception {
        node.addAttribute(U.spiAttribute(getSpi(), ATTR_SPI_CLASS), GridJobStealingFailoverSpi.class.getName());
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        super.beforeTest();

        rmtNode = new GridTestNode(UUID.randomUUID());

        addSpiDependency(rmtNode);

        rmtNode.setAttribute(U.spiAttribute(getSpi(), WAIT_JOBS_THRESHOLD_NODE_ATTR), getWaitJobsThreshold());

        GridDiscoveryMetricsAdapter metrics = new GridDiscoveryMetricsAdapter();

        metrics.setCurrentWaitingJobs(2);

        rmtNode.setMetrics(metrics);

        getSpiContext().addNode(rmtNode);

        getSpi().setStealingEnabled(true);
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        super.afterTest();

        getSpiContext().failNode(rmtNode);
    }

    /**
     * @throws Exception If test failed.
     */
    public void testSameAttribute() throws Exception {
        List<GridCollisionJobContext> waitCtxs = Collections.emptyList();

        Collection<GridCollisionJobContext> activeCtxs = Collections.emptyList();

        GridTestNode rmtNode = (GridTestNode)getSpiContext().remoteNodes().iterator().next();

        rmtNode.setAttribute("useCollision", true);

        getSpiContext().triggerMessage(rmtNode, new GridJobStealingRequest(1));

        // Set up the same attribute and value as for remote node.
        getSpi().setStealingAttributes(F.asMap("useCollision", true));

        getSpi().onCollision(new GridCollisionTestContext(activeCtxs, waitCtxs));

        // Cleanup
        rmtNode.removeAttribute("useCollision");

        // Set up the same attribute and value as for remote node.
        getSpi().setStealingAttributes(Collections.<String, Serializable>emptyMap());

        // Make sure that no message was sent.
        Serializable msg = getSpiContext().removeSentMessage(rmtNode);

        // Message should be sent to remote node because it has the same
        // attributes.
        assert msg != null;
    }

    /**
     * @throws Exception If test failed.
     */
    public void testEmptyRemoteAttribute() throws Exception {
        List<GridCollisionJobContext> waitCtxs = Collections.emptyList();

        Collection<GridCollisionJobContext> activeCtxs = Collections.emptyList();

        GridNode rmtNode = F.first(getSpiContext().remoteNodes());

        getSpiContext().triggerMessage(rmtNode, new GridJobStealingRequest(1));

        // Set up the same attribute and value as for remote node.
        getSpi().setStealingAttributes(F.asMap("useCollision", true));

        getSpi().onCollision(new GridCollisionTestContext(activeCtxs, waitCtxs));

        // Set up the same attribute and value as for remote node.
        getSpi().setStealingAttributes(Collections.<String, Serializable>emptyMap());

        // Make sure that no message was sent.
        Serializable msg = getSpiContext().removeSentMessage(rmtNode);

        // Message should not be sent to remote node at it does not have attribute
        assert msg == null;
    }

    /**
     * @throws Exception If test failed.
     */
    public void testEmptyLocalAttribute() throws Exception {
        // Collision SPI does not allow to send more than 1 message in a
        // certain period of time (see getMessageExpireTime() method).
        // Thus we have to wait for the message to be expired.
        Thread.sleep(50);

        List<GridCollisionJobContext> waitCtxs = Collections.emptyList();

        Collection<GridCollisionJobContext> activeCtxs = Collections.emptyList();

        GridTestNode rmtNode = (GridTestNode)F.first(getSpiContext().remoteNodes());

        rmtNode.setAttribute("useCollision", true);

        getSpiContext().triggerMessage(rmtNode, new GridJobStealingRequest(1));

        getSpi().onCollision(new GridCollisionTestContext(activeCtxs, waitCtxs));

        // Cleanup.
        rmtNode.removeAttribute("useCollision");

        // Make sure that no message was sent.
        Serializable msg = getSpiContext().removeSentMessage(rmtNode);

        // Message should be sent to remote node because it has the same
        // attributes.
        assert msg != null;
    }

   /**
    * @throws Exception If test failed.
    */
    public void testDiffAttribute() throws Exception {
       List<GridCollisionJobContext> waitCtxs = Collections.emptyList();

       Collection<GridCollisionJobContext> activeCtxs = Collections.emptyList();

      GridTestNode rmtNode = (GridTestNode)F.first(getSpiContext().remoteNodes());

       rmtNode.setAttribute("useCollision1", true);

       getSpiContext().triggerMessage(rmtNode, new GridJobStealingRequest(1));

       // Set up the same attribute and value as for remote node.
       getSpi().setStealingAttributes(F.asMap("useCollision2", true));

       getSpi().onCollision(new GridCollisionTestContext(activeCtxs, waitCtxs));

       // Cleanup
       rmtNode.removeAttribute("useCollision1");

       // Set up the same attribute and value as for remote node.
       getSpi().setStealingAttributes(Collections.<String, Serializable>emptyMap());

       // Make sure that no message was sent.
        Serializable msg = getSpiContext().removeSentMessage(rmtNode);

       // Message should be sent to remote node because it has the same
       // attributes.
       assert msg == null;
    }

    /**
     * @throws Exception If test failed.
     */
    public void testBothEmptyAttribute() throws Exception {
        // Collision SPI does not allow to send more than 1 message in a
        // certain period of time (see getMessageExpireTime() method).
        // Thus we have to wait for the message to be expired.
        Thread.sleep(50);

        List<GridCollisionJobContext> waitCtxs = Collections.emptyList();

        Collection<GridCollisionJobContext> activeCtxs = Collections.emptyList();

        GridNode rmtNode = F.first(getSpiContext().remoteNodes());

        getSpiContext().triggerMessage(rmtNode, new GridJobStealingRequest(1));

        getSpi().onCollision(new GridCollisionTestContext(activeCtxs, waitCtxs));

        // Make sure that no message was sent.
        Serializable msg = getSpiContext().removeSentMessage(rmtNode);

        // Message should be sent to remote node because it has the same
        // attributes.
        assert msg != null;
    }

    /**
     * @throws Exception If test failed.
     */
    public void testIsStealingOff() throws Exception {
        // Collision SPI does not allow to send more than 1 message in a
        // certain period of time (see getMessageExpireTime() method).
        // Thus we have to wait for the message to be expired.
        Thread.sleep(50);

        List<GridCollisionJobContext> waitCtxs = Collections.emptyList();

        Collection<GridCollisionJobContext> activeCtxs = Collections.emptyList();

        GridNode rmtNode = F.first(getSpiContext().remoteNodes());

        getSpi().setStealingEnabled(false);

        getSpiContext().triggerMessage(rmtNode, new GridJobStealingRequest(1));

        getSpi().onCollision(new GridCollisionTestContext(activeCtxs, waitCtxs));

        // Make sure that no message was sent.
        Serializable msg = getSpiContext().removeSentMessage(rmtNode);

        // Message should not be sent to remote node because stealing is off
        assert msg == null;
    }

    /**
     * @throws Exception If test failed.
     */
    public void testIsStealingOn() throws Exception {
        // Collision SPI does not allow to send more than 1 message in a
        // certain period of time (see getMessageExpireTime() method).
        // Thus we have to wait for the message to be expired.
        Thread.sleep(50);

        List<GridCollisionJobContext> waitCtxs = Collections.emptyList();

        Collection<GridCollisionJobContext> activeCtxs = Collections.emptyList();

        GridNode rmtNode = F.first(getSpiContext().remoteNodes());

        getSpi().setStealingEnabled(true);

        getSpiContext().triggerMessage(rmtNode, new GridJobStealingRequest(1));

        getSpi().onCollision(new GridCollisionTestContext(activeCtxs, waitCtxs));

        // Make sure that no message was sent.
        Serializable msg = getSpiContext().removeSentMessage(rmtNode);

        // Message should not be sent to remote node because stealing is on
        assert msg != null;
    }
}
