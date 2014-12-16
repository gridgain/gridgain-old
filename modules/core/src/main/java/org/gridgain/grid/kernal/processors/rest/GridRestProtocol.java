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

package org.gridgain.grid.kernal.processors.rest;

import org.gridgain.grid.*;
import org.gridgain.grid.lang.*;

import java.util.*;

/**
 * REST protocol.
 */
public interface GridRestProtocol {
    /**
     * @return Protocol name.
     */
    public abstract String name();

    /**
     * Returns protocol properties for setting node attributes. Has meaningful result
     * only after protocol start.
     *
     * @return Protocol properties.
     */
    public abstract Collection<GridBiTuple<String, Object>> getProperties();

    /**
     * Starts protocol.
     *
     * @param hnd Command handler.
     * @throws GridException If failed.
     */
    public abstract void start(GridRestProtocolHandler hnd) throws GridException;

    /**
     * Grid start callback.
     */
    public abstract void onKernalStart();

    /**
     * Stops protocol.
     */
    public abstract void stop();
}
