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

package org.gridgain.grid.kernal.processors.cache;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.query.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.marshaller.optimized.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.spi.indexing.h2.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;

import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheDistributionMode.*;
import static org.gridgain.grid.cache.GridCachePreloadMode.*;

/**
 * Tests cross cache queries.
 */
public class GridCacheCrossCacheQuerySelfTest extends GridCommonAbstractTest {
    /** */
    private static final GridTcpDiscoveryIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder(true);

    /** */
    private Grid grid;

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration c = super.getConfiguration(gridName);

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setIpFinder(ipFinder);

        c.setDiscoverySpi(disco);

        c.setMarshaller(new GridOptimizedMarshaller(false));

        GridH2IndexingSpi indexing = new GridH2IndexingSpi();

        c.setIndexingSpi(indexing);

        c.setCacheConfiguration(createCache("replicated", GridCacheMode.REPLICATED),
            createCache("partitioned", GridCacheMode.PARTITIONED));

        return c;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        grid = startGridsMultiThreaded(3);
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();

        grid = null;
    }

    /**
     * Creates new cache configuration.
     *
     * @param name Cache name.
     * @param mode Cache mode.
     * @return Cache configuration.
     */
    private static GridCacheConfiguration createCache(String name, GridCacheMode mode) {
        GridCacheConfiguration cc = defaultCacheConfiguration();

        cc.setName(name);
        cc.setCacheMode(mode);
        cc.setWriteSynchronizationMode(GridCacheWriteSynchronizationMode.FULL_SYNC);
        cc.setPreloadMode(SYNC);
        cc.setSwapEnabled(true);
        cc.setEvictNearSynchronized(false);
        cc.setAtomicityMode(TRANSACTIONAL);
        cc.setDistributionMode(NEAR_PARTITIONED);

        return cc;
    }

    /** @throws Exception If failed. */
    public void testOnProjection() throws Exception {
        GridCacheProjection<Integer, FactPurchase> prj = grid.<Integer, FactPurchase>cache("partitioned").projection(
            new GridPredicate<GridCacheEntry<Integer, FactPurchase>>() {
                @Override public boolean apply(GridCacheEntry<Integer, FactPurchase> e) {
                    return e.getKey() > 12;
                }
            });

        List<Map.Entry<Integer, FactPurchase>> res = body(prj);

        check(res);
    }

    /**
     * Fills the caches with data and executes the query.
     *
     * @param prj Cache projection.
     * @throws Exception If failed.
     * @return Result.
     */
    private List<Map.Entry<Integer, FactPurchase>> body(GridCacheProjection<Integer, FactPurchase> prj)
        throws Exception {
        int idGen = 0;

        GridCache<Integer, Object> dimCache = grid.cache("replicated");

        for (int i = 0; i < 2; i++) {
            int id = idGen++;

            dimCache.put(id, new DimStore(id, "Store" + id));
        }

        for (int i = 0; i < 5; i++) {
            int id = idGen++;

            dimCache.put(id, new DimProduct(id, "Product" + id));
        }

        GridCacheProjection<Integer, DimStore> stores = dimCache.projection(Integer.class, DimStore.class);
        GridCacheProjection<Integer, DimProduct> prods = dimCache.projection(Integer.class, DimProduct.class);

        GridCache<Integer, FactPurchase> factCache = grid.cache("partitioned");

        List<DimStore> dimStores = new ArrayList<>(stores.values());
        Collections.sort(dimStores, new Comparator<DimStore>() {
            @Override public int compare(DimStore o1, DimStore o2) {
                return o1.getId() > o2.getId() ? 1 : o1.getId() < o2.getId() ? -1 : 0;
            }
        });

        List<DimProduct> dimProds = new ArrayList<>(prods.values());
        Collections.sort(dimProds, new Comparator<DimProduct>() {
            @Override
            public int compare(DimProduct o1, DimProduct o2) {
                return o1.getId() > o2.getId() ? 1 : o1.getId() < o2.getId() ? -1 : 0;
            }
        });

        for (int i = 0; i < 10; i++) {
            int id = idGen++;

            DimStore store = dimStores.get(i % dimStores.size());
            DimProduct prod = dimProds.get(i % dimProds.size());

            factCache.put(id, new FactPurchase(id, prod.getId(), store.getId()));
        }

        GridCacheQuery<Map.Entry<Integer, FactPurchase>> qry = (prj == null ? factCache : prj).queries().createSqlQuery(
            FactPurchase.class,
            "from \"replicated\".DimStore, \"partitioned\".FactPurchase where DimStore.id = FactPurchase.storeId");

        List<Map.Entry<Integer, FactPurchase>> res = new ArrayList<>(qry.execute().get());
        Collections.sort(res, new Comparator<Map.Entry<Integer, FactPurchase>>() {
            @Override public int compare(Map.Entry<Integer, FactPurchase> o1, Map.Entry<Integer, FactPurchase> o2) {
                return o1.getKey() > o2.getKey() ? 1 : o1.getKey() < o2.getKey() ? -1 : 0;
            }
        });

        return res;
    }

    /**
     * Checks result.
     * @param res Result to check.
     */
    private static void check(List<Map.Entry<Integer, FactPurchase>> res) {
        assertEquals("Result size", 4, res.size());

        checkPurchase(res.get(0), 13, 3, 0);
        checkPurchase(res.get(1), 14, 4, 1);
        checkPurchase(res.get(2), 15, 5, 0);
        checkPurchase(res.get(3), 16, 6, 1);
    }

    /**
     * Checks purchase.
     * @param entry Entry to check.
     * @param id ID.
     * @param productId Product ID.
     * @param storeId Store ID.
     */
    private static void checkPurchase(Map.Entry<Integer, FactPurchase> entry, int id, int productId, int storeId) {
        FactPurchase purchase = entry.getValue();

        assertEquals("Id", id, entry.getKey().intValue());
        assertEquals("Id", id, purchase.getId());
        assertEquals("ProductId", productId, purchase.getProductId());
        assertEquals("StoreId", storeId, purchase.getStoreId());
    }

    /**
     * Represents a product available for purchase. In our {@code snowflake} schema a {@code product} is a {@code
     * 'dimension'} and will be cached in {@link GridCacheMode#REPLICATED} cache.
     */
    private static class DimProduct {
        /** Primary key. */
        @GridCacheQuerySqlField(unique = true)
        private int id;

        /** Product name. */
        private String name;

        /**
         * Constructs a product instance.
         *
         * @param id Product ID.
         * @param name Product name.
         */
        DimProduct(int id, String name) {
            this.id = id;
            this.name = name;
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
    }

    /**
     * Represents a physical store location. In our {@code snowflake} schema a {@code store} is a {@code 'dimension'}
     * and will be cached in {@link GridCacheMode#REPLICATED} cache.
     */
    private static class DimStore {
        /** Primary key. */
        @GridCacheQuerySqlField(unique = true)
        private int id;

        /** Store name. */
        @GridCacheQuerySqlField
        private String name;

        /**
         * Constructs a store instance.
         *
         * @param id Store ID.
         * @param name Store name.
         */
        DimStore(int id, String name) {
            this.id = id;
            this.name = name;
        }

        /**
         * Gets store ID.
         *
         * @return Store ID.
         */
        public int getId() {
            return id;
        }

        /**
         * Gets store name.
         *
         * @return Store name.
         */
        public String getName() {
            return name;
        }
    }

    /**
     * Represents a purchase record. In our {@code snowflake} schema purchase is a {@code 'fact'} and will be cached in
     * larger {@link GridCacheMode#PARTITIONED} cache.
     */
    private static class FactPurchase {
        /** Primary key. */
        @GridCacheQuerySqlField(unique = true)
        private int id;

        /** Foreign key to store at which purchase occurred. */
        @GridCacheQuerySqlField
        private int storeId;

        /** Foreign key to purchased product. */
        @GridCacheQuerySqlField
        private int productId;

        /**
         * Constructs a purchase record.
         *
         * @param id Purchase ID.
         * @param productId Purchased product ID.
         * @param storeId Store ID.
         */
        FactPurchase(int id, int productId, int storeId) {
            this.id = id;
            this.productId = productId;
            this.storeId = storeId;
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
    }
}
