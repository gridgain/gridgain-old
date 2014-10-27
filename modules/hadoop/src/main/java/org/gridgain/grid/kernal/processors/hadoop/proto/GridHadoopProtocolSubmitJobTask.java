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

package org.gridgain.grid.kernal.processors.hadoop.proto;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.hadoop.*;

import java.util.*;

import static org.gridgain.grid.hadoop.GridHadoopJobPhase.*;

/**
 * Submit job task.
 */
public class GridHadoopProtocolSubmitJobTask extends GridHadoopProtocolTaskAdapter<GridHadoopJobStatus> {
    /** */
    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @Override public GridHadoopJobStatus run(GridComputeJobContext jobCtx, GridHadoop hadoop,
        GridHadoopProtocolTaskArguments args) throws GridException {
        UUID nodeId = UUID.fromString(args.<String>get(0));
        Integer id = args.get(1);
        GridHadoopDefaultJobInfo info = args.get(2);

        assert nodeId != null;
        assert id != null;
        assert info != null;

        GridHadoopJobId jobId = new GridHadoopJobId(nodeId, id);

        hadoop.submit(jobId, info);

        GridHadoopJobStatus res = hadoop.status(jobId);

        if (res == null) { // Submission failed.
            res = new GridHadoopJobStatus(jobId, info.jobName(), info.user(), 0, 0, 0, 0,
                PHASE_CANCELLING, true, 1);
        }

        return res;
    }
}
