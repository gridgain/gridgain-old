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

import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.io.*;

/**
 * Data center replication status.
 */
public class GridDrStatus implements Externalizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Status: not paused. */
    public static final GridDrStatus NOT_PAUSED = new GridDrStatus(null, null);

    /** Pause reason or {@code null} if not paused. */
    private GridDrPauseReason reason;

    /** Error description. */
    private String errMsg;

    /**
     * Required by {@link Externalizable}.
     */
    public GridDrStatus() {
        // No-op.
    }

    /**
     * Constructor.
     *
     * @param reason Pause reason.
     * @param errMsg Error description.
     */
    public GridDrStatus(@Nullable GridDrPauseReason reason, @Nullable String errMsg) {
        this.reason = reason;
        this.errMsg = errMsg;
    }

    /**
     * Gets pause flag.
     *
     * @return {@code True} if paused.
     */
    public boolean paused() {
        return reason != null;
    }

    /**
     * Gets pause reason.
     *
     * @return Pause reason or {@code null} if not paused.
     */
    @Nullable public GridDrPauseReason reason() {
        return reason;
    }

    /**
     * Gets error description.
     *
     * @return Error description.
     */
    @Nullable public String error() {
        return errMsg;
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        U.writeEnum(out, reason);
        U.writeString(out, errMsg);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        reason = GridDrPauseReason.fromOrdinal(in.readByte());
        errMsg = U.readString(in);
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridDrStatus.class, this);
    }
}
