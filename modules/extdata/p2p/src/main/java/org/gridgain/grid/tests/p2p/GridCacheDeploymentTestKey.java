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

package org.gridgain.grid.tests.p2p;

/**
 * Test key for cache deployment tests.
 */
public class GridCacheDeploymentTestKey {
    /** */
    private String key;

    /**
     * Empty constructor.
     */
    public GridCacheDeploymentTestKey() {
        // No-op.
    }

    /**
     * @param key Key.
     */
    public GridCacheDeploymentTestKey(String key) {
        this.key = key;
    }

    /**
     * @param key Key value.
     */
    public void key(String key) {
        this.key = key;
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GridCacheDeploymentTestKey that = (GridCacheDeploymentTestKey)o;

        return !(key != null ? !key.equals(that.key) : that.key != null);

    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return key != null ? key.hashCode() : 0;
    }
}
