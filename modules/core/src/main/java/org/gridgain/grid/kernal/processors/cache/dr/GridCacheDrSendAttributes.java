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

import org.gridgain.grid.dr.cache.sender.*;
import org.gridgain.grid.dr.hub.sender.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.io.*;

/**
 * DR sender cache attributes.
 */
public class GridCacheDrSendAttributes implements Externalizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Data center replication mode. */
    private GridDrSenderCacheMode mode;

    /** Replication sender hub load balancing policy. */
    private GridDrSenderHubLoadBalancingMode sndHubLoadBalancingPlc;

    /** Class name for replication cache entry filter. */
    private String entryFilterClsName;

    /**
     * {@link Externalizable} support.
     */
    public GridCacheDrSendAttributes() {
        // No-op.
    }

    /**
     * @param cfg Configuration.
     */
    public GridCacheDrSendAttributes(GridDrSenderCacheConfiguration cfg) {
        assert cfg != null;

        entryFilterClsName = className(cfg.getEntryFilter());
        mode = cfg.getMode();
        sndHubLoadBalancingPlc = cfg.getSenderHubLoadBalancingMode();
    }

    /**
     * @return Data center replication mode.
     */
    public GridDrSenderCacheMode mode() {
        return mode;
    }

    /**
     * @return Replication sender hub load balancing policy.
     */
    public GridDrSenderHubLoadBalancingMode senderHubLoadBalancingPolicy() {
        return sndHubLoadBalancingPlc;
    }

    /**
     * @return Class name for replication cache entry filter.
     */
    public String entryFilterClassName() {
        return entryFilterClsName;
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        U.writeEnum0(out, mode);
        U.writeEnum0(out, sndHubLoadBalancingPlc);
        U.writeString(out, entryFilterClsName);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        mode = GridDrSenderCacheMode.fromOrdinal(U.readEnumOrdinal0(in));
        sndHubLoadBalancingPlc = GridDrSenderHubLoadBalancingMode.fromOrdinal(U.readEnumOrdinal0(in));
        entryFilterClsName = U.readString(in);
    }

    /**
     * @param obj Object to get class of.
     * @return Class name or {@code null}.
     */
    @Nullable private static String className(@Nullable Object obj) {
        return obj != null ? obj.getClass().getName() : null;
    }
}
