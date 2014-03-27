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

package org.gridgain.grid.resources;

import java.lang.annotation.*;
import org.gridgain.grid.cache.*;

/**
 * Annotates a field or a setter method for injection of grid cache name.
 * Grid cache name is provided to cache via {@link GridCacheConfiguration#getName()} method.
 * <p>
 * Cache name can be injected into components provided in the {@link GridCacheConfiguration},
 * if {@link GridCacheNameResource} annotation is used in another classes it is no-op.
 * <p>
 * Here is how injection would typically happen:
 * <pre name="code" class="java">
 * public class MyCacheStore implements GridCacheStore {
 *      ...
 *      &#64;GridCacheNameResource
 *      private String cacheName;
 *      ...
 *  }
 * </pre>
 * or
 * <pre name="code" class="java">
 * public class MyCacheStore implements GridCacheStore {
 *     ...
 *     private String cacheName;
 *     ...
 *     &#64;GridCacheNameResource
 *     public void setCacheName(String cacheName) {
 *          this.cacheName = cacheName;
 *     }
 *     ...
 * }
 * </pre>
 * <p>
 * See {@link GridCacheConfiguration#getName()} for cache configuration details.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface GridCacheNameResource {
    // No-op.
}
