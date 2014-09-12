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

package org.gridgain.grid.tests.p2p;

import org.gridgain.grid.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;

/**
 * User resource, that increases node-local counters
 * on deploy and undeploy.
 */
public class GridP2PAwareTestUserResource {
    /** Deploy counter key. */
    private static final String DEPLOY_CNT_KEY = "deployCnt";

    /** Undeploy counter key. */
    private static final String UNDEPLOY_CNT_KEY = "undeployCnt";

    /** Grid instance. */
    @GridInstanceResource
    private Grid grid;

    /** Grid logger. */
    @GridLoggerResource
    private GridLogger log;

    /**
     * Invoked on resource deploy. Increments deploy counter
     * in node-local store.
     */
    @SuppressWarnings("ConstantConditions")
    @GridUserResourceOnDeployed
    public void onDeployed() {
        concurrentIncrement(DEPLOY_CNT_KEY);
    }

    /**
     * Invoked on resource undeploy. Increments undeploy counter
     * in node-local store.
     */
    @SuppressWarnings("ConstantConditions")
    @GridUserResourceOnUndeployed
    public void onUndeployed() {
        concurrentIncrement(UNDEPLOY_CNT_KEY);
    }

    /**
     * Concurrently increments numeric cache value.
     *
     * @param key Key for the value to be incremented.
     */
    private <T> void concurrentIncrement(T key) {
        GridNodeLocalMap<T, Integer> nodeLoc = grid.nodeLocalMap();

        Integer cntr = nodeLoc.get(key);

        if (cntr == null)
            cntr = nodeLoc.putIfAbsent(key, 1);

        if (cntr != null) {
            while (!nodeLoc.replace(key, cntr, cntr + 1)) {
                cntr = nodeLoc.get(key);

                assert cntr != null;
            }
        }
    }
}
