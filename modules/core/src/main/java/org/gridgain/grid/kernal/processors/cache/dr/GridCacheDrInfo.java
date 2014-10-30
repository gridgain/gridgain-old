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

package org.gridgain.grid.kernal.processors.cache.dr;

import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.io.*;

/**
 * Cache DR info used as argument in PUT cache internal interfaces.
 */
public class GridCacheDrInfo<V> implements Externalizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Value. */
    private V val;

    /** DR version. */
    private GridCacheVersion ver;

    /**
     * {@link Externalizable} support.
     */
    public GridCacheDrInfo() {
        // No-op.
    }

    /**
     * Constructor.
     *
     * @param val Value.
     * @param ver Version.
     */
    public GridCacheDrInfo(V val, GridCacheVersion ver) {
        assert val != null;
        assert ver != null;

        this.val = val;
        this.ver = ver;
    }

    /**
     * @return Value.
     */
    public V value() {
        return val;
    }

    /**
     * @return Version.
     */
    public GridCacheVersion version() {
        return ver;
    }

    /**
     * @return TTL.
     */
    public long ttl() {
        return 0L;
    }

    /**
     * @return Expire time.
     */
    public long expireTime() {
        return 0L;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridCacheDrInfo.class, this);
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(val);
        CU.writeVersion(out, ver);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        val = (V)in.readObject();
        ver = CU.readVersion(in);
    }
}
