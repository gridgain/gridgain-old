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

package org.gridgain.grid.marshaller.optimized;

import org.gridgain.grid.lang.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.io.*;
import org.gridgain.grid.util.typedef.*;
import sun.misc.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import static org.gridgain.grid.marshaller.optimized.GridOptimizedMarshallerUtils.*;

/**
 * Optimized object output stream.
 */
class GridOptimizedObjectOutputStream extends ObjectOutputStream {
    /** */
    private static final Collection<String> CONVERTED_ERR = F.asList(
        "weblogic/management/ManagementException",
        "Externalizable class doesn't have default constructor: class " +
            "org.gridgain.grid.kernal.processors.email.GridEmailProcessor$2"
    );

    /** */
    private final HandleTable handles = new HandleTable(10, 3.00f);

    /** */
    private boolean requireSer;

    /** */
    private GridDataOutput out;

    /** */
    private Object curObj;

    /** */
    private List<T2<GridOptimizedFieldType, Long>> curFields;

    /** */
    private PutFieldImpl curPut;


    /**
     * @throws IOException In case of error.
     */
    GridOptimizedObjectOutputStream() throws IOException {
        // No-op.
    }

    /**
     * @param out Output.
     * @throws IOException In case of error.
     */
    GridOptimizedObjectOutputStream(GridDataOutput out) throws IOException {
        this.out = out;
    }

    /**
     * @param requireSer Require {@link Serializable} flag.
     */
    void requireSerializable(boolean requireSer) {
        this.requireSer = requireSer;
    }

    /**
     * @return Require {@link Serializable} flag.
     */
    boolean requireSerializable() {
        return requireSer;
    }

    /**
     * @param out Output.
     */
    public void out(GridDataOutput out) {
        this.out = out;
    }

    /**
     * @return Output.
     */
    public GridDataOutput out() {
        return out;
    }

    /** {@inheritDoc} */
    @Override public void close() throws IOException {
        reset();
    }

    /** {@inheritDoc} */
    @Override public void write(byte[] b) throws IOException {
        out.write(b);
    }

