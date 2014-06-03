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
 * Represents a purchase record. In our {@code snowflake} schema purchase
 * is a {@code 'fact'} and will be cached in larger {@link GridCacheMode#PARTITIONED}
 * cache.
 */
public class FactPurchase {
    /** Primary key. */
    @GridCacheQuerySqlField(index = true)
    private int id;

    /** Foreign key to store at which purchase occurred. */
    @GridCacheQuerySqlField
    private int storeId;

    /** Foreign key to purchased product. */
    @GridCacheQuerySqlField
    private int productId;

    /** Purchase price. */
    @GridCacheQuerySqlField
    private float purchasePrice;

    /**
     * Constructs a purchase record.
     *
     * @param id Purchase ID.
     * @param productId Purchased product ID.
     * @param storeId Store ID.
     * @param purchasePrice Purchase price.
     */
    public FactPurchase(int id, int productId, int storeId, float purchasePrice) {
        this.id = id;
        this.productId = productId;
        this.storeId = storeId;
        this.purchasePrice = purchasePrice;
    }

    /**
     * Gets purchase ID.
     *
     * @return Purchase ID.
     */
    public int getId() {
        return id;
    }

    /**
     * Gets purchased product ID.
     *
     * @return Product ID.
     */
    public int getProductId() {
        return productId;
    }

    /**
     * Gets ID of store at which purchase was made.
     *
     * @return Store ID.
     */
    public int getStoreId() {
        return storeId;
    }

    /**
     * Gets purchase price.
     *
     * @return Purchase price.
     */
    public float getPurchasePrice() {
        return purchasePrice;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return "FactPurchase [id=" + id +
            ", productId=" + productId +
            ", storeId=" + storeId +
            ", purchasePrice=" + purchasePrice + ']';
    }
}
