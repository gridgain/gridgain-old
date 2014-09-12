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

package org.gridgain.grid.ggfs.mapreduce;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.ggfs.*;

import java.io.*;

/**
 * Defines executable unit for {@link GridGgfsTask}. Before this job is executed, it is assigned one of the
 * ranges provided by the {@link GridGgfsRecordResolver} passed to one of the {@code GridGgfs.execute(...)} methods.
 * <p>
 * {@link #execute(GridGgfs, GridGgfsFileRange, GridGgfsInputStream)} method is given {@link GridGgfsFileRange} this
 * job is expected to operate on, and already opened {@link GridGgfsInputStream} for the file this range belongs to.
 * <p>
 * Note that provided input stream has position already adjusted to range start. However, it will not
 * automatically stop on range end. This is done to provide capability in some cases to look beyond
 * the range end or seek position before the reange start.
 * <p>
 * In majority of the cases, when you want to process only provided range, you should explicitly control amount
 * of returned data and stop at range end. You can also use {@link GridGgfsInputStreamJobAdapter}, which operates
 * on {@link GridGgfsRangeInputStream} bounded to range start and end, or manually wrap provided input stream with
 * {@link GridGgfsRangeInputStream}.
 * <p>
 * You can inject any resources in concrete implementation, just as with regular {@link GridComputeJob} implementations.
 */
public interface GridGgfsJob {
    /**
     * Executes this job.
     *
     * @param ggfs GGFS instance.
     * @param range File range aligned to record boundaries.
     * @param in Input stream for split file. This input stream is not aligned to range and points to file start
     *     by default.
     * @return Execution result.
     * @throws GridException If execution failed.
     * @throws IOException If file system operation resulted in IO exception.
     */
    public Object execute(GridGgfs ggfs, GridGgfsFileRange range, GridGgfsInputStream in) throws GridException,
        IOException;

    /**
     * This method is called when system detects that completion of this
     * job can no longer alter the overall outcome (for example, when parent task
     * has already reduced the results). Job is also cancelled when
     * {@link GridComputeTaskFuture#cancel()} is called.
     * <p>
     * Note that job cancellation is only a hint, and just like with
     * {@link Thread#interrupt()}  method, it is really up to the actual job
     * instance to gracefully finish execution and exit.
     */
    public void cancel();
}
