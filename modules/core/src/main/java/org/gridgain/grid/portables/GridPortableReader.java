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

package org.gridgain.grid.portables;

import org.jetbrains.annotations.*;

import java.sql.*;
import java.util.*;
import java.util.Date;

/**
 * Reader for portable objects used in {@link GridPortableMarshalAware} implementations.
 * Useful for the cases when user wants a fine-grained control over serialization.
 * <p>
 * Note that GridGain never writes full strings for field or type names. Instead,
 * for performance reasons, GridGain writes integer hash codes for type and field names.
 * It has been tested that hash code conflicts for the type names or the field names
 * within the same type are virtually non-existent and, to gain performance, it is safe
 * to work with hash codes. For the cases when hash codes for different types or fields
 * actually do collide, GridGain provides {@link GridPortableIdMapper} which
 * allows to override the automatically generated hash code IDs for the type and field names.
 */
public interface GridPortableReader {
    /**
     * @param fieldName Field name.
     * @return Byte value.
     * @throws GridPortableException In case of error.
     */
    public byte readByte(String fieldName) throws GridPortableException;

    /**
     * @param fieldName Field name.
     * @return Short value.
     * @throws GridPortableException In case of error.
     */
    public short readShort(String fieldName) throws GridPortableException;

    /**
     * @param fieldName Field name.
     * @return Integer value.
     * @throws GridPortableException In case of error.
     */
    public int readInt(String fieldName) throws GridPortableException;

    /**
     * @param fieldName Field name.
     * @return Long value.
     * @throws GridPortableException In case of error.
     */
    public long readLong(String fieldName) throws GridPortableException;

    /**
     * @param fieldName Field name.
     * @throws GridPortableException In case of error.
     * @return Float value.
     */
    public float readFloat(String fieldName) throws GridPortableException;

    /**
     * @param fieldName Field name.
     * @return Double value.
     * @throws GridPortableException In case of error.
     */
    public double readDouble(String fieldName) throws GridPortableException;

    /**
     * @param fieldName Field name.
     * @return Char value.
     * @throws GridPortableException In case of error.
     */
    public char readChar(String fieldName) throws GridPortableException;

    /**
     * @param fieldName Field name.
     * @return Boolean value.
     * @throws GridPortableException In case of error.
     */
    public boolean readBoolean(String fieldName) throws GridPortableException;

    /**
     * @param fieldName Field name.
     * @return String value.
     * @throws GridPortableException In case of error.
     */
    @Nullable public String readString(String fieldName) throws GridPortableException;

    /**
     * @param fieldName Field name.
     * @return UUID.
     * @throws GridPortableException In case of error.
     */
    @Nullable public UUID readUuid(String fieldName) throws GridPortableException;

    /**
     * @param fieldName Field name.
     * @return Date.
     * @throws GridPortableException In case of error.
     */
    @Nullable public Date readDate(String fieldName) throws GridPortableException;

    /**
     * @param fieldName Field name.
     * @return Timestamp.
     * @throws GridPortableException In case of error.
     */
    @Nullable public Timestamp readTimestamp(String fieldName) throws GridPortableException;

    /**
     * @param fieldName Field name.
     * @return Object.
     * @throws GridPortableException In case of error.
     */
    @Nullable public <T> T readObject(String fieldName) throws GridPortableException;

    /**
     * @param fieldName Field name.
     * @return Byte array.
     * @throws GridPortableException In case of error.
     */
    @Nullable public byte[] readByteArray(String fieldName) throws GridPortableException;

    /**
     * @param fieldName Field name.
     * @return Short array.
     * @throws GridPortableException In case of error.
     */
    @Nullable public short[] readShortArray(String fieldName) throws GridPortableException;

    /**
     * @param fieldName Field name.
     * @return Integer array.
     * @throws GridPortableException In case of error.
     */
    @Nullable public int[] readIntArray(String fieldName) throws GridPortableException;

    /**
     * @param fieldName Field name.
     * @return Long array.
     * @throws GridPortableException In case of error.
     */
    @Nullable public long[] readLongArray(String fieldName) throws GridPortableException;

    /**
     * @param fieldName Field name.
     * @return Float array.
     * @throws GridPortableException In case of error.
     */
    @Nullable public float[] readFloatArray(String fieldName) throws GridPortableException;

    /**
     * @param fieldName Field name.
     * @return Byte array.
     * @throws GridPortableException In case of error.
     */
    @Nullable public double[] readDoubleArray(String fieldName) throws GridPortableException;

    /**
     * @param fieldName Field name.
     * @return Char array.
     * @throws GridPortableException In case of error.
     */
    @Nullable public char[] readCharArray(String fieldName) throws GridPortableException;

    /**
     * @param fieldName Field name.
     * @return Boolean array.
     * @throws GridPortableException In case of error.
     */
    @Nullable public boolean[] readBooleanArray(String fieldName) throws GridPortableException;

    /**
     * @param fieldName Field name.
     * @return String array.
     * @throws GridPortableException In case of error.
     */
    @Nullable public String[] readStringArray(String fieldName) throws GridPortableException;

    /**
     * @param fieldName Field name.
     * @return UUID array.
     * @throws GridPortableException In case of error.
     */
    @Nullable public UUID[] readUuidArray(String fieldName) throws GridPortableException;

    /**
     * @param fieldName Field name.
     * @return Date array.
     * @throws GridPortableException In case of error.
     */
    @Nullable public Date[] readDateArray(String fieldName) throws GridPortableException;

    /**
     * @param fieldName Field name.
     * @return Object array.
     * @throws GridPortableException In case of error.
     */
    @Nullable public Object[] readObjectArray(String fieldName) throws GridPortableException;

    /**
     * @param fieldName Field name.
     * @return Collection.
     * @throws GridPortableException In case of error.
     */
    @Nullable public <T> Collection<T> readCollection(String fieldName) throws GridPortableException;

    /**
     * @param fieldName Field name.
     * @param colCls Collection class.
     * @return Collection.
     * @throws GridPortableException In case of error.
     */
    @Nullable public <T> Collection<T> readCollection(String fieldName, Class<? extends Collection<T>> colCls)
        throws GridPortableException;

    /**
     * @param fieldName Field name.
     * @return Map.
     * @throws GridPortableException In case of error.
     */
    @Nullable public <K, V> Map<K, V> readMap(String fieldName) throws GridPortableException;

    /**
     * @param fieldName Field name.
     * @param mapCls Map class.
     * @return Map.
     * @throws GridPortableException In case of error.
     */
    @Nullable public <K, V> Map<K, V> readMap(String fieldName, Class<? extends Map<K, V>> mapCls)
        throws GridPortableException;

    /**
     * @param fieldName Field name.
     * @param enumCls Enum class.
     * @return Value.
     * @throws GridPortableException In case of error.
     */
    @Nullable public <T extends Enum<?>> T readEnum(String fieldName, Class<T> enumCls) throws GridPortableException;

    /**
     * @param fieldName Field name.
     * @param enumCls Enum class.
     * @return Value.
     * @throws GridPortableException In case of error.
     */
    @Nullable public <T extends Enum<?>> T[] readEnumArray(String fieldName, Class<T> enumCls)
        throws GridPortableException;

    /**
     * Gets raw reader. Raw reader does not use field name hash codes, therefore,
     * making the format even more compact. However, if the raw reader is used,
     * dynamic structure changes to the portable objects are not supported.
     *
     * @return Raw reader.
     */
    public GridPortableRawReader rawReader();
}
