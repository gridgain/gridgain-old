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

package org.gridgain.loadtests.dsi;

import org.gridgain.grid.Grid;
import org.gridgain.grid.GridException;
import org.gridgain.grid.GridLifecycleBean;
import org.gridgain.grid.GridLifecycleEventType;
import org.gridgain.grid.resources.GridInstanceResource;
import org.gridgain.grid.resources.GridSpringApplicationContextResource;
import org.springframework.context.ApplicationContext;

/**
 *
 */
public class GridDsiLifecycleBean implements GridLifecycleBean {
    /**
     * Grid instance will be automatically injected. For additional resources
     * that can be injected into lifecycle beans see
     * {@link GridLifecycleBean} documentation.
     */
    @GridInstanceResource
    private Grid grid;

    /** */
    @SuppressWarnings("UnusedDeclaration")
    @GridSpringApplicationContextResource
    private ApplicationContext springCtx;

    /** {@inheritDoc} */
    @Override public void onLifecycleEvent(GridLifecycleEventType evt) throws GridException {
        switch (evt) {
            case BEFORE_GRID_START:
                break;

            case AFTER_GRID_START:
                grid.cache("PARTITIONED_CACHE").dataStructures().atomicSequence("ID", 0, true);
                break;

            case BEFORE_GRID_STOP:
                break;

            case AFTER_GRID_STOP:
                break;
        }
    }
}
