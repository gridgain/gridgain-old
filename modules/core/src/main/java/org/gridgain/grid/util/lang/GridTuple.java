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

package org.gridgain.grid.util.lang;

import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.tostring.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;

/**
 * Convenience class representing mutable tuple of a single value.
 * <h2 class="header">Thread Safety</h2>
 * This class doesn't provide any synchronization for multi-threaded access
 * and it is responsibility of the user of this class to provide outside
 * synchronization, if needed.
 * @see GridFunc#t1()
 * @see GridFunc#t(Object)
 */
public class GridTuple<V> implements Iterable<V>, Cloneable, Externalizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** The value to wrap. */
    @GridToStringInclude
    private V val;

    /**
     * Empty constructor required by {@link Externalizable}.
     */
    public GridTuple() {
        // No-op.
    }

    /**
     * Constructs mutable object with given value.
     *
     * @param val Wrapped value.
     */
    public GridTuple(@Nullable V val) {
        this.val = val;
    }

    /**
     * Gets value.
     *
     * @return Wrapped value.
     */
    @Nullable public V get() {
        return val;
    }

    /**
     * Sets value.
     *
     * @param val Value to set.
     */
    public void set(@Nullable V val) {
        this.val = val;
    }

    /** {@inheritDoc} */
    @Override public Iterator<V> iterator() {
        return new Iterator<V>() {
            private boolean hasNext = true;

            @Override public boolean hasNext() {
                return hasNext;
            }

            @Override public V next() {
                if (!hasNext())
                    throw new NoSuchElementException();

                hasNext = false;

                return val;
            }

            @Override public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"CloneDoesntDeclareCloneNotSupportedException"})
    @Override public Object clone() {
        try {
            return super.clone();
        }
        catch (CloneNotSupportedException ignore) {
            throw new InternalError();
        }
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(val);
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"unchecked"})
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        val = (V)in.readObject();
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return val == null ? 0 : val.hashCode();
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (!(obj instanceof GridTuple))
            return false;

        GridTuple t = (GridTuple)obj;

        // Both nulls or equals.
        return val == null ? t.val == null : val.equals(t.val);
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridTuple.class, this);
    }
}
