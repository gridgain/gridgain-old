/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.cache.spring;

import org.springframework.cache.annotation.*;

/**
 * Test service.
 */
public class GridSpringDynamicCacheTestService {
    /**
     * @param key Key.
     * @return Value.
     */
    @Cacheable({"testCache1", "testCache2"})
    public String cacheable(Integer key) {
        assert key != null;

        return "value" + key;
    }

    /**
     * @param key Key.
     * @return Value.
     */
    @CachePut({"testCache1", "testCache2"})
    public String cachePut(Integer key) {
        assert key != null;

        return "value" + key;
    }

    /**
     * @param key Key.
     */
    @CacheEvict("testCache1")
    public void cacheEvict(Integer key) {
        // No-op.
    }

    /**
     */
    @CacheEvict(value = "testCache1", allEntries = true)
    public void cacheEvictAll() {
        // No-op.
    }
}
