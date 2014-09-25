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

import org.gridgain.grid.*;
import org.gridgain.grid.portables.*;

import java.util.*;

/**
 * Task where argument and result are {@link GridClientTestPortable}.
 */
public class GridClientPortableArgumentTask extends GridTaskSingleJobSplitAdapter {
    /** {@inheritDoc} */
    @Override protected Object executeJob(int gridSize, Object arg) throws GridException {
        Collection args = (Collection)arg;

        Iterator<Object> it = args.iterator();

        assert args.size() == 2 : args.size();

        boolean expPortable = (Boolean)it.next();

        GridClientTestPortable p;

        if (expPortable) {
            GridPortableObject obj = (GridPortableObject)it.next();

            p = obj.deserialize();
        }
        else
            p = (GridClientTestPortable)it.next();

        assert p != null;

        return new GridClientTestPortable(p.i + 1, true);
    }
}
