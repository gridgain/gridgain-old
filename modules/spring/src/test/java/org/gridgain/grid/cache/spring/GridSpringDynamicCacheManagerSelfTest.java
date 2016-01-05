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

package org.gridgain.grid.cache.spring;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.testframework.junits.common.*;
import org.springframework.beans.factory.*;
import org.springframework.cache.*;
import org.springframework.context.support.*;

import java.lang.reflect.*;
import java.util.*;

/**
 * Spring cache test.
 */
public class GridSpringDynamicCacheManagerSelfTest extends GridCommonAbstractTest {
    /** */
    private static final GridTcpDiscoveryIpFinder IP_FINDER = new GridTcpDiscoveryVmIpFinder(true);

    /** */
    private static final String DATA_CACHE_NAME = "data";

    /** */
    private GridSpringDynamicCacheTestService svc;

    /** */
    private CacheManager mgr;

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        GridCacheConfiguration cache = new GridCacheConfiguration();

        cache.setName(DATA_CACHE_NAME);

        cfg.setCacheConfiguration(cache);

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setIpFinder(IP_FINDER);

        cfg.setDiscoverySpi(disco);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override public String getTestGridName() {
        return "testGrid";
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        startGrid();
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        stopAllGrids();
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        BeanFactory factory = new ClassPathXmlApplicationContext(
            "org/gridgain/grid/cache/spring/spring-dynamic-caching.xml");

        svc = (GridSpringDynamicCacheTestService)factory.getBean("testService");
        mgr = (CacheManager)factory.getBean("cacheManager");
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        grid().cache(DATA_CACHE_NAME).removeAll();
    }

    /**
     * @throws Exception If failed.
     */
    public void testNames() throws Exception {
        assertEquals("value1", svc.cacheable(1));

        Collection<String> names = mgr.getCacheNames();

        assertEquals(names.toString(), 2, names.size());
    }

    /**
     * @throws Exception If failed.
     */
    public void testCacheAndEvict() throws Exception {
        GridCache<Object, String> c = grid().cache(DATA_CACHE_NAME);

        assertEquals("value1", svc.cacheable(1));

        assertEquals(2, c.size());

        assertEquals("value1", c.get(key("testCache1", 1)));
        assertEquals("value1", c.get(key("testCache2", 1)));

        svc.cacheEvict(1);

        assertEquals(1, c.size());

        assertEquals(null, c.get(key("testCache1", 1)));
        assertEquals("value1", c.get(key("testCache2", 1)));
    }

    /**
     * @throws Exception If failed.
     */
    public void testPutAndEvict() throws Exception {
        GridCache<Object, String> c = grid().cache(DATA_CACHE_NAME);

        assertEquals("value1", svc.cachePut(1));

        assertEquals(2, c.size());

        assertEquals("value1", c.get(key("testCache1", 1)));
        assertEquals("value1", c.get(key("testCache2", 1)));

        svc.cacheEvict(1);

        assertEquals(1, c.size());

        assertEquals(null, c.get(key("testCache1", 1)));
        assertEquals("value1", c.get(key("testCache2", 1)));
    }

    /**
     * @throws Exception If failed.
     */
    public void testCacheAndEvictAll() throws Exception {
        GridCache<Object, String> c = grid().cache(DATA_CACHE_NAME);

        assertEquals("value1", svc.cacheable(1));
        assertEquals("value2", svc.cacheable(2));

        assertEquals(4, c.size());

        assertEquals("value1", c.get(key("testCache1", 1)));
        assertEquals("value1", c.get(key("testCache2", 1)));
        assertEquals("value2", c.get(key("testCache1", 2)));
        assertEquals("value2", c.get(key("testCache2", 2)));

        svc.cacheEvictAll();

        assertEquals(2, c.size());

        assertEquals(null, c.get(key("testCache1", 1)));
        assertEquals("value1", c.get(key("testCache2", 1)));
        assertEquals(null, c.get(key("testCache1", 2)));
        assertEquals("value2", c.get(key("testCache2", 2)));
    }


    /**
     * @throws Exception If failed.
     */
    public void testPutAndEvictAll() throws Exception {
        GridCache<Object, String> c = grid().cache(DATA_CACHE_NAME);

        assertEquals("value1", svc.cachePut(1));
        assertEquals("value2", svc.cachePut(2));

        assertEquals(4, c.size());

        assertEquals("value1", c.get(key("testCache1", 1)));
        assertEquals("value1", c.get(key("testCache2", 1)));
        assertEquals("value2", c.get(key("testCache1", 2)));
        assertEquals("value2", c.get(key("testCache2", 2)));

        svc.cacheEvictAll();

        assertEquals(2, c.size());

        assertEquals(null, c.get(key("testCache1", 1)));
        assertEquals("value1", c.get(key("testCache2", 1)));
        assertEquals(null, c.get(key("testCache1", 2)));
        assertEquals("value2", c.get(key("testCache2", 2)));
    }

    /**
     * @param cacheName Cache name.
     * @param key Key.
     * @return Data key.
     * @throws Exception In case of error.
     */
    private Object key(String cacheName, int key) throws Exception {
        Class<?> cls = Class.forName(GridSpringDynamicCacheManager.class.getName() + "$DataKey");

        Constructor<?> cons = cls.getDeclaredConstructor(String.class, Object.class);

        cons.setAccessible(true);

        return cons.newInstance(cacheName, key);
    }
}
