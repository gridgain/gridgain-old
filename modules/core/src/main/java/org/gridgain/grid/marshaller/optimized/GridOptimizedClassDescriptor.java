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
import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.typedef.*;
import sun.misc.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import static java.lang.reflect.Modifier.*;
import static org.gridgain.grid.marshaller.optimized.GridOptimizedClassResolver.*;
import static org.gridgain.grid.marshaller.optimized.GridOptimizedFieldType.*;
import static org.gridgain.grid.marshaller.optimized.GridOptimizedMarshallerUtils.*;

/**
 * Class descriptor.
 */
class GridOptimizedClassDescriptor {
    /** Unsafe. */
    private static final Unsafe UNSAFE = GridUnsafe.unsafe();

    /** */
    private static final int TYPE_BYTE = 1;

    /** */
    private static final int TYPE_SHORT = 2;

    /** */
    private static final int TYPE_INT = 3;

    /** */
    private static final int TYPE_LONG = 4;

    /** */
    private static final int TYPE_FLOAT = 5;

    /** */
    private static final int TYPE_DOUBLE = 6;

    /** */
    private static final int TYPE_CHAR = 7;

    /** */
    private static final int TYPE_BOOLEAN = 8;

    /** */
    private static final int TYPE_BYTE_ARR = 9;

    /** */
    private static final int TYPE_SHORT_ARR = 10;

    /** */
    private static final int TYPE_INT_ARR = 11;

    /** */
    private static final int TYPE_LONG_ARR = 12;

    /** */
    private static final int TYPE_FLOAT_ARR = 13;

    /** */
    private static final int TYPE_DOUBLE_ARR = 14;

    /** */
    private static final int TYPE_CHAR_ARR = 15;

    /** */
    private static final int TYPE_BOOLEAN_ARR = 16;

    /** */
    private static final int TYPE_OBJ_ARR = 17;

    /** */
    private static final int TYPE_STR = 18;

    /** */
    private static final int TYPE_ENUM = 19;

    /** */
    private static final int TYPE_UUID = 20;

    /** */
    private static final int TYPE_PROPS = 21;

    /** */
    private static final int TYPE_ARRAY_LIST = 22;

    /** */
    private static final int TYPE_HASH_MAP = 23;

    /** */
    private static final int TYPE_HASH_SET = 24;

    /** */
    private static final int TYPE_LINKED_LIST = 25;

    /** */
    private static final int TYPE_LINKED_HASH_MAP = 26;

    /** */
    private static final int TYPE_LINKED_HASH_SET = 27;

    /** */
    private static final int TYPE_DATE = 28;

    /** */
    private static final int TYPE_CLS = 29;

    /** */
    private static final int TYPE_EXTERNALIZABLE = 50;

    /** */
    private static final int TYPE_SERIALIZABLE = 51;

    /** Class. */
    private Class<?> cls;

    /** Header. */
    private Integer hdr;

    /** ID. */
    private Integer id;

    /** Short ID. */
    private Short shortId;

    /** Class name. */
    private String name;

    /** Class type. */
    private int type;

    /** Primitive flag. */
    private boolean isPrimitive;

    /** Enum flag. */
    private boolean isEnum;

    /** Serializable flag. */
    private boolean isSerial;

    /** Excluded flag. */
    private final boolean excluded;

    /** {@code True} if descriptor is for {@link Class}. */
    private boolean isCls;

    /** Array component type. */
    private Class<?> arrCompType;

    /** Enumeration values. */
    private Object[] enumVals;

    /** Constructor. */
    private Constructor<?> constructor;

    /** Fields. */
    private List<List<Field>> fields;

    /** Fields. */
    private List<List<T2<GridOptimizedFieldType, Long>>> fieldOffs;

    /** {@code writeObject} methods. */
    private List<Method> writeObjMtds;

    /** {@code writeReplace} method. */
    private Method writeReplaceMtd;

    /** {@code readObject} methods. */
    private List<Method> readObjMtds;

    /** {@code readResolve} method. */
    private Method readResolveMtd;

    /** Field info map. */
    private Map<String, GridBiTuple<Integer, GridOptimizedFieldType>> fieldInfoMap;

