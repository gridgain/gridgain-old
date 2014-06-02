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

package org.gridgain.loadtests.streamer;

import org.gridgain.grid.lang.*;
import org.gridgain.grid.streamer.*;

import java.util.*;

/**
 * Configurable streamer load.
 */
public class GridStreamerLoad {
    /** Steamer name. */
    private String name;

    /** Load closures. */
    private List<GridInClosure<GridStreamer>> clos;

    /**
     * @return Steamer name.
     */
    public String getName() {
        return name;
    }

    /**
     * @param name Steamer name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return Query closure.
     */
    public List<GridInClosure<GridStreamer>> getClosures() {
        return clos;
    }

    /**
     * @param clos Query closure.
     */
    public void setClosures(List<GridInClosure<GridStreamer>> clos) {
        this.clos = clos;
    }
}
