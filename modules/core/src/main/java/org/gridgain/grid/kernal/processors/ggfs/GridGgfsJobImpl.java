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

package org.gridgain.grid.kernal.processors.ggfs;

import org.gridgain.grid.compute.*;
import org.gridgain.grid.*;
import org.gridgain.grid.ggfs.*;
import org.gridgain.grid.ggfs.mapreduce.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;

import java.io.*;

/**
 * GGFS job implementation.
 */
public class GridGgfsJobImpl implements GridComputeJob, GridInternalWrapper<GridGgfsJob> {
    /** */
    private static final long serialVersionUID = 0L;

    /** GGFS job. */
    private GridGgfsJob job;

    /** GGFS name. */
    private String ggfsName;

    /** GGFS path. */
    private GridGgfsPath path;

    /** Start. */
    private long start;

    /** Length. */
    private long len;

    /** Split resolver. */
    private GridGgfsRecordResolver rslvr;

    /** Injected grid. */
    @GridInstanceResource
    private Grid grid;

    /** Injected logger. */
    @GridLoggerResource
    private GridLogger log;

    /**
     * @param job GGFS job.
     * @param ggfsName GGFS name.
     * @param path Split path.
     * @param start Split start offset.
     * @param len Split length.
     * @param rslvr GGFS split resolver.
     */
    public GridGgfsJobImpl(GridGgfsJob job, String ggfsName, GridGgfsPath path, long start, long len,
        GridGgfsRecordResolver rslvr) {
        this.job = job;
        this.ggfsName = ggfsName;
        this.path = path;
        this.start = start;
        this.len = len;
        this.rslvr = rslvr;
    }

    /** {@inheritDoc} */
    @Override public Object execute() throws GridException {
        GridGgfs ggfs = grid.ggfs(ggfsName);

        try (GridGgfsInputStream in = ggfs.open(path)) {
            GridGgfsFileRange split = new GridGgfsFileRange(path, start, len);

            if (rslvr != null) {
                split = rslvr.resolveRecords(ggfs, in, split);

                if (split == null) {
                    log.warning("No data found for split on local node after resolver is applied " +
                        "[ggfsName=" + ggfsName + ", path=" + path + ", start=" + start + ", len=" + len + ']');

                    return null;
                }
            }

            in.seek(split.start());

            return job.execute(ggfs, new GridGgfsFileRange(path, split.start(), split.length()), in);
        }
        catch (IOException e) {
            throw new GridException("Failed to execute GGFS job for file split [ggfsName=" + ggfsName +
                ", path=" + path + ", start=" + start + ", len=" + len + ']', e);
        }
    }

    /** {@inheritDoc} */
    @Override public void cancel() {
        job.cancel();
    }

    /** {@inheritDoc} */
    @Override public GridGgfsJob userObject() {
        return job;
    }
}
