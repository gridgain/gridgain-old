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

package org.gridgain.grid;

import org.gridgain.grid.compute.*;
import org.gridgain.grid.resources.*;
import org.jetbrains.annotations.*;

/**
 * Grid deployment mode. Deployment mode is specified at grid startup via
 * {@link GridConfiguration#getDeploymentMode()} configuration property
 * (it can also be specified in Spring XML configuration file). The main
 * difference between all deployment modes is how classes and user resources
 * are loaded on remote nodes via peer-class-loading mechanism. User resources
 * can be instances of caches, databased connections, or any other class
 * specified by user with {@link GridUserResource @GridUserResource} annotation.
 * <p>
 * Refer to {@link GridUserResource} documentation and examples for more
 * information on how user resources are created and injected.
 * <p>
 * The following deployment modes are supported:
 * <ul>
 * <li>{@link #PRIVATE}</li>
 * <li>{@link #ISOLATED}</li>
 * <li>{@link #SHARED}</li>
 * <li>{@link #CONTINUOUS}</li>
 * </ul>
 * <h1 class="header">User Version</h1>
 * User version comes into play whenever you would like to redeploy tasks deployed
 * in {@link #SHARED} or {@link #CONTINUOUS} modes. By default, GridGain will
 * automatically detect if class-loader changed or a node is restarted. However,
 * if you would like to change and redeploy code on a subset of nodes, or in
 * case of {@link #CONTINUOUS} mode to kill the ever living deployment, you should
 * change the user version.
 * <p>
 * User version is specified in {@code META-INF/gridgain.xml} file as follows:
 * <pre name="code" class="xml">
 *    &lt;!-- User version. --&gt;
 *    &lt;bean id="userVersion" class="java.lang.String"&gt;
 *        &lt;constructor-arg value="0"/&gt;
 *    &lt;/bean>
 * </pre>
 * By default, all gridgain startup scripts ({@code gridgain.sh} or {@code gridgain.bat})
 * pick up user version from {@code GRIDGAIN_HOME/config/userversion} folder. Usually, it
 * is just enough to update user version under that folder, however, in case of {@code GAR}
 * or {@code JAR} deployment, you should remember to provide {@code META-INF/gridgain.xml}
 * file with desired user version in it.
 * <p>
 * <h1 class="header">Always-Local Development</h1>
 * GridGain deployment (regardless of mode) allows you to develop everything as you would
 * locally. You never need to specifically write any kind of code for remote nodes. For
 * example, if you need to use a distributed cache from your {@link GridComputeJob}, then you can
 * the following:
 * <ol>
 *  <li>
 *      Simply startup stand-alone GridGain nodes by executing
 *      {@code GRIDGAIN_HOME/ggstart.{sh|bat}} scripts.
 *  <li>
 *      Inject your cache instance into your jobs via
 *      {@link GridUserResource @GridUserResource} annotation. The cache can be initialized
 *      and destroyed with {@link GridUserResourceOnDeployed @GridUserResourceOnDeployed} and
 *      {@link GridUserResourceOnUndeployed @GridUserResourceOnUndeployed} annotations.
 *  </li>
 *  <li>
 *      Now, all jobs executing locally or remotely can have a single instance of cache
 *      on every node, and all jobs can access instances stored by any other job without
 *      any need for explicit deployment.
 *  </li>
 * </ol>
 */
public enum GridDeploymentMode {
    /**
     * In this mode deployed classes do not share user resources
     * (see {@link GridUserResource}). Basically, user resources are created
     * once per deployed task class and then get reused for all executions.
     * <p>
     * Note that classes deployed within the same class loader on master
     * node, will still share the same class loader remotely on worker nodes.
     * However, tasks deployed from different master nodes will not
     * share the same class loader on worker nodes, which is useful in
     * development when different developers can be working on different
     * versions of the same classes.
     * <p>
     * Also note that resources are associated with task deployment,
     * not task execution. If the same deployed task gets executed multiple
     * times, then it will keep reusing the same user resources
     * every time.
     */
    PRIVATE,

