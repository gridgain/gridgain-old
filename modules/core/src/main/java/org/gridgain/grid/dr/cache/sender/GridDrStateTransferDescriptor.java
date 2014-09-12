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

package org.gridgain.grid.dr.cache.sender;

import org.gridgain.grid.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.io.*;
import java.util.*;

/**
 * Data center replication state transfer descriptor.
 */
public class GridDrStateTransferDescriptor implements Externalizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Unique state transfer ID. */
    private GridUuid id;

    /** Target data center IDs. */
    private Collection<Byte> dataCenterIds;

    /**
     * {@link Externalizable} support.
     */
    public GridDrStateTransferDescriptor() {
        // No-op.
    }

    /**
     * Constructor.
     *
     * @param id Unique state transfer ID.
     * @param dataCenterIds Target data center IDs.
     */
    public GridDrStateTransferDescriptor(GridUuid id, Collection<Byte> dataCenterIds) {
        this.id = id;
        this.dataCenterIds = dataCenterIds;
    }

    /**
     * Gets unique state transfer ID.
     *
     * @return Unique state transfer ID.
     */
    public GridUuid id() {
        return id;
    }

    /**
     * Gets target data center IDs.
     *
     * @return Target data center IDs.
     */
    public Collection<Byte> dataCenterIds() {
        return dataCenterIds;
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object obj) {
        return obj != null && obj instanceof GridDrStateTransferDescriptor && F.eq(id, (((GridDrStateTransferDescriptor) obj).id));
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return id.hashCode();
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        U.writeGridUuid(out, id);
        U.writeCollection(out, dataCenterIds);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        id = U.readGridUuid(in);
        dataCenterIds = U.readCollection(in);
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridDrStateTransferDescriptor.class, this);
    }
}
