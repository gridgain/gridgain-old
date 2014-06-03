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

package org.gridgain.grid.spi.checkpoint.sharedfs;

import org.gridgain.grid.util.typedef.internal.*;

/**
 * Helper class that keeps checkpoint expiration date and last file
 * access date inside. This class used by {@link GridSharedFsTimeoutTask}
 * to track and delete obsolete files.
 */
class GridSharedFsTimeData {
    /** Checkpoint expiration date. */
    private long expTime;

    /** File last access date. */
    private long lastAcsTime;

    /** Key of checkpoint. */
    private String key;

    /**
     * Creates new instance of checkpoint time information.
     *
     * @param expTime Checkpoint expiration time.
     * @param lastAcsTime File last access time.
     * @param key Key of checkpoint.
     */
    GridSharedFsTimeData(long expTime, long lastAcsTime, String key) {
        assert expTime >= 0;
        assert lastAcsTime > 0;

        this.lastAcsTime = lastAcsTime;
        this.expTime = expTime;
        this.key = key;
    }

    /**
     * Gets checkpoint expiration time.
     *
     * @return Expire time.
     */
    long getExpireTime() {
        return expTime;
    }

    /**
     * Sets checkpoint expiration time.
     *
     * @param expTime Checkpoint time-to-live value.
     */
    void setExpireTime(long expTime) {
        assert expTime >= 0;

        this.expTime = expTime;
    }

    /**
     * Gets last file access time.
     *
     * @return Saved time.
     */
    long getLastAccessTime() {
        return lastAcsTime;
    }

    /**
     * Sets file last access time. This time usually is the same as file last
     * modification date.
     *
     * @param lastAcsTime File access time in milliseconds.
     */
    void setLastAccessTime(long lastAcsTime) {
        assert lastAcsTime > 0;

        this.lastAcsTime = lastAcsTime;
    }

    /**
     * Gets key of checkpoint.
     *
     * @return Key of checkpoint.
     */
    String getKey() {
        return key;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridSharedFsTimeData.class, this);
    }
}