    /**
     * Unlike {@link #PRIVATE} mode, where different deployed tasks will
     * never use the same instance of user resources, in {@code ISOLATED}
     * mode, tasks or classes deployed within the same class loader
     * will share the same instances of user resources (see {@link GridUserResource}).
     * This means that if multiple tasks classes are loaded by the same
     * class loader on master node, then they will share instances
     * of user resources on worker nodes. In other words, user resources
     * get initialized once per class loader and then get reused for all
     * consecutive executions.
     * <p>
     * Note that classes deployed within the same class loader on master
     * node, will still share the same class loader remotely on worker nodes.
     * However, tasks deployed from different master nodes will not
     * share the same class loader on worker nodes, which is especially
     * useful when different developers can be working on different versions
     * of the same classes.
     */
    ISOLATED,

    /**
     * Same as {@link #ISOLATED}, but now tasks from
     * different master nodes with the same user version and same
     * class loader will share the same class loader on remote
     * nodes. Classes will be undeployed whenever all master
     * nodes leave grid or user version changes.
     * <p>
     * The advantage of this approach is that it allows tasks coming from
     * different master nodes share the same instances of user resources
     * (see {@link GridUserResource}) on worker nodes. This allows for all
     * tasks executing on remote nodes to reuse, for example, the same instances of
     * connection pools or caches. When using this mode, you can
     * startup multiple stand-alone GridGain worker nodes, define user resources
     * on master nodes and have them initialize once on worker nodes regardless
     * of which master node they came from.
     * <p>
     * This method is specifically useful in production as, in comparison
     * to {@link #ISOLATED} deployment mode, which has a scope of single
     * class loader on a single master node, this mode broadens the
     * deployment scope to all master nodes.
     * <p>
     * Note that classes deployed in this mode will be undeployed if
     * all master nodes left grid or if user version changed. User version can
     * be specified in {@code META-INF/gridgain.xml} file as a Spring bean
     * property with name {@code userVersion}. This file has to be in the class
     * path of the class used for task execution.
     * <p>
     * {@code SHARED} deployment mode is default mode used by the grid.
     */
    SHARED,

    /**
     * Same as {@link #SHARED} deployment mode, but user resources
     * (see {@link GridUserResource}) will not be undeployed even after all master
     * nodes left grid. Tasks from different master nodes with the same user
     * version and same class loader will share the same class loader on remote
     * worker nodes. Classes will be undeployed whenever user version changes.
     * <p>
     * The advantage of this approach is that it allows tasks coming from
     * different master nodes share the same instances of user resources
     * (see {@link GridUserResource}) on worker nodes. This allows for all
     * tasks executing on remote nodes to reuse, for example, the same instances of
     * connection pools or caches. When using this mode, you can
     * startup multiple stand-alone GridGain worker nodes, define user resources
     * on master nodes and have them initialize once on worker nodes regardless
     * of which master node they came from.
     * <p>
     * This method is specifically useful in production as, in comparison
     * to {@link #ISOLATED} deployment mode, which has a scope of single
     * class loader on a single master node, <tt>CONTINUOUS</tt> mode broadens
     * the deployment scope to all master nodes.
     * <p>
     * Note that classes deployed in <tt>CONTINUOUS</tt> mode will be undeployed
     * only if user version changes. User version can be specified in
     * {@code META-INF/gridgain.xml} file as a Spring bean property with name
     * {@code userVersion}. This file has to be in the class
     * path of the class used for task execution.
     */
    CONTINUOUS;

    /** Enum values. */
    private static final GridDeploymentMode[] VALS = values();

    /**
     * Efficiently gets enumerated value from its ordinal.
     *
     * @param ord Ordinal value.
     * @return Enumerated value.
     */
    @Nullable public static GridDeploymentMode fromOrdinal(int ord) {
        return ord >= 0 && ord < VALS.length ? VALS[ord] : null;
    }
}
