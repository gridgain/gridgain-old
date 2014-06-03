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

package org.gridgain.examples;

import org.gridgain.grid.*;
import org.gridgain.grid.streamer.*;

import java.net.*;

/**
 *
 */
public class ExamplesUtils {
    /** */
    private static final ClassLoader CLS_LDR = ExamplesUtils.class.getClassLoader();

    /**
     * Exits with code {@code -1} if maximum memory is below 90% of minimally allowed threshold.
     *
     * @param min Minimum memory threshold.
     */
    public static void checkMinMemory(long min) {
        long maxMem = Runtime.getRuntime().maxMemory();

        if (maxMem < .85 * min) {
            System.err.println("Heap limit is too low (" + (maxMem / (1024 * 1024)) +
                "MB), please increase heap size at least up to " + (min / (1024 * 1024)) + "MB.");

            System.exit(-1);
        }
    }

    /**
     * Returns URL resolved by class loader for classes in examples project.
     *
     * @return Resolved URL.
     */
    public static URL url(String path) {
        URL url = CLS_LDR.getResource(path);

        if (url == null)
            throw new RuntimeException("Failed to resolve resource URL by path: " + path);

        return url;
    }

    /**
     * Checks minimum topology size for running a certain example.
     *
     * @param prj Projection to check size for.
     * @param size Minimum number of nodes required to run a certain example.
     * @return {@code True} if check passed, {@code false} otherwise.
     */
    public static boolean checkMinTopologySize(GridProjection prj, int size) {
        int prjSize = prj.nodes().size();

        if (prjSize < size) {
            System.out.println();
            System.out.println(">>> Please start at least " + size + " grid nodes to run example.");
            System.out.println();

            return false;
        }

        return true;
    }

    /**
     * @param grid Grid.
     * @param name Streamer name.
     * @return {@code True} if grid has streamer with given name.
     */
    public static boolean hasStreamer(Grid grid, String name) {
        if (grid.configuration().getStreamerConfiguration() != null) {
            for (GridStreamerConfiguration cfg : grid.configuration().getStreamerConfiguration()) {
                if (name.equals(cfg.getName()))
                    return true;
            }
        }

        return false;
    }
}
