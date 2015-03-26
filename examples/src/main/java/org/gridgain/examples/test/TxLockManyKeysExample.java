/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.examples.test;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.resources.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * Demonstrates event consume API that allows to register event listeners on remote nodes.
 * Note that grid events are disabled by default and must be specifically enabled,
 * just like in {@code examples/config/example-compute.xml} file.
 * <p>
 * Remote nodes should always be started with configuration: {@code 'ggstart.sh examples/config/example-compute.xml'}.
 * <p>
 * Alternatively you can run {@link org.gridgain.examples.ComputeNodeStartup} in another JVM which will start
 * GridGain node with {@code examples/config/example-compute.xml} configuration.
 */
public class TxLockManyKeysExample {
    /**
     * Executes example.
     *
     * @param args Command line arguments, none required.
     * @throws org.gridgain.grid.GridException If example execution failed.
     */
    public static void main(String[] args) throws Exception {
        try (Grid grid = GridGain.start("examples/config/example-cache.xml")) {
            System.out.println();
            System.out.println(">>> TxLock example started.");

            TxLockOneKeyExample.checkExplicitLock(grid, 4);
        }
    }
}
