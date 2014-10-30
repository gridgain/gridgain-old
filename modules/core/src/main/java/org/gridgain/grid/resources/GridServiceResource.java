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
import java.util.*;

/**
 * Annotates a field or a setter method for injection of GridGain service(s) by specified service name.
 * If you want to get all services associated with specified service name than use {@link Collection} type
 * (note that no descendant classes are allowed). Otherwise, just use the type of your service class.
 * <p>
 * Here is how injection would typically happen:
 * <pre name="code" class="java">
 * public class MyGridJob implements GridComputeJob {
 *      ...
 *      // Inject single instance of 'myService'. If there is
 *      // more than one, first deployed instance will be picked.
 *      &#64;GridServiceResource(serviceName = "myService")
 *      private MyService srvc;
 *      ...
 *  }
 * </pre>
 * or
 * <pre name="code" class="java">
 * public class MyGridJob implements GridComputeJob {
 *      ...
 *      // Inject all locally deployed instances of 'myService'.
 *      &#64;GridServiceResource(serviceName = "myService")
 *      private Collection&lt;MyService&gt; srvc;
 *      ...
 *  }
 * </pre>
 * or attach the same annotations to methods:
 * <pre name="code" class="java">
 * public class MyGridJob implements GridComputeJob {
 *     ...
 *     private Collection&lt;MyService&gt; srvcs;
 *     ...
 *      // Inject all locally deployed instances of 'myService'.
 *     &#64;GridServiceResource(serviceName = "myService")
 *     public void setGridServices(Collection&lt;MyService&gt; srvcs) {
 *          this.srvcs = srvcs;
 *     }
 *     ...
 * }
 * </pre>
 * <p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface GridServiceResource {
    /**
     * Service name.
     *
     * @return Name of the injected services.
     */
    public String serviceName();
}