    /** Field info list. */
    private List<GridBiTuple<Integer, GridOptimizedFieldType>> fieldInfoList;

    /** Defaults field offset. */
    private long dfltsFieldOff;

    /** Load factor field offset. */
    private long loadFactorFieldOff;

    /** Map field offset. */
    private long mapFieldOff;

    /** Access order field offset. */
    private long accessOrderFieldOff;

    /**
     * Creates descriptor for class.
     *
     * @param cls Class.
     * @throws IOException In case of error.
     */
    @SuppressWarnings({"ForLoopReplaceableByForEach", "MapReplaceableByEnumMap"})
    GridOptimizedClassDescriptor(Class<?> cls) throws IOException {
        this.cls = cls;

        excluded = GridMarshallerExclusions.isExcluded(cls);

        T2<Integer, Integer> t = GridOptimizedClassResolver.writeClassData(cls);

        hdr = t.get1();
        id = t.get2();
        name = cls.getName();

        if (!excluded) {
            Class<?> parent;

            if (cls == byte.class || cls == Byte.class) {
                type = TYPE_BYTE;

                isPrimitive = true;
            }
            else if (cls == short.class || cls == Short.class) {
                type = TYPE_SHORT;

                isPrimitive = true;
            }
            else if (cls == int.class || cls == Integer.class) {
                type = TYPE_INT;

                isPrimitive = true;
            }
            else if (cls == long.class || cls == Long.class) {
                type = TYPE_LONG;

                isPrimitive = true;
            }
            else if (cls == float.class || cls == Float.class) {
                type = TYPE_FLOAT;

                isPrimitive = true;
            }
            else if (cls == double.class || cls == Double.class) {
                type = TYPE_DOUBLE;

                isPrimitive = true;
            }
            else if (cls == char.class || cls == Character.class) {
                type = TYPE_CHAR;

                isPrimitive = true;
            }
            else if (cls == boolean.class || cls == Boolean.class) {
                type = TYPE_BOOLEAN;

                isPrimitive = true;
            }
            else if (cls == byte[].class)
                type = TYPE_BYTE_ARR;
            else if (cls == short[].class)
                type = TYPE_SHORT_ARR;
            else if (cls == int[].class)
                type = TYPE_INT_ARR;
            else if (cls == long[].class)
                type = TYPE_LONG_ARR;
            else if (cls == float[].class)
                type = TYPE_FLOAT_ARR;
            else if (cls == double[].class)
                type = TYPE_DOUBLE_ARR;
            else if (cls == char[].class)
                type = TYPE_CHAR_ARR;
            else if (cls == boolean[].class)
                type = TYPE_BOOLEAN_ARR;
            else if (cls.isArray()) {
                type = TYPE_OBJ_ARR;

                arrCompType = cls.getComponentType();
            }
            else if (cls == String.class)
                type = TYPE_STR;
            else if (cls.isEnum()) {
                type = TYPE_ENUM;

                isEnum = true;
                enumVals = cls.getEnumConstants();
            }
            // Support for enum constants, based on anonymous children classes.
            else if ((parent = cls.getSuperclass()) != null && parent.isEnum()) {
                type = TYPE_ENUM;

                isEnum = true;
                enumVals = parent.getEnumConstants();
            }
            else if (cls == UUID.class)
                type = TYPE_UUID;
            else if (cls == Properties.class) {
                type = TYPE_PROPS;

                try {
                    dfltsFieldOff = UNSAFE.objectFieldOffset(Properties.class.getDeclaredField("defaults"));
                }
                catch (NoSuchFieldException e) {
                    throw new IOException(e);
                }
            }
            else if (cls == ArrayList.class)
                type = TYPE_ARRAY_LIST;
            else if (cls == HashMap.class) {
                type = TYPE_HASH_MAP;

                try {
                    loadFactorFieldOff = UNSAFE.objectFieldOffset(HashMap.class.getDeclaredField("loadFactor"));
                }
                catch (NoSuchFieldException e) {
                    throw new IOException(e);
                }
            }
            else if (cls == HashSet.class) {
                type = TYPE_HASH_SET;

                try {
                    loadFactorFieldOff = UNSAFE.objectFieldOffset(HashMap.class.getDeclaredField("loadFactor"));
                    mapFieldOff = UNSAFE.objectFieldOffset(HashSet.class.getDeclaredField("map"));
                }
                catch (NoSuchFieldException e) {
                    throw new IOException(e);
                }
            }
            else if (cls == LinkedList.class)
                type = TYPE_LINKED_LIST;
            else if (cls == LinkedHashMap.class) {
                type = TYPE_LINKED_HASH_MAP;

                try {
                    loadFactorFieldOff = UNSAFE.objectFieldOffset(HashMap.class.getDeclaredField("loadFactor"));
                    accessOrderFieldOff = UNSAFE.objectFieldOffset(LinkedHashMap.class.getDeclaredField("accessOrder"));
                }
                catch (NoSuchFieldException e) {
                    throw new IOException(e);
                }
            }
            else if (cls == LinkedHashSet.class) {
                type = TYPE_LINKED_HASH_SET;

                try {
                    loadFactorFieldOff = UNSAFE.objectFieldOffset(HashMap.class.getDeclaredField("loadFactor"));
                    mapFieldOff = UNSAFE.objectFieldOffset(HashSet.class.getDeclaredField("map"));
                }
                catch (NoSuchFieldException e) {
                    throw new IOException(e);
                }
            }
            else if (cls == Date.class)
                type = TYPE_DATE;
            else if (cls == Class.class) {
                type = TYPE_CLS;

                isCls = true;
            }
            else {
                Class<?> c = cls;

                while ((writeReplaceMtd == null || readResolveMtd == null) && c != null && !c.equals(Object.class)) {
                    if (writeReplaceMtd == null) {
                        try {
                            writeReplaceMtd = c.getDeclaredMethod("writeReplace");

                            if (!isStatic(writeReplaceMtd.getModifiers()) &&
                                writeReplaceMtd.getReturnType().equals(Object.class))
                                writeReplaceMtd.setAccessible(true);
                            else
                                // Set method back to null if it has incorrect signature.
                                writeReplaceMtd = null;
                        }
                        catch (NoSuchMethodException ignored) {
                            // No-op.
                        }
                    }

                    if (readResolveMtd == null) {
                        try {
                            readResolveMtd = c.getDeclaredMethod("readResolve");

                            if (!isStatic(readResolveMtd.getModifiers()) &&
                                readResolveMtd.getReturnType().equals(Object.class))
                                readResolveMtd.setAccessible(true);
                            else
                                // Set method back to null if it has incorrect signature.
                                readResolveMtd = null;
                        }
                        catch (NoSuchMethodException ignored) {
                            // No-op.
                        }
                    }

                    c = c.getSuperclass();
                }

                if (Externalizable.class.isAssignableFrom(cls)) {
                    type = TYPE_EXTERNALIZABLE;

                    try {
                        constructor = cls.getDeclaredConstructor();

                        constructor.setAccessible(true);
                    }
                    catch (NoSuchMethodException e) {
                        throw new IOException("Externalizable class doesn't have default constructor: " + cls, e);
                    }
                }
                else {
                    type = TYPE_SERIALIZABLE;

                    isSerial = Serializable.class.isAssignableFrom(cls);

                    writeObjMtds = new ArrayList<>();
                    readObjMtds = new ArrayList<>();
                    fields = new ArrayList<>();
                    fieldOffs = new ArrayList<>();

                    for (c = cls; c != null && !c.equals(Object.class); c = c.getSuperclass()) {
                        Method mtd;

                        try {
                            mtd = c.getDeclaredMethod("writeObject", ObjectOutputStream.class);

                            int mod = mtd.getModifiers();

                            if (!isStatic(mod) && isPrivate(mod) && mtd.getReturnType() == Void.TYPE)
                                mtd.setAccessible(true);
                            else
                                // Set method back to null if it has incorrect signature.
                                mtd = null;
                        }
                        catch (NoSuchMethodException ignored) {
                            mtd = null;
                        }

                        writeObjMtds.add(mtd);

                        try {
                            mtd = c.getDeclaredMethod("readObject", ObjectInputStream.class);

                            int mod = mtd.getModifiers();

                            if (!isStatic(mod) && isPrivate(mod) && mtd.getReturnType() == Void.TYPE)
                                mtd.setAccessible(true);
                            else
                                // Set method back to null if it has incorrect signature.
                                mtd = null;
                        }
                        catch (NoSuchMethodException ignored) {
                            mtd = null;
                        }

                        readObjMtds.add(mtd);

                        Field[] clsFields0 = c.getDeclaredFields();

                        Arrays.sort(clsFields0, new Comparator<Field>() {
                            @Override public int compare(Field f1, Field f2) {
                                return f1.getName().compareTo(f2.getName());
                            }
                        });

                        List<Field> clsFields = new ArrayList<>(clsFields0.length);
                        List<T2<GridOptimizedFieldType, Long>> clsFieldOffs =
                            new ArrayList<>(clsFields0.length);

                        for (int i = 0; i < clsFields0.length; i++) {
                            Field f = clsFields0[i];

                            int mod = f.getModifiers();

                            if (!isStatic(mod) && !isTransient(mod)) {
                                GridOptimizedFieldType type = fieldType(f.getType());

                                clsFields.add(f);
                                clsFieldOffs.add(new T2<>(type, UNSAFE.objectFieldOffset(f)));
                            }
                        }

                        fields.add(clsFields);
                        fieldOffs.add(clsFieldOffs);
                    }

                    Collections.reverse(writeObjMtds);
                    Collections.reverse(readObjMtds);
                    Collections.reverse(fields);
                    Collections.reverse(fieldOffs);

                    try {
                        Field serFieldsDesc = cls.getDeclaredField("serialPersistentFields");

                        int mod = serFieldsDesc.getModifiers();

                        if (serFieldsDesc.getType() == ObjectStreamField[].class &&
                            isPrivate(mod) && isStatic(mod) && isFinal(mod)) {
                            serFieldsDesc.setAccessible(true);

                            ObjectStreamField[] serFields = (ObjectStreamField[])serFieldsDesc.get(null);

                            fieldInfoMap = new HashMap<>();

                            for (int i = 0; i < serFields.length; i++) {
                                ObjectStreamField serField = serFields[i];

                                fieldInfoMap.put(serField.getName(), F.t(i, fieldType(serField.getType())));
                            }
                        }
                    }
                    catch (NoSuchFieldException ignored) {
                        // No-op.
                    }
                    catch (IllegalAccessException e) {
                        throw new IOException("Failed to get value of 'serialPersistentFields' field in class: " +
                            cls.getName(), e);
                    }

                    if (fieldInfoMap == null) {
                        fieldInfoMap = new HashMap<>();

                        if (!fields.isEmpty()) {
                            List<Field> ownFields = fields.get(fields.size() - 1);

                            for (int i = 0; i < ownFields.size(); i++) {
                                Field f = ownFields.get(i);

                                fieldInfoMap.put(f.getName(), F.t(i, fieldType(f.getType())));
                            }
                        }
                    }

                    fieldInfoList = new ArrayList<>(fieldInfoMap.values());

                    Collections.sort(fieldInfoList, new Comparator<GridBiTuple<Integer, GridOptimizedFieldType>>() {
                        @Override public int compare(GridBiTuple<Integer, GridOptimizedFieldType> t1,
                            GridBiTuple<Integer, GridOptimizedFieldType> t2) {
                            return t1.get1().compareTo(t2.get1());
                        }
                    });
                }
            }
        }

        shortId = computeSerialVersionUid(cls, !F.isEmpty(fields) ? fields.get(fields.size() - 1) : null).shortValue();
    }

