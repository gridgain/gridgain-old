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

package org.gridgain.grid.kernal.processors.hadoop.taskexecutor.external;

import org.gridgain.grid.hadoop.*;
import org.gridgain.grid.kernal.processors.hadoop.jobtracker.*;
import org.gridgain.grid.kernal.processors.hadoop.message.*;

/**
 * Job info update request.
 */
public class GridHadoopJobInfoUpdateRequest implements GridHadoopMessage {
    /** */
    private static final long serialVersionUID = 0L;

    /** Job ID. */
    private GridHadoopJobId jobId;

    /** Job phase. */
    private GridHadoopJobPhase jobPhase;

    /** Reducers addresses. */
    private GridHadoopProcessDescriptor[] reducersAddrs;

    /**
     * @param jobId Job ID.
     * @param jobPhase Job phase.
     * @param reducersAddrs Reducers addresses.
     */
    public GridHadoopJobInfoUpdateRequest(GridHadoopJobId jobId, GridHadoopJobPhase jobPhase,
        GridHadoopProcessDescriptor[] reducersAddrs) {
        this.jobId = jobId;
        this.jobPhase = jobPhase;
        this.reducersAddrs = reducersAddrs;
    }

    /**
     * @return Job ID.
     */
    public GridHadoopJobId jobId() {
        return jobId;
    }

    /**
     * @return Job phase.
     */
    public GridHadoopJobPhase jobPhase() {
        return jobPhase;
    }

    /**
     * @return Reducers addresses.
     */
    public GridHadoopProcessDescriptor[] reducersAddresses() {
        return reducersAddrs;
    }
}
