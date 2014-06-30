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

package org.gridgain.grid;

import org.gridgain.grid.marshaller.optimized.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.lang.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;

/**
 * This is a faster performing version of {@link UUID}. On basic tests this version is at least
 * 10x time faster for ID creation. It uses extra memory for 8-byte counter additionally to
 * internal UUID.
 */
public final class GridUuid implements Comparable<GridUuid>, Iterable<GridUuid>, Cloneable, Externalizable,
    GridOptimizedMarshallable {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    @SuppressWarnings({"NonConstantFieldWithUpperCaseName", "AbbreviationUsage", "UnusedDeclaration"})
    private static Object GG_CLASS_ID;

    /** VM ID. */
    public static final UUID VM_ID = UUID.randomUUID();

    /** */
    private static final AtomicLong cntGen = new AtomicLong(U.currentTimeMillis());

    /** */
    private UUID gid;

    /** */
    private long locId;

    /**
     * Empty constructor required for {@link Externalizable}.
     */
    public GridUuid() {
        // No-op.
    }

    /**
     * Constructs {@code GridUuid} from a global and local identifiers.
     *
     * @param gid UUID.
     * @param locId Counter.
     */
    public GridUuid(UUID gid, long locId) {
        assert gid != null;

        this.gid = gid;
        this.locId = locId;
    }

    /**
     * Gets {@link UUID} associated with local VM.
     *
     * @return {@link UUID} associated with local VM.
     */
    public static UUID vmId() {
        return VM_ID;
    }

    /**
     * Gets last generated local ID.
     *
     * @return Last generated local ID.
     */
    public static long lastLocalId() {
        return cntGen.get();
    }

    /**
     * Creates new pseudo-random ID.
     *
     * @return Newly created pseudo-random ID.
     */
    public static GridUuid randomUuid() {
        return new GridUuid(VM_ID, cntGen.incrementAndGet());
    }

    /**
     * Constructs new {@code GridUuid} based on global and local ID portions.
     *
     * @param id UUID instance.
     * @return Newly created pseudo-random ID.
     */
    public static GridUuid fromUuid(UUID id) {
        A.notNull(id, "id");

        return new GridUuid(id, cntGen.getAndIncrement());
    }

    /**
     * Converts string into {@code GridUuid}. The String must be in the format generated
     * by {@link #toString() GridUuid.toString()} method.
     *
     * @param s String to convert to {@code GridUuid}.
     * @return {@code GridUuid} instance representing given string.
     */
    public static GridUuid fromString(String s) {
        int firstDash = s.indexOf('-');

        return new GridUuid(
                UUID.fromString(s.substring(firstDash + 1)),
                Long.valueOf(new StringBuilder(s.substring(0, firstDash)).reverse().toString(), 16)
        );
    }


    /**
     * Gets a short string version of this ID. Use it only for UI where full version is
     * available to the application.
     *
     * @return Short string version of this ID.
     */
    public String shortString() {
        return new StringBuilder(Long.toHexString(locId)).reverse().toString();
    }

    /**
     * Gets global ID portion of this {@code GridUuid}.
     *
     * @return Global ID portion of this {@code GridUuid}.
     */
    public UUID globalId() {
        return gid;
    }

    /**
     * Gets local ID portion of this {@code GridUuid}.
     *
     * @return Local ID portion of this {@code GridUuid}.
     */
    public long localId() {
        return locId;
    }

    /** {@inheritDoc} */
    @Override public Object ggClassId() {
        return GG_CLASS_ID;
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        U.writeUuid(out, gid);

        out.writeLong(locId);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException {
        gid = U.readUuid(in);

        locId = in.readLong();
    }

    /** {@inheritDoc} */
    @Override public int compareTo(GridUuid o) {
        if (o == this)
            return 0;

        if (o == null)
            return 1;

        return locId < o.locId ? -1 : locId > o.locId ? 1 : gid.compareTo(o.globalId());
    }

    /** {@inheritDoc} */
    @Override public GridIterator<GridUuid> iterator() {
        return F.iterator(Collections.singleton(this), F.<GridUuid>identity(), true);
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (!(obj instanceof GridUuid))
            return false;

        GridUuid that = (GridUuid)obj;

        return that.locId == locId && that.gid.equals(gid);
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return 31 * gid.hashCode() + (int)(locId ^ (locId >>> 32));
    }

    /** {@inheritDoc} */
    @Override public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return shortString() + '-' + gid.toString();
    }
}
