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

package org.gridgain.grid.kernal.processors.cache;

import java.io.*;

/**
 * Extended cache version which also has additional DR version.
 */
public class GridCacheVersionEx extends GridCacheVersion {
    /** */
    private static final long serialVersionUID = 0L;

    /** DR version. */
    private GridCacheVersion drVer;

    /**
     * {@link Externalizable} support.
     */
    public GridCacheVersionEx() {
        // No-op.
    }

    /**
     * Constructor.
     *
     * @param topVer Topology version.
     * @param globalTime Global time.
     * @param order Order.
     * @param nodeOrder Node order.
     * @param dataCenterId Data center ID.
     * @param drVer DR version.
     */
    public GridCacheVersionEx(int topVer, long globalTime, long order, int nodeOrder, byte dataCenterId,
        GridCacheVersion drVer) {
        super(topVer, globalTime, order, nodeOrder, dataCenterId);

        assert drVer != null && !(drVer instanceof GridCacheVersionEx); // DR version can only be plain here.

        this.drVer = drVer;
    }

    /**
     * Constructor.
     *
     * @param topVer Topology version.
     * @param nodeOrderDrId Node order and DR ID.
     * @param globalTime Globally adjusted time.
     * @param order Version order.
     * @param drVer DR version.
     */
    public GridCacheVersionEx(int topVer, int nodeOrderDrId, long globalTime, long order, GridCacheVersion drVer) {
        super(topVer, nodeOrderDrId, globalTime, order);

        assert drVer != null && !(drVer instanceof GridCacheVersionEx); // DR version can only be plain here.

        this.drVer = drVer;
    }

    /** {@inheritDoc} */
    @Override public GridCacheVersion drVersion() {
        return drVer;
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException {
        super.readExternal(in);

        drVer = new GridCacheVersion();

        drVer.readExternal(in);
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);

        drVer.writeExternal(out);
    }
}
