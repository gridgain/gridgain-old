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

package org.gridgain.client;

import java.io.*;

/**
 * Cache metrics used to obtain statistics on cache itself or any of its entries.
 * Cache metrics can be fetched from server either for the whole cache or for
 * specific entry via any of the {@code metrics(...)} methods available on
 * {@link GridClientData} API.
 */
public interface GridClientDataMetrics extends Serializable {
    /**
     * Gets create time of the owning entity (either cache or entry).
     *
     * @return Create time.
     */
    public long createTime();

    /**
     * Gets last write time of the owning entity (either cache or entry).
     *
     * @return Last write time.
     */
    public long writeTime();

    /**
     * Gets last read time of the owning entity (either cache or entry).
     *
     * @return Last read time.
     */
    public long readTime();

    /**
     * Gets total number of reads of the owning entity (either cache or entry).
     *
     * @return Total number of reads.
     */
    public int reads();

    /**
     * Gets total number of writes of the owning entity (either cache or entry).
     *
     * @return Total number of writes.
     */
    public int writes();

    /**
     * Gets total number of hits for the owning entity (either cache or entry).
     *
     * @return Number of hits.
     */
    public int hits();

    /**
     * Gets total number of misses for the owning entity (either cache or entry).
     *
     * @return Number of misses.
     */
    public int misses();
}
