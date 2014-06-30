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

/**
 * Annotates a field or a setter method for injection of address resolver.
 * Address resolver is provided via {@link org.gridgain.grid.GridConfiguration#getAddressResolver()}.
 * <p>
 * Address resolver can be injected into instances of {@link org.gridgain.grid.spi.GridSpi}.
 * <p>
 * Here is how injection would typically happen:
 * <pre name="code" class="java">
 * public class SomeGridSpi implements GridSpi {
 *      ...
 *      &#64;GridAddressResolverResource
 *      private GridAddressResolver addrsRslvr;
 *      ...
 *  }
 * </pre>
 * or
 * <pre name="code" class="java">
 * public class SomeGridSpi implements GridSpi {
 *     ...
 *     private GridAddressResolver addrsRslvr;
 *     ...
 *     &#64;GridAddressResolverResource
 *     public void setAddressResolver(GridAddressResolver addrsRslvr) {
 *          this.addrsRslvr = addrsRslvr;
 *     }
 *     ...
 * }
 * </pre>
 * <p>
 * See {@link org.gridgain.grid.GridConfiguration#getAddressResolver()} for Grid configuration details.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface GridAddressResolverResource {
    // No-op.
}
