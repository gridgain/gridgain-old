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

package org.gridgain.grid.kernal.processors.cache.extras;

import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.util.*;
import org.jetbrains.annotations.*;

/**
 * Cache extras.
 */
public interface GridCacheEntryExtras<K> {
    /**
     * @return Attributes data.
     */
    @Nullable public GridLeanMap<String, Object> attributesData();

    /**
     * @param attrData Attributes data.
     * @return Updated extras.
     */
    public GridCacheEntryExtras<K> attributesData(GridLeanMap<String, Object> attrData);

    /**
     * @return MVCC.
     */
    @Nullable public GridCacheMvcc<K> mvcc();

    /**
     * @param mvcc NVCC.
     * @return Updated extras.
     */
    public GridCacheEntryExtras<K> mvcc(GridCacheMvcc<K> mvcc);

    /**
     * @return Obsolete version.
     */
    @Nullable public GridCacheVersion obsoleteVersion();

    /**
     * @param obsoleteVer Obsolete version.
     * @return Updated extras.
     */
    public GridCacheEntryExtras<K> obsoleteVersion(GridCacheVersion obsoleteVer);

    /**
     * @return TTL.
     */
    public long ttl();

    /**
     * @return Expire time.
     */
    public long expireTime();

    /**
     * @param ttl TTL.
     * @param expireTime Expire time.
     * @return Updated extras.
     */
    public GridCacheEntryExtras<K> ttlAndExpireTime(long ttl, long expireTime);

    /**
     * @return Extras size.
     */
    public int size();
}
