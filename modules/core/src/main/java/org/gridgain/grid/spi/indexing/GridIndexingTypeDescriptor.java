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

package org.gridgain.grid.spi.indexing;


import org.gridgain.grid.spi.*;

import java.util.*;

/**
 * Value descriptor which allows to extract fields from value object of given type.
 * See also {@link GridIndexingSpi#registerType(String, GridIndexingTypeDescriptor)}.
 */
public interface GridIndexingTypeDescriptor {
    /**
     * Gets type name which uniquely identifies this type.
     *
     * @return Type name which uniquely identifies this type.
     */
    public String name();

    /**
     * Gets mapping from values field name to its type.
     *
     * @return Fields that can be indexed, participate in queries and can be queried using
     *      {@link GridIndexingSpi#queryFields(String, String, Collection, GridIndexingQueryFilter[])}
     *      method.
     */
    public Map<String, Class<?>> valueFields();

    /**
     * Gets mapping from keys field name to its type.
     *
     * @return Fields that can be indexed, participate in queries and can be queried using
     *      {@link GridIndexingSpi#queryFields(String, String, Collection, GridIndexingQueryFilter[])}
     *      method.
     */
    public Map<String, Class<?>> keyFields();

    /**
     * Gets field value for given object.
     *
     * @param obj Object to get field value from.
     * @param field Field name.
     * @return Value for given field.
     * @throws GridSpiException If failed.
     */
    public <T> T value(Object obj, String field) throws GridSpiException;

    /**
     * Gets indexes for this type.
     *
     * @return Indexes for this type.
     */
    public Map<String, GridIndexDescriptor> indexes();

    /**
     * Gets value class.
     *
     * @return Value class.
     */
    public Class<?> valueClass();

    /**
     * Gets key class.
     *
     * @return Key class.
     */
    public Class<?> keyClass();

    /**
     * Returns {@code true} if string representation of value should be indexed as text.
     *
     * @return If string representation of value should be full-text indexed.
     */
    public boolean valueTextIndex();
}
