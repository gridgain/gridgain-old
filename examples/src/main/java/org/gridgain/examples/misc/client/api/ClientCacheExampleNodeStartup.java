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

package org.gridgain.examples.misc.client.api;

import org.gridgain.grid.*;
import org.gridgain.grid.product.*;

import static org.gridgain.grid.product.GridProductEdition.*;

/**
 * Starts up grid node (server) for use with {@link ClientCacheExample}.
 * <p>
 * Note that different nodes cannot share the same port for rest services. If you want
 * to start more than one node on the same physical machine you must provide different
 * configurations for each node. Otherwise, this example would not work.
 */
@GridOnlyAvailableIn(DATA_GRID)
public class ClientCacheExampleNodeStartup {
    /**
     * Starts up a node with cache configuration.
     *
     * @param args Command line arguments, none required.
     * @throws GridException In case of any exception.
     */
    public static void main(String[] args) throws GridException {
        String springCfgPath = "examples/config/example-cache.xml";

        GridGain.start(springCfgPath);
    }
}
