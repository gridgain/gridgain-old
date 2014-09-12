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

import org.gridgain.grid.dr.cache.receiver.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.io.*;

/**
 * DR receiver cache attributes.
 */
public class GridCacheDrReceiveAttributes implements Externalizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Policy for conflict resolver. */
    private GridDrReceiverCacheConflictResolverMode conflictRslvrMode;

    /** Conflict resolver class name. */
    private String conflictRslvrClsName;

    /**
     * {@link Externalizable} support.
     */
    public GridCacheDrReceiveAttributes() {
        // No-op.
    }

    /**
     * @param cfg Configuration.
     */
    public GridCacheDrReceiveAttributes(GridDrReceiverCacheConfiguration cfg) {
        assert cfg != null;

        conflictRslvrClsName = className(cfg.getConflictResolver());
        conflictRslvrMode = cfg.getConflictResolverMode();
    }

    /**
     * @return Policy for conflict resolver.
     */
    public GridDrReceiverCacheConflictResolverMode conflictResolverMode() {
        return conflictRslvrMode;
    }

    /**
     * @return Conflict resolver class name.
     */
    public String conflictResolverClassName() {
        return conflictRslvrClsName;
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        U.writeEnum0(out, conflictRslvrMode);
        U.writeString(out, conflictRslvrClsName);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        conflictRslvrMode = GridDrReceiverCacheConflictResolverMode.fromOrdinal(U.readEnumOrdinal0(in));
        conflictRslvrClsName = U.readString(in);
    }

    /**
     * @param obj Object to get class of.
     * @return Class name or {@code null}.
     */
    @Nullable private static String className(@Nullable Object obj) {
        return obj != null ? obj.getClass().getName() : null;
    }
}
