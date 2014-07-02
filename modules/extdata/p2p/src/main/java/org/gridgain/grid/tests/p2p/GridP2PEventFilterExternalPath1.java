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
import org.gridgain.grid.events.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.resources.*;

/**
 * Simple event filter
 */
@SuppressWarnings({"ProhibitedExceptionThrown"})
public class GridP2PEventFilterExternalPath1 implements GridPredicate<GridEvent> {
    /** */
    @GridUserResource
    private transient GridTestUserResource rsrc;

    /** Instance of grid. Used for save class loader and injected resource. */
    @GridInstanceResource
    private Grid grid;

    /** {@inheritDoc} */
    @Override public boolean apply(GridEvent evt) {
        try {
            int[] res = new int[] {
                System.identityHashCode(rsrc),
                System.identityHashCode(getClass().getClassLoader())
            };

            grid.forRemotes().message().send(null, res);
        }
        catch (GridException e) {
            throw new RuntimeException(e);
        }

        return true;
    }
}
