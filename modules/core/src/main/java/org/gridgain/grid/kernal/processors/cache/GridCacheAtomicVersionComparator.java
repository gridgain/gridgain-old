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

package org.gridgain.grid.kernal.processors.cache;

import java.io.*;
import java.util.*;

/**
 * Atomic cache version comparator.
 */
public class GridCacheAtomicVersionComparator implements Comparator<GridCacheVersion>, Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @Override public int compare(GridCacheVersion one, GridCacheVersion other) {
        int topVer = one.topologyVersion();
        int otherTopVer = other.topologyVersion();

        if (topVer == otherTopVer) {
            long globalTime = one.globalTime();
            long otherGlobalTime = other.globalTime();

            if (globalTime == otherGlobalTime) {
                long locOrder = one.order();
                long otherLocOrder = other.order();

                if (locOrder == otherLocOrder) {
                    int nodeOrder = one.nodeOrder();
                    int otherNodeOrder = other.nodeOrder();

                    return nodeOrder == otherNodeOrder ? 0 : nodeOrder < otherNodeOrder ? -1 : 1;
                }
                else
                    return locOrder > otherLocOrder ? 1 : -1;
            }
            else
                return globalTime > otherGlobalTime ? 1 : -1;
        }
        else
            return topVer > otherTopVer ? 1 : -1;
    }
}
