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
import org.gridgain.grid.util.typedef.internal.*;

/**
 * Extras where MVCC, obsolete version and TTL are set.
 */
public class GridCacheMvccObsoleteTtlEntryExtras<K> extends GridCacheEntryExtrasAdapter<K> {
    /** MVCC. */
    private GridCacheMvcc<K> mvcc;

    /** Obsolete version. */
    private GridCacheVersion obsoleteVer;

    /** TTL. */
    private long ttl;

    /** Expire time. */
    private long expireTime;

    /**
     * Constructor.
     *
     * @param mvcc MVCC.
     * @param obsoleteVer Obsolete version.
     * @param ttl TTL.
     * @param expireTime Expire time.
     */
    public GridCacheMvccObsoleteTtlEntryExtras(GridCacheMvcc<K> mvcc, GridCacheVersion obsoleteVer, long ttl,
        long expireTime) {
        assert mvcc != null;
        assert obsoleteVer != null;
        assert ttl != 0;

        this.mvcc = mvcc;
        this.obsoleteVer = obsoleteVer;
        this.ttl = ttl;
        this.expireTime = expireTime;
    }

    /** {@inheritDoc} */
    @Override public GridCacheEntryExtras<K> attributesData(GridLeanMap<String, Object> attrData) {
        return attrData != null ? new GridCacheAttributesMvccObsoleteTtlEntryExtras<>(attrData, mvcc, obsoleteVer, ttl,
            expireTime) : this;
    }

    /** {@inheritDoc} */
    @Override public GridCacheMvcc<K> mvcc() {
        return mvcc;
    }

    /** {@inheritDoc} */
    @Override public GridCacheEntryExtras<K> mvcc(GridCacheMvcc<K> mvcc) {
        if (mvcc != null) {
            this.mvcc = mvcc;

            return this;
        }
        else
            return new GridCacheObsoleteTtlEntryExtras<>(obsoleteVer, ttl, expireTime);
    }

    /** {@inheritDoc} */
    @Override public GridCacheVersion obsoleteVersion() {
        return obsoleteVer;
    }

    /** {@inheritDoc} */
    @Override public GridCacheEntryExtras<K> obsoleteVersion(GridCacheVersion obsoleteVer) {
        if (obsoleteVer != null) {
            this.obsoleteVer = obsoleteVer;

            return this;
        }
        else
            return new GridCacheMvccTtlEntryExtras<>(mvcc, ttl, expireTime);
    }

    /** {@inheritDoc} */
    @Override public long ttl() {
        return ttl;
    }

    /** {@inheritDoc} */
    @Override public long expireTime() {
        return expireTime;
    }

    /** {@inheritDoc} */
    @Override public GridCacheEntryExtras<K> ttlAndExpireTime(long ttl, long expireTime) {
        if (ttl != 0) {
            this.ttl = ttl;
            this.expireTime = expireTime;

            return this;
        }
        else
            return new GridCacheMvccObsoleteEntryExtras<>(mvcc, obsoleteVer);
    }

    /** {@inheritDoc} */
    @Override public int size() {
        return 32;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridCacheMvccObsoleteTtlEntryExtras.class, this);
    }
}
