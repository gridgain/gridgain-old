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

package org.gridgain.grid.cache.query;

import java.lang.annotation.*;

/**
 * Annotates fields for SQL queries. All fields that will be involved in SQL clauses must have
 * this annotation. For more information about cache queries see {@link GridCacheQuery} documentation.
 * @see GridCacheQuery
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface GridCacheQuerySqlField {
    /**
     * Specifies whether cache should maintain an index for this field or not.
     * Just like with databases, field indexing may require additional overhead
     * during updates, but makes select operations faster.
     * <p>
     * When {@code GridH2IndexingSpi} is set as indexing SPI and indexed field is
     * of type {@code com.vividsolutions.jts.geom.Geometry} (or any subclass of this class) then GridGain will
     * consider this index as spatial providing performance boost for spatial queries.
     *
     * @return {@code True} if index must be created for this field in database.
     */
    boolean index() default false;

    /**
     * Specifies whether index should be unique or not. This property only
     * makes sense if {@link #index()} property is set to {@code true}.
     *
     * @return {@code True} if field index should be unique.
     * @deprecated No longer supported, will be ignored.
     */
    @Deprecated
    boolean unique() default false;

    /**
     * Specifies whether index should be in descending order or not. This property only
     * makes sense if {@link #index()} property is set to {@code true}.
     *
     * @return {@code True} if field index should be in descending order.
     */
    boolean descending() default false;

    /**
     * Array of index groups this field belongs to. Groups are used for compound indexes,
     * whenever index should be created on more than one field. All fields within the same
     * group will belong to the same index.
     * <p>
     * Most of the times user will not specify any group, which means that cache should
     * simply create a single field index.
     *
     * @return Array of group names.
     */
    String[] groups() default {};

    /**
     * Array of ordered index groups this field belongs to.
     *
     * @return Array of ordered group indexes.
     */
    Group[] orderedGroups() default {};

    /**
     * Property name. If not provided then field name will be used.
     *
     * @return Name of property.
     */
    String name() default "";

    /**
     * Describes group of index and position of field in this group.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    @SuppressWarnings("PublicInnerClass")
    public static @interface Group {
        /**
         * Group index name where this field participate.
         *
         * @return Group index name
         */
        String name();

        /**
         * Fields in this group index will be sorted on this attribute.
         *
         * @return Order number.
         */
        int order();

        /**
         * Defines sorting order for this field in group.
         *
         * @return True if field will be in descending order.
         */
        boolean descending() default false;
    }
}
