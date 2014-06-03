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

/**
 * {@code GGFS} metrics snapshot for the file system. Note, that some metrics are global and
 * some are local (i.e. per each node).
 */
public interface GridGgfsMetrics {
    /**
     * Gets local used space in bytes. This is the sum of all file chunks stored on local node.
     * <p>
     * This is a local metric.
     *
     * @return Node used space in bytes.
     */
    public long localSpaceSize();

    /**
     * Gets maximum amount of data that can be stored on local node. This metrics is either
     * equal to {@link GridGgfsConfiguration#getMaxSpaceSize()}, or, if it is {@code 0}, equal to
     * {@code 80%} of maximum heap size allocated for JVM.
     *
     * @return Maximum GGFS local space size.
     */
    public long maxSpaceSize();

    /**
    * Get used space in bytes used in the secondary file system.
    * <p>
    * This is a global metric.
    *
    * @return Used space in the secondary file system or {@code 0} in case no secondary file system is configured.
    */
    public long secondarySpaceSize();

    /**
     * Gets number of directories created in file system.
     * <p>
     * This is a global metric.
     *
     * @return Number of directories.
     */
    public int directoriesCount();

    /**
     * Gets number of files stored in file system.
     * <p>
     * This is a global metric.
     *
     * @return Number of files.
     */
    public int filesCount();

    /**
     * Gets number of files that are currently opened for reading.
     * <p>
     * This is a local metric.
     *
     * @return Number of opened files.
     */
    public int filesOpenedForRead();

    /**
     * Gets number of files that are currently opened for writing.
     * <p>
     * This is a local metric.
     *
     * @return Number of opened files.
     */
    public int filesOpenedForWrite();

    /**
     * Gets total blocks read, local and remote.
     * <p>
     * This is a local metric.
     *
     * @return Total blocks read.
     */
    public long blocksReadTotal();

    /**
     * Gets total remote blocks read.
     * <p>
     * This is a local metric.
     *
     * @return Total blocks remote read.
     */
    public long blocksReadRemote();

    /**
     * Gets total blocks written, local and remote.
     * <p>
     * This is a local metric.
     *
     * @return Total blocks written.
     */
    public long blocksWrittenTotal();

    /**
     * Gets total remote blocks written.
     * <p>
     * This is a local metric.
     *
     * @return Total blocks written.
     */
    public long blocksWrittenRemote();

    /**
     * Gets total bytes read.
     * <p>
     * This is a local metric.
     *
     * @return Total bytes read.
     */
    public long bytesRead();

    /**
     * Gets total bytes read time.
     * <p>
     * This is a local metric.
     *
     * @return Total bytes read time.
     */
    public long bytesReadTime();

    /**
     * Gets total bytes written.
     * <p>
     * This is a local metric.
     *
     * @return Total bytes written.
     */
    public long bytesWritten();

    /**
     * Gets total bytes write time.
     * <p>
     * This is a local metric.
     *
     * @return Total bytes write time.
     */
    public long bytesWriteTime();
}
