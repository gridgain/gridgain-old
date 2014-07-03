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

package org.gridgain.loadtests.direct.newnodes;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;

/**
 * Single split on new nodes test job target.
 */
public class GridSingleSplitNewNodesTestJobTarget {
    /**
     * @param level Level.
     * @param jobSes Job session.
     * @return Always returns {@code 1}.
     * @throws GridException If failed.
     */
    @SuppressWarnings("unused")
    public int executeLoadTestJob(int level, GridComputeTaskSession jobSes) throws GridException {
        assert level > 0;
        assert jobSes != null;

        try {
            assert "1".equals(jobSes.waitForAttribute("1st", 10000));

            assert "2".equals(jobSes.waitForAttribute("2nd", 10000));
        }
        catch (InterruptedException e) {
            // Fail.
            throw new GridException("Failed to wait for attribute.", e);
        }

        return 1;
    }
}
