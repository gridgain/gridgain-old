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

package org.gridgain.examples.datagrid.starschema;

import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.query.*;

/**
 * Represents a product available for purchase. In our {@code snowflake} schema a {@code product}
 * is a {@code 'dimension'} and will be cached in {@link GridCacheMode#REPLICATED}
 * cache.
 */
public class DimProduct {
    /** Primary key. */
    @GridCacheQuerySqlField(index = true)
    private int id;

    /** Product name. */
    private String name;

    /** Product list price. */
    @GridCacheQuerySqlField
    private float price;

    /** Available product quantity. */
    private int qty;

    /**
     * Constructs a product instance.
     *
     * @param id Product ID.
     * @param name Product name.
     * @param price Product list price.
     * @param qty Available product quantity.
     */
    public DimProduct(int id, String name, float price, int qty) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.qty = qty;
    }

    /**
     * Gets product ID.
     *
     * @return Product ID.
     */
    public int getId() {
        return id;
    }

    /**
     * Gets product name.
     *
     * @return Product name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets product list price.
     *
     * @return Product list price.
     */
    public float getPrice() {
        return price;
    }

    /**
     * Gets available product quantity.
     *
     * @return Available product quantity.
     */
    public int getQuantity() {
        return qty;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return "DimProduct [id=" + id +
            ", name=" + name +
            ", price=" + price +
            ", qty=" + qty + ']';
    }
}
