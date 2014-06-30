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

package org.gridgain.client;

import org.gridgain.grid.compute.*;
import org.gridgain.grid.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.springframework.beans.factory.*;
import org.springframework.context.support.*;

import java.net.*;
import java.util.*;

import static org.gridgain.grid.compute.GridComputeJobResultPolicy.*;

/**
 * Start node task, applicable arguments:
 * <ul>
 *     <li>tcp</li>
 *     <li>http</li>
 *     <li>tcp+ssl</li>
 *     <li>http+ssl</li>
 * </ul>
 */
public class GridClientStartNodeTask extends GridTaskSingleJobSplitAdapter<String, Integer> {
    /**
     * Available node's configurations.
     */
    private static final Map<String, String> NODE_CFG = new HashMap<String, String>() {{
        put("tcp", "modules/clients/src/test/resources/spring-server-node.xml");
        put("http", "modules/clients/src/test/resources/spring-server-node.xml");
        put("tcp+ssl", "modules/clients/src/test/resources/spring-server-ssl-node.xml");
        put("http+ssl", "modules/clients/src/test/resources/spring-server-ssl-node.xml");
    }};

    /** */
    @GridLoggerResource
    private transient GridLogger log;

    /** */
    @GridInstanceResource
    private transient Grid grid;

    /** {@inheritDoc} */
    @Override protected Object executeJob(int gridSize, String type) throws GridException {
        log.info(">>> Starting new grid node [currGridSize=" + gridSize + ", arg=" + type + "]");

        if (type == null)
            throw new IllegalArgumentException("Node type to start should be specified.");

        GridConfiguration cfg = getConfig(type);

        // Generate unique for this VM grid name.
        String gridName = cfg.getGridName() + " (" + UUID.randomUUID() + ")";

        // Update grid name (required to be unique).
        cfg.setGridName(gridName);

        // Start new node in current VM.
        Grid g =  G.start(cfg);

        log.info(">>> Grid started [nodeId=" + g.localNode().id() + ", name='" + g.name() + "']");

        return true;
    }

    /** {@inheritDoc} */
    @Override public GridComputeJobResultPolicy result(GridComputeJobResult res, List<GridComputeJobResult> rcvd)
        throws GridException {
        if (res.getException() != null)
            return FAILOVER;

        return WAIT;
    }

    /**
     * Load grid configuration for specified node type.
     *
     * @param type Node type to load configuration for.
     * @return Grid configuration for specified node type.
     */
    static GridConfiguration getConfig(String type) {
        String path = NODE_CFG.get(type);

        if (path == null)
            throw new IllegalArgumentException("Unsupported node type: " + type);

        URL url = U.resolveGridGainUrl(path);

        BeanFactory ctx = new FileSystemXmlApplicationContext(url.toString());

        return (GridConfiguration)ctx.getBean("grid.cfg");
    }

    /**
     * Example for start/stop node tasks.
     *
     * @param args Not used.
     */
    public static void main(String[] args) {
        String nodeType = "tcp+ssl";

        // Start initial node = 1
        try (Grid g = G.start(NODE_CFG.get(nodeType))) {
            // Change topology.
            changeTopology(g, 4, 1, nodeType);
            changeTopology(g, 1, 4, nodeType);

            // Stop node by id = 0
            g.compute().execute(GridClientStopNodeTask.class, g.localNode().id().toString()).get();

            // Wait for node stops.
            //U.sleep(1000);

            assert G.allGrids().isEmpty();
        }
        catch (GridException e) {
            System.err.println("Uncaught exception: " + e.getMessage());

            e.printStackTrace(System.err);
        }
    }

    /**
     * Change topology.
     *
     * @param parent Grid to execute tasks on.
     * @param add New nodes count.
     * @param rmv Remove nodes count.
     * @param type Type of nodes to manipulate.
     * @throws GridException On any exception.
     */
    private static void changeTopology(Grid parent, int add, int rmv, String type) throws GridException {
        Collection<GridComputeTaskFuture<?>> tasks = new ArrayList<>();

        // Start nodes in parallel.
        while (add-- > 0)
            tasks.add(parent.compute().execute(GridClientStartNodeTask.class, type));

        for (GridComputeTaskFuture<?> task : tasks)
            task.get();

        // Stop nodes in sequence.
        while (rmv-- > 0)
            parent.compute().execute(GridClientStopNodeTask.class, type).get();

        // Wait for node stops.
        //U.sleep(1000);

        Collection<String> gridNames = new ArrayList<>();

        for (Grid g : G.allGrids())
            gridNames.add(g.name());

        parent.log().info(">>> Available grids: " + gridNames);
    }
}