    /**
     * @return Excluded flag.
     */
    boolean excluded() {
        return excluded;
    }

    /**
     * @return Class.
     */
    Class<?> describedClass() {
        return cls;
    }

    /**
     * @return Header.
     */
    Integer header() {
        return hdr;
    }

    /**
     * @return ID.
     */
    Integer id() {
        return id;
    }

    /**
     * @return Short ID.
     */
    Short shortId() {
        return shortId;
    }

    /**
     * @return Class name.
     */
    String name() {
        return name;
    }

    /**
     * @return Array component type.
     */
    Class<?> componentType() {
        return arrCompType;
    }

    /**
     * @return Primitive flag.
     */
    boolean isPrimitive() {
        return isPrimitive;
    }

    /**
     * @return Enum flag.
     */
    boolean isEnum() {
        return isEnum;
    }

    /**
     * @return {@code True} if descriptor is for {@link Class}.
     */
    boolean isClass() {
        return isCls;
    }

    /**
     * @return Number of fields.
     */
    int fieldsCount() {
        return fieldInfoMap.size();
    }

    /**
     * @param name Field name.
     * @return Field info.
     */
    GridBiTuple<Integer, GridOptimizedFieldType> fieldInfo(String name) {
        return fieldInfoMap.get(name);
    }

    /**
     * @return Field infos.
     */
    List<GridBiTuple<Integer, GridOptimizedFieldType>> fieldInfos() {
        return fieldInfoList;
    }

