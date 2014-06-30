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

package org.gridgain.grid.kernal.processors.streamer;

import org.gridgain.grid.*;
import org.gridgain.grid.streamer.*;

import java.util.*;

/**
 * Test stage.
 */
class GridTestStage implements GridStreamerStage<Object> {
    /** Stage name. */
    private String name;

    /** Stage closure. */
    private SC stageClos;

    /**
     * @param name Stage name.
     * @param stageClos Stage closure to execute.
     */
    GridTestStage(String name, SC stageClos) {
        this.name = name;
        this.stageClos = stageClos;
    }

    /** {@inheritDoc} */
    @Override public String name() {
        return name;
    }

    /** {@inheritDoc} */
    @Override public Map<String, Collection<?>> run(GridStreamerContext ctx, Collection<Object> evts)
        throws GridException {
        return stageClos.apply(name(), ctx, evts);
    }
}
