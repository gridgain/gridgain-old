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

package org.gridgain.grid.ggfs;

import org.gridgain.grid.util.typedef.internal.*;

import java.io.*;

/**
 * Path summary: total files count, total directories count, total length.
 */
public class GridGgfsPathSummary implements Externalizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Path. */
    private GridGgfsPath path;

    /** File count. */
    private int filesCnt;

    /** Directories count. */
    private int dirCnt;

    /** Length consumed. */
    private long totalLen;

    /**
     * Empty constructor required by {@link Externalizable}.
     */
    public GridGgfsPathSummary() {
        // No-op.
    }

    /**
     * Construct empty path summary.
     *
     * @param path Path.
     */
    public GridGgfsPathSummary(GridGgfsPath path) {
        this.path = path;
    }

    /**
     * @return Files count.
     */
    public int filesCount() {
        return filesCnt;
    }

    /**
     * @param filesCnt Files count.
     */
    public void filesCount(int filesCnt) {
        this.filesCnt = filesCnt;
    }

    /**
     * @return Directories count.
     */
    public int directoriesCount() {
        return dirCnt;
    }

    /**
     * @param dirCnt Directories count.
     */
    public void directoriesCount(int dirCnt) {
        this.dirCnt = dirCnt;
    }

    /**
     * @return Total length.
     */
    public long totalLength() {
        return totalLen;
    }

    /**
     * @param totalLen Total length.
     */
    public void totalLength(long totalLen) {
        this.totalLen = totalLen;
    }

    /**
     * @return Path for which summary is obtained.
     */
    public GridGgfsPath path() {
        return path;
    }

    /**
     * @param path Path for which summary is obtained.
     */
    public void path(GridGgfsPath path) {
        this.path = path;
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(filesCnt);
        out.writeInt(dirCnt);
        out.writeLong(totalLen);

        path.writeExternal(out);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        filesCnt = in.readInt();
        dirCnt = in.readInt();
        totalLen = in.readLong();

        path = new GridGgfsPath();
        path.readExternal(in);
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridGgfsPathSummary.class, this);
    }
}
