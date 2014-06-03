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

package org.gridgain.grid.cache.affinity;

import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.tostring.*;

import java.io.*;

/**
 * Optional wrapper for cache keys to provide support
 * for custom affinity mapping. The value returned by
 * {@link #affinityKey(Object)} method will be used for key-to-node
 * affinity.
 * <p>
 * Note that the {@link #equals(Object)} and {@link #hashCode()} methods
 * delegate directly to the wrapped cache key provided by {@link #key()}
 * method.
 * <p>
 * This class is optional and does not have to be used. It only provides
 * extra convenience whenever custom affinity mapping is required. Here is
 * an example of how {@code Person} objects can be collocated with
 * {@code Company} objects they belong to:
 * <pre name="code" class="java">
 * Object personKey = new GridCacheAffinityKey(myPersonId, myCompanyId);
 *
 * // Both, the company and the person objects will be cached on the same node.
 * cache.put(myCompanyId, new Company(..));
 * cache.put(personKey, new Person(..));
 * </pre>
 * <p>
 * For more information and examples of cache affinity refer to
 * {@link GridCacheAffinityKeyMapper} and {@link GridCacheAffinityKeyMapped @GridCacheAffinityKeyMapped}
 * documentation.
 * @see GridCacheAffinityKeyMapped
 * @see GridCacheAffinityKeyMapper
 * @see GridCacheAffinityFunction
 */
public class GridCacheAffinityKey<K> implements Externalizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Key. */
    @GridToStringInclude
    private K key;

    /** Affinity key. */
    @GridToStringInclude
    private Object affKey;

    /**
     * Empty constructor.
     */
    public GridCacheAffinityKey() {
        // No-op.
    }

    /**
     * Initializes key wrapper for a given key. If affinity key
     * is not initialized, then this key will be used for affinity.
     *
     * @param key Key.
     */
    public GridCacheAffinityKey(K key) {
        A.notNull(key, "key");

        this.key = key;
    }

    /**
     * Initializes key together with its affinity key counter-part.
     *
     * @param key Key.
     * @param affKey Affinity key.
     */
    public GridCacheAffinityKey(K key, Object affKey) {
        A.notNull(key, "key");

        this.key = key;
        this.affKey = affKey;
    }

    /**
     * Gets wrapped key.
     *
     * @return Wrapped key.
     */
    public K key() {
        return key;
    }

    /**
     * Sets wrapped key.
     *
     * @param key Wrapped key.
     */
    public void key(K key) {
        this.key = key;
    }

    /**
     * Gets affinity key to use for affinity mapping. If affinity key is not provided,
     * then {@code key} value will be returned.
     * <p>
     * This method is annotated with {@link GridCacheAffinityKeyMapped} and will be picked up
     * by {@link GridCacheDefaultAffinityKeyMapper} automatically.
     *
     * @return Affinity key to use for affinity mapping.
     */
    @GridCacheAffinityKeyMapped
    @SuppressWarnings({"unchecked"})
    public <T> T affinityKey() {
        A.notNull(key, "key");

        return (T)(affKey == null ? key : affKey);
    }

    /**
     * Sets affinity key to use for affinity mapping. If affinity key is not provided,
     * then {@code key} value will be returned.
     *
     * @param affKey Affinity key to use for affinity mapping.
     */
    public void affinityKey(Object affKey) {
        this.affKey = affKey;
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(key);
        out.writeObject(affKey);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        key = (K)in.readObject();
        affKey = in.readObject();
    }

    /**
     * Hash code implementation which delegates to the underlying {@link #key()}. Note, however,
     * that different subclasses of {@code GridCacheAffinityKey} will produce different hash codes.
     * <p>
     * Users should override this method if different behavior is desired.
     *
     * @return Hash code.
     */
    @Override public int hashCode() {
        A.notNull(key, "key");

        return 31 * key.hashCode() + getClass().getName().hashCode();
    }

    /**
     * Equality check which delegates to the underlying key equality. Note, however, that
     * different subclasses of {@code GridCacheAffinityKey} will never be equal.
     * <p>
     * Users should override this method if different behavior is desired.
     *
     * @param obj Object to check for equality.
     * @return {@code True} if objects are equal.
     */
    @Override public boolean equals(Object obj) {
        A.notNull(key, "key");

        return obj != null && getClass() == obj.getClass() && key.equals(((GridCacheAffinityKey)obj).key);
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridCacheAffinityKey.class, this);
    }
}
