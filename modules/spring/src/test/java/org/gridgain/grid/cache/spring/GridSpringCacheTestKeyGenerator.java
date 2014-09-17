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

import org.springframework.cache.interceptor.*;

import java.lang.reflect.*;

/**
 * Key generator.
 */
public class GridSpringCacheTestKeyGenerator implements KeyGenerator {
    /** {@inheritDoc} */
    @Override public Object generate(Object target, Method mtd, Object... params) {
        assert params != null;
        assert params.length > 0;

        if (params.length == 1)
            return params[0];
        else {
            assert params.length == 2;

            return new GridSpringCacheTestKey((Integer)params[0], (String)params[1]);
        }
    }
}