    /**
     * Replaces object.
     *
     * @param obj Object.
     * @return Replaced object or {@code null} if there is no {@code writeReplace} method.
     * @throws IOException In case of error.
     */
    Object replace(Object obj) throws IOException {
        if (writeReplaceMtd != null) {
            try {
                return writeReplaceMtd.invoke(obj);
            }
            catch (IllegalAccessException | InvocationTargetException e) {
                throw new IOException(e);
            }
        }

        return obj;
    }

    /**
     * Writes object to stream.
     *
     * @param out Output stream.
     * @param obj Object.
     * @throws IOException In case of error.
     */
    @SuppressWarnings("ForLoopReplaceableByForEach")
    void write(GridOptimizedObjectOutputStream out, Object obj) throws IOException {
        switch (type) {
            case TYPE_BYTE:
                out.writeByte((Byte)obj);

                break;

            case TYPE_SHORT:
                out.writeShort((Short)obj);

                break;

            case TYPE_INT:
                out.writeInt((Integer)obj);

                break;

            case TYPE_LONG:
                out.writeLong((Long)obj);

                break;

            case TYPE_FLOAT:
                out.writeFloat((Float)obj);

                break;

            case TYPE_DOUBLE:
                out.writeDouble((Double)obj);

                break;

            case TYPE_CHAR:
                out.writeChar((Character)obj);

                break;

            case TYPE_BOOLEAN:
                out.writeBoolean((Boolean)obj);

                break;

            case TYPE_BYTE_ARR:
                out.writeByteArray((byte[])obj);

                break;

            case TYPE_SHORT_ARR:
                out.writeShortArray((short[])obj);

                break;

            case TYPE_INT_ARR:
                out.writeIntArray((int[])obj);

                break;

            case TYPE_LONG_ARR:
                out.writeLongArray((long[])obj);

                break;

            case TYPE_FLOAT_ARR:
                out.writeFloatArray((float[])obj);

                break;

            case TYPE_DOUBLE_ARR:
                out.writeDoubleArray((double[])obj);

                break;

            case TYPE_CHAR_ARR:
                out.writeCharArray((char[])obj);

                break;

            case TYPE_BOOLEAN_ARR:
                out.writeBooleanArray((boolean[])obj);

                break;

            case TYPE_OBJ_ARR:
                out.writeArray((Object[])obj);

                break;

            case TYPE_STR:
                out.writeString((String)obj);

                break;

            case TYPE_ENUM:
                out.writeInt(((Enum)obj).ordinal());

                break;

            case TYPE_UUID:
                out.writeUuid((UUID)obj);

                break;

            case TYPE_PROPS:
                out.writeProperties((Properties)obj, dfltsFieldOff);

                break;

            case TYPE_ARRAY_LIST:
                out.writeArrayList((ArrayList<?>)obj);

                break;

            case TYPE_HASH_MAP:
                out.writeHashMap((HashMap<?, ?>)obj, loadFactorFieldOff, false);

                break;

            case TYPE_HASH_SET:
                out.writeHashSet((HashSet<?>)obj, mapFieldOff, loadFactorFieldOff);

                break;

            case TYPE_LINKED_LIST:
                out.writeLinkedList((LinkedList<?>)obj);

                break;

            case TYPE_LINKED_HASH_MAP:
                out.writeLinkedHashMap((LinkedHashMap<?, ?>)obj, loadFactorFieldOff, accessOrderFieldOff, false);

                break;

            case TYPE_LINKED_HASH_SET:
                out.writeLinkedHashSet((LinkedHashSet<?>)obj, mapFieldOff, loadFactorFieldOff);

                break;

            case TYPE_DATE:
                out.writeDate((Date)obj);

                break;

            case TYPE_CLS:
                writeClass(out, classDescriptor((Class<?>)obj, obj));

                break;

            case TYPE_EXTERNALIZABLE:
                out.writeExternalizable(obj);

                break;

            case TYPE_SERIALIZABLE:
                if (out.requireSerializable() && !isSerial)
                    throw new NotSerializableException("Must implement java.io.Serializable or " +
                        "set GridOptimizedMarshaller.setRequireSerializable() to false " +
                        "(note that performance may degrade if object is not Serializable): " + name);

                out.writeSerializable(obj, fieldOffs, writeObjMtds);

                break;

            default:
                throw new IllegalStateException("Invalid class type: " + type);
        }
    }