    /** {@inheritDoc} */
    @Override public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
    }

    /** {@inheritDoc} */
    @Override protected void writeObjectOverride(Object obj) throws IOException {
        try {
            writeObject0(obj);
        }
        catch (IOException e) {
            Throwable t = e;

            do {
                if (CONVERTED_ERR.contains(t.getMessage()))
                    throw new IOException("You are trying to serialize internal classes that are not supposed " +
                        "to be serialized. Check that all non-serializable fields are transient. Consider using " +
                        "static inner classes instead of non-static inner classes and anonymous classes.", e);
            }
            while ((t = t.getCause()) != null);

            throw e;
        }
    }

    /**
     * Writes object to stream.
     *
     * @param obj Object.
     * @throws IOException In case of error.
     */
    private void writeObject0(Object obj) throws IOException {
        curObj = null;
        curFields = null;
        curPut = null;

        if (obj == null)
            writeByte(NULL);
        else {
            Class<?> cls = obj.getClass();

            GridOptimizedClassDescriptor desc = classDescriptor(cls, obj);

            if (desc.excluded()) {
                writeByte(NULL);

                return;
            }

            Object obj0 = desc.replace(obj);

            if (obj0 == null) {
                writeByte(NULL);

                return;
            }

            int handle = -1;

            if (!desc.isPrimitive() && !desc.isEnum() && !desc.isClass())
                handle = handles.lookup(obj);

            if (obj0 != obj) {
                obj = obj0;

                desc = classDescriptor(obj.getClass(), obj);
            }

            if (handle >= 0) {
                writeByte(HANDLE);
                writeInt(handle);
            }
            else {
                writeByte(OBJECT);

                GridOptimizedClassResolver.writeClass(this, desc);

                desc.write(this, obj);
            }
        }
    }

    /**
     * Writes array to this stream.
     *
     * @param arr Array.
     * @throws IOException In case of error.
     */
    @SuppressWarnings("ForLoopReplaceableByForEach")
    void writeArray(Object[] arr) throws IOException {
        int len = arr.length;

        writeInt(len);

        for (int i = 0; i < len; i++) {
            Object obj = arr[i];

            writeObject0(obj);
        }
    }

    /**
     * Writes {@link UUID} to this stream.
     *
     * @param uuid UUID.
     * @throws IOException In case of error.
     */
    void writeUuid(UUID uuid) throws IOException {
        writeLong(uuid.getMostSignificantBits());
        writeLong(uuid.getLeastSignificantBits());
    }

    /**
     * Writes {@link Properties} to this stream.
     *
     * @param props Properties.
     * @param dfltsFieldOff Defaults field offset.
     * @throws IOException In case of error.
     */
    void writeProperties(Properties props, long dfltsFieldOff) throws IOException {
        Properties dflts = (Properties)getObject(props, dfltsFieldOff);

        if (dflts == null)
            writeBoolean(true);
        else {
            writeBoolean(false);

            writeObject0(dflts);
        }

        Set<String> names = props.stringPropertyNames();

        writeInt(names.size());

        for (String name : names) {
            writeUTF(name);
            writeUTF(props.getProperty(name));
        }
    }

    /**
     * Writes externalizable object.
     *
     * @param obj Object.
     * @throws IOException In case of error.
     */
    void writeExternalizable(Object obj) throws IOException {
        Externalizable extObj = (Externalizable)obj;

        extObj.writeExternal(this);
    }

    /**
     * Writes serializable object.
     *
     * @param obj Object.
     * @param fieldOffs Field offsets.
     * @param mtds {@code writeObject} methods.
     * @throws IOException In case of error.
     */
    @SuppressWarnings("ForLoopReplaceableByForEach")
    void writeSerializable(Object obj, List<List<T2<GridOptimizedFieldType, Long>>> fieldOffs, List<Method> mtds)
        throws IOException {
        for (int i = 0; i < mtds.size(); i++) {
            Method mtd = mtds.get(i);

            if (mtd != null) {
                curObj = obj;
                curFields = fieldOffs.get(i);

                try {
                    mtd.invoke(obj, this);
                }
                catch (IllegalAccessException | InvocationTargetException e) {
                    throw new IOException(e);
                }
            }
            else
                writeFields(obj, fieldOffs.get(i));
        }
    }

    /**
     * Writes {@link ArrayList}.
     *
     * @param list List.
     * @throws IOException In case of error.
     */
    @SuppressWarnings({"ForLoopReplaceableByForEach", "TypeMayBeWeakened"})
    void writeArrayList(ArrayList<?> list) throws IOException {
        int size = list.size();

        writeInt(size);

        for (int i = 0; i < size; i++)
            writeObject0(list.get(i));
    }

    /**
     * Writes {@link HashMap}.
     *
     * @param map Map.
     * @param loadFactorFieldOff Load factor field offset.
     * @param set Whether writing underlying map from {@link HashSet}.
     * @throws IOException In case of error.
     */
    @SuppressWarnings("TypeMayBeWeakened")
    void writeHashMap(HashMap<?, ?> map, long loadFactorFieldOff, boolean set) throws IOException {
        int size = map.size();

        writeInt(size);
        writeFloat(getFloat(map, loadFactorFieldOff));

        for (Map.Entry<?, ?> e : map.entrySet()) {
            writeObject0(e.getKey());

            if (!set)
                writeObject0(e.getValue());
        }
    }

    /**
     * Writes {@link HashSet}.
     *
     * @param set Set.
     * @param mapFieldOff Map field offset.
     * @param loadFactorFieldOff Load factor field offset.
     * @throws IOException In case of error.
     */
    void writeHashSet(HashSet<?> set, long mapFieldOff, long loadFactorFieldOff) throws IOException {
        writeHashMap((HashMap<?, ?>)getObject(set, mapFieldOff), loadFactorFieldOff, true);
    }

    /**
     * Writes {@link LinkedList}.
     *
     * @param list List.
     * @throws IOException In case of error.
     */
    @SuppressWarnings("TypeMayBeWeakened")
    void writeLinkedList(LinkedList<?> list) throws IOException {
        int size = list.size();

        writeInt(size);

        for (Object obj : list)
            writeObject0(obj);
    }

    /**
     * Writes {@link LinkedHashMap}.
     *
     * @param map Map.
     * @param loadFactorFieldOff Load factor field offset.
     * @param accessOrderFieldOff access order field offset.
     * @param set Whether writing underlying map from {@link LinkedHashSet}.
     * @throws IOException In case of error.
     */
    @SuppressWarnings("TypeMayBeWeakened")
    void writeLinkedHashMap(LinkedHashMap<?, ?> map, long loadFactorFieldOff, long accessOrderFieldOff, boolean set)
        throws IOException {
        int size = map.size();

        writeInt(size);
        writeFloat(getFloat(map, loadFactorFieldOff));

        if (accessOrderFieldOff >= 0)
            writeBoolean(getBoolean(map, accessOrderFieldOff));
        else
            writeBoolean(false);

        for (Map.Entry<?, ?> e : map.entrySet()) {
            writeObject0(e.getKey());

            if (!set)
                writeObject0(e.getValue());
        }
    }

    /**
     * Writes {@link LinkedHashSet}.
     *
     * @param set Set.
     * @param mapFieldOff Map field offset.
     * @param loadFactorFieldOff Load factor field offset.
     * @throws IOException In case of error.
     */
    void writeLinkedHashSet(LinkedHashSet<?> set, long mapFieldOff, long loadFactorFieldOff) throws IOException {
        LinkedHashMap<?, ?> map = (LinkedHashMap<?, ?>)getObject(set, mapFieldOff);

        writeLinkedHashMap(map, loadFactorFieldOff, -1, true);
    }

    /**
     * Writes {@link Date}.
     *
     * @param date Date.
     * @throws IOException In case of error.
     */
    void writeDate(Date date) throws IOException {
        writeLong(date.getTime());
    }

    /**
     * Writes all non-static and non-transient field values to this stream.
     *
     * @param obj Object.
     * @param fieldOffs Field offsets.
     * @throws IOException In case of error.
     */
    @SuppressWarnings("ForLoopReplaceableByForEach")
    private void writeFields(Object obj, List<T2<GridOptimizedFieldType, Long>> fieldOffs) throws IOException {
        for (int i = 0; i < fieldOffs.size(); i++) {
            T2<GridOptimizedFieldType, Long> t = fieldOffs.get(i);

            switch (t.get1()) {
                case BYTE:
                    writeByte(getByte(obj, t.get2()));

                    break;

                case SHORT:
                    writeShort(getShort(obj, t.get2()));

                    break;

                case INT:
                    writeInt(getInt(obj, t.get2()));

                    break;

                case LONG:
                    writeLong(getLong(obj, t.get2()));

                    break;

                case FLOAT:
                    writeFloat(getFloat(obj, t.get2()));

                    break;

                case DOUBLE:
                    writeDouble(getDouble(obj, t.get2()));

                    break;

                case CHAR:
                    writeChar(getChar(obj, t.get2()));

                    break;

                case BOOLEAN:
                    writeBoolean(getBoolean(obj, t.get2()));

                    break;

                case OTHER:
                    writeObject0(getObject(obj, t.get2()));
            }
        }
    }

    /**
     * Writes array of {@code byte}s.
     *
     * @param arr Array.
     * @throws IOException In case of error.
     */
    void writeByteArray(byte[] arr) throws IOException {
        out.writeByteArray(arr);
    }

    /**
     * Writes array of {@code short}s.
     *
     * @param arr Array.
     * @throws IOException In case of error.
     */
    void writeShortArray(short[] arr) throws IOException {
        out.writeShortArray(arr);
    }

    /**
     * Writes array of {@code int}s.
     *
     * @param arr Array.
     * @throws IOException In case of error.
     */
    void writeIntArray(int[] arr) throws IOException {
        out.writeIntArray(arr);
    }

    /**
     * Writes array of {@code long}s.
     *
     * @param arr Array.
     * @throws IOException In case of error.
     */
    void writeLongArray(long[] arr) throws IOException {
        out.writeLongArray(arr);
    }

    /**
     * Writes array of {@code float}s.
     *
     * @param arr Array.
     * @throws IOException In case of error.
     */
    void writeFloatArray(float[] arr) throws IOException {
        out.writeFloatArray(arr);
    }

    /**
     * Writes array of {@code double}s.
     *
     * @param arr Array.
     * @throws IOException In case of error.
     */
    void writeDoubleArray(double[] arr) throws IOException {
        out.writeDoubleArray(arr);
    }

    /**
     * Writes array of {@code char}s.
     *
     * @param arr Array.
     * @throws IOException In case of error.
     */
    void writeCharArray(char[] arr) throws IOException {
        out.writeCharArray(arr);
    }

    /**
     * Writes array of {@code boolean}s.
     *
     * @param arr Array.
     * @throws IOException In case of error.
     */
    void writeBooleanArray(boolean[] arr) throws IOException {
        out.writeBooleanArray(arr);
    }

    /**
     * Writes {@link String}.
     *
     * @param str String.
     * @throws IOException In case of error.
     */
    void writeString(String str) throws IOException {
        out.writeUTF(str);
    }

    /** {@inheritDoc} */
    @Override public void writeBoolean(boolean v) throws IOException {
        out.writeBoolean(v);
    }

    /** {@inheritDoc} */
    @Override public void writeByte(int v) throws IOException {
        out.writeByte(v);
    }

    /** {@inheritDoc} */
    @Override public void writeShort(int v) throws IOException {
        out.writeShort(v);
    }

    /** {@inheritDoc} */
    @Override public void writeChar(int v) throws IOException {
        out.writeChar(v);
    }

    /** {@inheritDoc} */
    @Override public void writeInt(int v) throws IOException {
        out.writeInt(v);
    }

    /** {@inheritDoc} */
    @Override public void writeLong(long v) throws IOException {
        out.writeLong(v);
    }

    /** {@inheritDoc} */
    @Override public void writeFloat(float v) throws IOException {
        out.writeFloat(v);
    }

    /** {@inheritDoc} */
    @Override public void writeDouble(double v) throws IOException {
        out.writeDouble(v);
    }

    /** {@inheritDoc} */
    @Override public void write(int b) throws IOException {
        writeByte(b);
    }

    /** {@inheritDoc} */
    @Override public void writeBytes(String s) throws IOException {
        out.writeBytes(s);
    }

    /** {@inheritDoc} */
    @Override public void writeChars(String s) throws IOException {
        out.writeChars(s);
    }

    /** {@inheritDoc} */
    @Override public void writeUTF(String s) throws IOException {
        out.writeUTF(s);
    }

    /** {@inheritDoc} */
    @Override public void useProtocolVersion(int ver) throws IOException {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public void writeUnshared(Object obj) throws IOException {
        writeObject0(obj);
    }

    /** {@inheritDoc} */
    @Override public void defaultWriteObject() throws IOException {
        if (curObj == null)
            throw new NotActiveException("Not in writeObject() call.");

        writeFields(curObj, curFields);
    }

    /** {@inheritDoc} */
    @Override public ObjectOutputStream.PutField putFields() throws IOException {
        if (curObj == null)
            throw new NotActiveException("Not in writeObject() call.");

        if (curPut == null)
            curPut = new PutFieldImpl(this);

        return curPut;
    }

    /** {@inheritDoc} */
    @Override public void writeFields() throws IOException {
        if (curObj == null)
            throw new NotActiveException("Not in writeObject() call.");

        if (curPut == null)
            throw new NotActiveException("putFields() was not called.");

        for (GridBiTuple<GridOptimizedFieldType, Object> t : curPut.objs) {
            switch (t.get1()) {
                case BYTE:
                    writeByte((Byte)t.get2());

                    break;

                case SHORT:
                    writeShort((Short)t.get2());

                    break;

                case INT:
                    writeInt((Integer)t.get2());

                    break;

                case LONG:
                    writeLong((Long)t.get2());

                    break;

                case FLOAT:
                    writeFloat((Float)t.get2());

                    break;

                case DOUBLE:
                    writeDouble((Double)t.get2());

                    break;

                case CHAR:
                    writeChar((Character)t.get2());

                    break;

                case BOOLEAN:
                    writeBoolean((Boolean)t.get2());

                    break;

                case OTHER:
                    writeObject0(t.get2());
            }
        }
    }

    /** {@inheritDoc} */
    @Override public void reset() throws IOException {
        out.reset();
        handles.clear();

        curObj = null;
        curFields = null;
        curPut = null;
    }

    /** {@inheritDoc} */
    @Override public void flush() throws IOException {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public void drain() throws IOException {
        // No-op.
    }

    /**
     * Returns objects that were added to handles table.
     * Used ONLY for test purposes.
     *
     * @return Handled objects.
     */
    Object[] handledObjects() {
        return handles.objs;
    }

    /**
     * Lightweight identity hash table which maps objects to integer handles,
     * assigned in ascending order.
     */
    private static class HandleTable {
        /** */
        private static final Unsafe UNSAFE = GridUnsafe.unsafe();

        /** */
        private static final long intArrOff = UNSAFE.arrayBaseOffset(int[].class);

        /** Number of mappings in table/next available handle. */
        private int size;

        /** Size threshold determining when to expand hash spine. */
        private int threshold;

        /** Factor for computing size threshold. */
        private final float loadFactor;

        /** Maps hash value -> candidate handle value. */
        private int[] spine;

        /** Maps handle value -> next candidate handle value. */
        private int[] next;

        /** Maps handle value -> associated object. */
        private Object[] objs;

        /** */
        private int[] spineEmpty;

        /** */
        private int[] nextEmpty;

        /**
         * Creates new HandleTable with given capacity and load factor.
         *
         * @param initCap Initial capacity.
         * @param loadFactor Load factor.
         */
        HandleTable(int initCap, float loadFactor) {
            this.loadFactor = loadFactor;

            spine = new int[initCap];
            next = new int[initCap];
            objs = new Object[initCap];
            spineEmpty = new int[initCap];
            nextEmpty = new int[initCap];

            Arrays.fill(spineEmpty, -1);
            Arrays.fill(nextEmpty, -1);

            threshold = (int)(initCap * loadFactor);

            clear();
        }

        /**
         * Looks up and returns handle associated with given object, or -1 if
         * no mapping found.
         *
         * @param obj Object.
         * @return Handle.
         */
        int lookup(Object obj) {
            int idx = hash(obj) % spine.length;

            if (size > 0) {
                for (int i = spine[idx]; i >= 0; i = next[i])
                    if (objs[i] == obj)
                        return i;
            }

            if (size >= next.length)
                growEntries();

            if (size >= threshold)
                growSpine();

            insert(obj, size, idx);

            size++;

            return -1;
        }

        /**
         * Resets table to its initial (empty) state.
         */
        void clear() {
            UNSAFE.copyMemory(spineEmpty, intArrOff, spine, intArrOff, spineEmpty.length << 2);
            UNSAFE.copyMemory(nextEmpty, intArrOff, next, intArrOff, nextEmpty.length << 2);

            Arrays.fill(objs, null);

            size = 0;
        }

        /**
         * Inserts mapping object -> handle mapping into table. Assumes table
         * is large enough to accommodate new mapping.
         *
         * @param obj Object.
         * @param handle Handle.
         * @param idx Index.
         */
        private void insert(Object obj, int handle, int idx) {
            objs[handle] = obj;
            next[handle] = spine[idx];
            spine[idx] = handle;
        }

        /**
         * Expands the hash "spine" - equivalent to increasing the number of
         * buckets in a conventional hash table.
         */
        private void growSpine() {
            int size = (spine.length << 1) + 1;

            spine = new int[size];
            spineEmpty = new int[size];
            threshold = (int)(spine.length * loadFactor);

            Arrays.fill(spineEmpty, -1);

            UNSAFE.copyMemory(spineEmpty, intArrOff, spine, intArrOff, spineEmpty.length << 2);

            for (int i = 0; i < this.size; i++) {
                Object obj = objs[i];

                int idx = hash(obj) % spine.length;

                insert(objs[i], i, idx);
            }
        }

        /**
         * Increases hash table capacity by lengthening entry arrays.
         */
        private void growEntries() {
            int newLen = (next.length << 1) + 1;
            int[] newNext = new int[newLen];

            UNSAFE.copyMemory(next, intArrOff, newNext, intArrOff, size << 2);

            next = newNext;
            nextEmpty = new int[newLen];

            Arrays.fill(nextEmpty, -1);

            Object[] newObjs = new Object[newLen];

            System.arraycopy(objs, 0, newObjs, 0, size);

            objs = newObjs;
        }

        /**
         * Returns hash value for given object.
         *
         * @param obj Object.
         * @return Hash value.
         */
        private int hash(Object obj) {
            return System.identityHashCode(obj) & 0x7FFFFFFF;
        }
    }

    /**
     * {@link PutField} implementation.
     */
    private static class PutFieldImpl extends PutField {
        /** Stream. */
        private final GridOptimizedObjectOutputStream out;

        /** Class descriptor. */
        private final GridOptimizedClassDescriptor desc;

        /** Values. */
        private final GridBiTuple<GridOptimizedFieldType, Object>[] objs;

        /**
         * @param out Output stream.
         * @throws IOException In case of error.
         */
        @SuppressWarnings("unchecked")
        private PutFieldImpl(GridOptimizedObjectOutputStream out) throws IOException {
            this.out = out;

            desc = classDescriptor(out.curObj.getClass(), out.curObj);

            objs = new GridBiTuple[desc.fieldsCount()];
        }

        /** {@inheritDoc} */
        @Override public void put(String name, boolean val) {
            value(name, val);
        }

        /** {@inheritDoc} */
        @Override public void put(String name, byte val) {
            value(name, val);
        }

        /** {@inheritDoc} */
        @Override public void put(String name, char val) {
            value(name, val);
        }

        /** {@inheritDoc} */
        @Override public void put(String name, short val) {
            value(name, val);
        }

        /** {@inheritDoc} */
        @Override public void put(String name, int val) {
            value(name, val);
        }

        /** {@inheritDoc} */
        @Override public void put(String name, long val) {
            value(name, val);
        }

        /** {@inheritDoc} */
        @Override public void put(String name, float val) {
            value(name, val);
        }

        /** {@inheritDoc} */
        @Override public void put(String name, double val) {
            value(name, val);
        }

        /** {@inheritDoc} */
        @Override public void put(String name, Object val) {
            value(name, val);
        }

        /** {@inheritDoc} */
        @Override public void write(ObjectOutput out) throws IOException {
            if (out != this.out)
                throw new IllegalArgumentException("Wrong stream.");

            this.out.writeFields();
        }

        /**
         * @param name Field name.
         * @param val Value.
         */
        private void value(String name, Object val) {
            GridBiTuple<Integer, GridOptimizedFieldType> info = desc.fieldInfo(name);

            objs[info.get1()] = F.t(info.get2(), val);
        }
    }
}
