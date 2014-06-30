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

package org.gridgain.grid.kernal.processors.cache.distributed;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.kernal.processors.cache.distributed.replicated.preloader.*;
import org.gridgain.grid.marshaller.optimized.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.thread.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.junits.common.*;

import java.io.*;
import java.util.concurrent.*;

import static org.gridgain.grid.events.GridEventType.*;
import static org.gridgain.grid.cache.GridCachePreloadMode.*;

/**
 * Tests for replicated cache {@link GridReplicatedPreloader preloader}.
 */
@SuppressWarnings({"PublicInnerClass"})
public class GridCachePreloadLifecycleAbstractTest extends GridCommonAbstractTest {
    /** */
    protected static final String TEST_STRING = "ABC";

    /** */
    protected static final GridCachePreloadMode DFLT_PRELOAD_MODE = SYNC;

    /** */
    protected GridCachePreloadMode preloadMode = DFLT_PRELOAD_MODE;

    /** */
    protected GridLifecycleBean lifecycleBean;

    /** */
    private GridTcpDiscoveryIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder(true);

    /** Default keys. */
    protected static final String[] DFLT_KEYS = new String[] {
        "Branches",
        "CurrencyCurvesAssign",
        "CurRefIndex",
        "MaturityClasses",
        "Folders",
        "FloatingRates",
        "Swap",
        "Portfolios"
    };


    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration c = super.getConfiguration(gridName);

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setIpFinder(ipFinder);

        c.setDiscoverySpi(disco);

        c.setIncludeEventTypes(EVT_TASK_FAILED, EVT_TASK_FINISHED, EVT_JOB_MAPPED);
        c.setIncludeProperties();
        c.setDeploymentMode(GridDeploymentMode.SHARED);
        c.setNetworkTimeout(10000);
        c.setRestEnabled(false);
        c.setMarshaller(new GridOptimizedMarshaller(false));

//        c.setPeerClassLoadingLocalClassPathExclude(GridCachePreloadLifecycleAbstractTest.class.getName(),
//            MyValue.class.getName());

        c.setExecutorService(new GridThreadPoolExecutor(10, 10, 0, new LinkedBlockingQueue<Runnable>()));
        c.setSystemExecutorService(new GridThreadPoolExecutor(10, 10, 0, new LinkedBlockingQueue<Runnable>()));
        c.setPeerClassLoadingExecutorService(new GridThreadPoolExecutor(3, 3, 0, new LinkedBlockingQueue<Runnable>()));

        c.setLifecycleBeans(lifecycleBean);

        return c;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        preloadMode = DFLT_PRELOAD_MODE;
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        super.afterTest();

        lifecycleBean = null;

        stopAllGrids();
    }
    /** {@inheritDoc} */
    @Override protected long getTestTimeout() {
        return 4 * 60 * 1000; // 4 min.
    }

    /**
     * @param key Key.
     * @return Value.
     */
    protected String value(Object key) {
        return TEST_STRING + '-' + key;
    }

    /**
     * @param plain Flag to use plain strings.
     * @param cnt Number of keys to gen.
     * @param lookup Optional key lookup array.
     * @return Generated keys.
     */
    @SuppressWarnings("IfMayBeConditional")
    protected Object[] keys(boolean plain, int cnt, String... lookup) {
        Object[] arr = new Object[cnt];

        for (int i = 0; i < cnt; i++)
            if (plain)
                arr[i] = i < lookup.length ? lookup[i] : "str-" + i;
            else
                arr[i] = i < lookup.length ? new MyStringKey(lookup[i]) : new MyStringKey("str-" + i);

        return arr;
    }

    /**
     *
     */
    public static class MyStringKey implements Serializable {
        /** Key. */
        private final String str;

        /**
         * @param str Key.
         */
        public MyStringKey(String str) {
            this.str = str;
        }

        /** {@inheritDoc} */
        @Override public int hashCode() {
            return 31 + ((str == null) ? 0 : str.hashCode());
        }

        /** {@inheritDoc} */
        @Override public boolean equals(Object obj) {
//            if (this == obj)
//                return true;
//
//            if (obj == null)
//                return false;
//
//            if (getClass() != obj.getClass())
//                return false;
//
//            MyStringKey other = (MyStringKey) obj;
//
//            if (str == null) {
//                if (other.str != null)
//                    return false;
//            }
//            else if (!str.equals(other.str))
//                return false;
//
//            return true;
            return toString().equals(obj.toString());
        }

        /** {@inheritDoc} */
        @Override public String toString() {
//            return str;
            return S.toString(MyStringKey.class, this, "clsLdr", getClass().getClassLoader());
        }
    }

    /**
     *
     */
    public static class MyValue implements Serializable {
        /** Data. */
        private final String data;

        /**
         * @param data Data.
         */
        public MyValue(String data) {
            assert data != null;

            this.data = data;
        }

        /** {@inheritDoc} */
        @Override public boolean equals(Object obj) {
            if (obj == this)
                return true;

            if (obj instanceof MyValue) {
                MyValue myObj = (MyValue) obj;

                return data.equals(myObj.data);
            }

            return false;
            // return data.equals(obj.toString());
        }

        /** {@inheritDoc} */
        @Override public String toString() {
//            return data;
            return S.toString(MyValue.class, this, "clsLdr", getClass().getClassLoader());
        }
    }
}