    /**
     * Reads object from stream.
     *
     * @param in Input stream.
     * @return Object.
     * @throws ClassNotFoundException If class not found.
     * @throws IOException In case of error.
     */
    Object read(GridOptimizedObjectInputStream in) throws ClassNotFoundException, IOException {
        switch (type) {
            case TYPE_BYTE:
                return in.readByte();

            case TYPE_SHORT:
                return in.readShort();

            case TYPE_INT:
                return in.readInt();

            case TYPE_LONG:
                return in.readLong();

            case TYPE_FLOAT:
                return in.readFloat();

            case TYPE_DOUBLE:
                return in.readDouble();

            case TYPE_CHAR:
                return in.readChar();

            case TYPE_BOOLEAN:
                return in.readBoolean();

            case TYPE_BYTE_ARR:
                return in.readByteArray();

            case TYPE_SHORT_ARR:
                return in.readShortArray();

            case TYPE_INT_ARR:
                return in.readIntArray();

            case TYPE_LONG_ARR:
                return in.readLongArray();

            case TYPE_FLOAT_ARR:
                return in.readFloatArray();

            case TYPE_DOUBLE_ARR:
                return in.readDoubleArray();

            case TYPE_CHAR_ARR:
                return in.readCharArray();

            case TYPE_BOOLEAN_ARR:
                return in.readBooleanArray();

            case TYPE_OBJ_ARR:
                return in.readArray(arrCompType);

            case TYPE_STR:
                return in.readString();

            case TYPE_ENUM:
                return enumVals[in.readInt()];

            case TYPE_UUID:
                return in.readUuid();

            case TYPE_PROPS:
                return in.readProperties();

            case TYPE_ARRAY_LIST:
                return in.readArrayList();

            case TYPE_HASH_MAP:
                return in.readHashMap(false);

            case TYPE_HASH_SET:
                return in.readHashSet(mapFieldOff);

            case TYPE_LINKED_LIST:
                return in.readLinkedList();

            case TYPE_LINKED_HASH_MAP:
                return in.readLinkedHashMap(false);

            case TYPE_LINKED_HASH_SET:
                return in.readLinkedHashSet(mapFieldOff);

            case TYPE_DATE:
                return in.readDate();

            case TYPE_CLS:
                return readClass(in, in.classLoader()).describedClass();

            case TYPE_EXTERNALIZABLE:
                return in.readExternalizable(constructor, readResolveMtd);

            case TYPE_SERIALIZABLE:
                return in.readSerializable(cls, fieldOffs, readObjMtds, readResolveMtd);

            default:
                throw new IllegalStateException("Invalid class type: " + type);
        }
    }

    /**
     * @param cls Class.
     * @return Type.
     */
    @SuppressWarnings("IfMayBeConditional")
    private GridOptimizedFieldType fieldType(Class<?> cls) {
        GridOptimizedFieldType type;

        if (cls == byte.class)
            type = BYTE;
        else if (cls == short.class)
            type = SHORT;
        else if (cls == int.class)
            type = INT;
        else if (cls == long.class)
            type = LONG;
        else if (cls == float.class)
            type = FLOAT;
        else if (cls == double.class)
            type = DOUBLE;
        else if (cls == char.class)
            type = CHAR;
        else if (cls == boolean.class)
            type = BOOLEAN;
        else
            type = OTHER;

        return type;
    }
}
