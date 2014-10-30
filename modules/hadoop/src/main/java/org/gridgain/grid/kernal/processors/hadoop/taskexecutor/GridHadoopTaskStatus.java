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

package org.gridgain.grid.kernal.processors.hadoop.taskexecutor;

import org.gridgain.grid.hadoop.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.io.*;

/**
 * Task status.
 */
public class GridHadoopTaskStatus implements Externalizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private GridHadoopTaskState state;

    /** */
    private Throwable failCause;

    /** */
    private GridHadoopCounters cntrs;

    /**
     * Default constructor required by {@link Externalizable}.
     */
    public GridHadoopTaskStatus() {
        // No-op.
    }

    /**
     * Creates new instance.
     *
     * @param state Task state.
     * @param failCause Failure cause (if any).
     */
    public GridHadoopTaskStatus(GridHadoopTaskState state, @Nullable Throwable failCause) {
        this(state, failCause, null);
    }

    /**
     * Creates new instance.
     *
     * @param state Task state.
     * @param failCause Failure cause (if any).
     * @param cntrs Task counters.
     */
    public GridHadoopTaskStatus(GridHadoopTaskState state, @Nullable Throwable failCause,
        @Nullable GridHadoopCounters cntrs) {
        assert state != null;

        this.state = state;
        this.failCause = failCause;
        this.cntrs = cntrs;
    }

    /**
     * @return State.
     */
    public GridHadoopTaskState state() {
        return state;
    }

    /**
     * @return Fail cause.
     */
    @Nullable public Throwable failCause() {
        return failCause;
    }

    /**
     * @return Counters.
     */
    @Nullable public GridHadoopCounters counters() {
        return cntrs;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridHadoopTaskStatus.class, this);
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(state);
        out.writeObject(failCause);
        out.writeObject(cntrs);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        state = (GridHadoopTaskState)in.readObject();
        failCause = (Throwable)in.readObject();
        cntrs = (GridHadoopCounters)in.readObject();
    }
}
