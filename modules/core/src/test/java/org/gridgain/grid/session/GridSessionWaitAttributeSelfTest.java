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

package org.gridgain.grid.session;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.junits.common.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Tests waiting for session attributes.
 */
@GridCommonTest(group = "Task Session")
@SuppressWarnings({"PublicInnerClass"})
public class GridSessionWaitAttributeSelfTest extends GridCommonAbstractTest {
    /** */
    private static final int ATTR_NUM = 100;

    /** */
    private static final int JOB_NUM = 10;

    /** */
    private static final long WAIT_TIMEOUT = 20000;

    /** */
    private enum WaitAttributeType {
        /** waitForAttribute(Serializable key). */
        WAIT_FOR_ATTRIBUTE_KEY,

        /** waitForAttribute(Serializable key, Serializable val). */
        WAIT_FOR_ATTRIBUTE_KEY_VAL,

        /** waitForAttribute(Serializable key, long timeout). */
        WAIT_FOR_ATTRIBUTE_KEY_TIMEOUT,

        /** waitForAttribute(Serializable key, Serializable val, long timeout). */
        WAIT_FOR_ATTRIBUTE_KEY_VAL_TIMEOUT,

        /** waitForAttributes(Collection<? extends Serializable> keys). */
        WAIT_FOR_ATTRIBUTES_KEYS,

        /** waitForAttributes(Map<? extends Serializable, ? extends Serializable> attrs). */
        WAIT_FOR_ATTRIBUTES_ATTRS,

        /** waitForAttributes(Collection<? extends Serializable> keys, long timeout). */
        WAIT_FOR_ATTRIBUTES_KEYS_TIMEOUT,

        /** waitForAttributes(Map<? extends Serializable, ? extends Serializable> attrs, long timeout). */
        WAIT_FOR_ATTRIBUTES_ATTRS_TIMEOUT
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration c = super.getConfiguration(gridName);

        GridTcpDiscoverySpi discoSpi = new GridTcpDiscoverySpi();

        discoSpi.setIpFinder(new GridTcpDiscoveryVmIpFinder(true));

        c.setDiscoverySpi(discoSpi);

        c.setExecutorService(
            new ThreadPoolExecutor(
                JOB_NUM * 2,
                JOB_NUM * 2,
                0, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>()));

        c.setExecutorServiceShutdown(true);

        return c;
    }


    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        startGrid(1);
        startGrid(2);
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        stopGrid(1);
        stopGrid(2);
    }

    /**
     * @param prefix Prefix.
     * @param mtd Method.
     * @param i Index.
     * @return Session attribute key.
     */
    private static String createKey(String prefix, Enum mtd, int i) {
        assert prefix != null;
        assert mtd != null;

        return prefix + "test.key." + mtd.name() + '.' + i;
    }

    /**
     * @param prefix Prefix.
     * @param mtd Method.
     * @param i Index.
     * @return Session attribute value.
     */
    private static String createValue(String prefix, Enum mtd, int i) {
        assert prefix != null;
        assert mtd != null;

        return prefix + "test.value." + mtd.name() + '.' + i;
    }

    /**
     * @throws Exception If failed.
     */
    public void testWaitAttribute() throws Exception {
        checkWaitAttributeMethod(WaitAttributeType.WAIT_FOR_ATTRIBUTE_KEY);
    }

    /**
     * @throws Exception If failed.
     */
    public void testWaitAttributeWithTimeout() throws Exception {
        checkWaitAttributeMethod(WaitAttributeType.WAIT_FOR_ATTRIBUTE_KEY_TIMEOUT);
    }

    /**
     * @throws Exception If failed.
     */
    public void testWaitAttributeValue() throws Exception {
        checkWaitAttributeMethod(WaitAttributeType.WAIT_FOR_ATTRIBUTE_KEY_VAL);
    }

    /**
     * @throws Exception If failed.
     */
    public void testWaitAttributeValueWithTimeout() throws Exception {
        checkWaitAttributeMethod(WaitAttributeType.WAIT_FOR_ATTRIBUTE_KEY_VAL_TIMEOUT);
    }

    /**
     * @throws Exception If failed.
     */
    public void testWaitAttributeValues() throws Exception {
        checkWaitAttributeMethod(WaitAttributeType.WAIT_FOR_ATTRIBUTES_ATTRS);
    }

    /**
     * @throws Exception If failed.
     */
    public void testWaitAttributeValuesWithTimeout() throws Exception {
        checkWaitAttributeMethod(WaitAttributeType.WAIT_FOR_ATTRIBUTES_ATTRS_TIMEOUT);
    }

    /**
     * @throws Exception If failed.
     */
    public void testWaitAttributes() throws Exception {
        checkWaitAttributeMethod(WaitAttributeType.WAIT_FOR_ATTRIBUTES_KEYS);
    }

    /**
     * @throws Exception If failed.
     */
    public void testWaitAttributesWithTimeout() throws Exception {
        checkWaitAttributeMethod(WaitAttributeType.WAIT_FOR_ATTRIBUTES_KEYS_TIMEOUT);
    }

    /**
     * @param type Type.
     * @throws Exception If failed.
     */
    @SuppressWarnings("unchecked")
    private void checkWaitAttributeMethod(WaitAttributeType type) throws Exception {
        assert type != null;

        Grid grid1 = G.grid(getTestGridName() + '1');
        Grid grid2 = G.grid(getTestGridName() + '2');

        assert grid1 != null;
        assert grid2 != null;

        grid1.compute().localDeployTask(TestSessionTask.class, TestSessionTask.class.getClassLoader());

        GridComputeTaskFuture<?> fut = grid1.compute().execute(TestSessionTask.class.getName(), type);

        fut.waitForMap();

        GridComputeTaskSession ses = fut.getTaskSession();

        info("Task job siblings [size=" + ses.getJobSiblings().size() + ", siblings=" + ses.getJobSiblings() + ']');

        for (int i = 0; i < ATTR_NUM; i++) {
            String key = createKey("fut", type, i);
            String val = createValue("fut", type, i);

            ses.setAttribute(key, val);
        }

        // Check all job attributes.
        for (GridComputeJobSibling sibling : ses.getJobSiblings()) {
            info("Checking session attributes for sibling: " + sibling);

            checkSessionAttributes(ses, sibling.getJobId().toString(), type);
        }

        // Check that fut attributes have been set.
        checkSessionAttributes(ses, "fut", type);

        // Signal finish.
        ses.setAttribute("done", true);

        fut.get();
    }

    /**
     * @param ses Session.
     * @param prefix Prefix.
     * @param type Type.
     * @throws GridException If failed.
     */
    private static void checkSessionAttributes(GridComputeTaskSession ses, String prefix, WaitAttributeType type)
        throws GridException {
        assert ses != null;
        assert type != null;

        try {
            switch (type) {
                case WAIT_FOR_ATTRIBUTE_KEY: {
                    for (int i = 0; i < ATTR_NUM; i++) {
                        String key = createKey(prefix, type, i);
                        String val = createValue(prefix, type, i);

                        Serializable obj = ses.waitForAttribute(key);

                        assert obj != null :
                            "Failed to wait for attribute [key=" + key + ", val=" + val + ", receivedVal=" + obj + ']';
                        assert val.equals(obj) :
                            "Failed to wait for attribute [key=" + key + ", val=" + val + ", receivedVal=" + obj + ']';

                        //System.out.println(Thread.currentThread().getName() + ":: Waited for attribute [key=" + key + ", val=" + obj + ", ses=" + ses + ']');
                    }

                    break;
                }

                case WAIT_FOR_ATTRIBUTE_KEY_TIMEOUT: {
                    for (int i = 0; i < ATTR_NUM; i++) {
                        String key = createKey(prefix, type, i);
                        String val = createValue(prefix, type, i);

                        Serializable obj = ses.waitForAttribute(key, WAIT_TIMEOUT);

                        assert obj != null :
                            "Failed to wait for attribute [key=" + key + ", val=" + val + ", receivedVal=" + obj + ']';
                        assert val.equals(obj) :
                            "Failed to wait for attribute [key=" + key + ", val=" + val + ", receivedVal=" + obj + ']';
                    }

                    break;
                }

                case WAIT_FOR_ATTRIBUTE_KEY_VAL: {
                    for (int i = 0; i < ATTR_NUM; i++) {
                        String key = createKey(prefix, type, i);
                        String val = createValue(prefix, type, i);

                        boolean attr = ses.waitForAttribute(key, val);

                        assert attr :
                            "Failed to wait for attribute [key=" + key + ", val=" + val + ']';
                    }

                    break;
                }

                case WAIT_FOR_ATTRIBUTE_KEY_VAL_TIMEOUT: {
                    for (int i = 0; i < ATTR_NUM; i++) {
                        String key = createKey(prefix, type, i);
                        String val = createValue(prefix, type, i);

                        boolean attr = ses.waitForAttribute(key, val, WAIT_TIMEOUT);

                        assert attr :
                            "Failed to wait for attribute [key=" + key + ", val=" + val + ']';
                    }

                    break;
                }

                case WAIT_FOR_ATTRIBUTES_ATTRS: {
                    Map<Object, Object> map = new HashMap<>();

                    for (int i = 0; i < ATTR_NUM; i++)
                        map.put(createKey(prefix, type, i), createValue(prefix, type, i));

                    boolean attrs = ses.waitForAttributes(map);

                    assert attrs :
                        "Failed to wait for attribute [attrs=" + map + ']';

                    break;
                }

                case WAIT_FOR_ATTRIBUTES_ATTRS_TIMEOUT: {
                    Map<Object, Object> map = new HashMap<>();

                    for (int i = 0; i < ATTR_NUM; i++)
                        map.put(createKey(prefix, type, i), createValue(prefix, type, i));

                    boolean attrs = ses.waitForAttributes(map, WAIT_TIMEOUT);

                    assert attrs :
                        "Failed to wait for attribute [attrs=" + map + ']';

                    break;
                }

                case WAIT_FOR_ATTRIBUTES_KEYS: {
                    Map<Object, Object> map = new HashMap<>();

                    for (int i = 0; i < ATTR_NUM; i++)
                        map.put(createKey(prefix, type, i), createValue(prefix, type, i));

                    Map<?, ?> res = ses.waitForAttributes(map.keySet());

                    assert res != null : "Failed to wait for attribute [keys=" + map.keySet() + ']';

                    for (Map.Entry<Object, Object> entry : map.entrySet()) {
                        Object obj = res.get(entry.getKey());

                        assert obj != null : "Failed to get value from result map [key=" + entry.getKey() + ']';
                        assert entry.getValue().equals(obj) : "Fount unexpected value [key=" + entry.getKey()
                            + ", val=" + obj + ", expected=" + entry.getValue();
                    }

                    break;
                }

                case WAIT_FOR_ATTRIBUTES_KEYS_TIMEOUT: {
                    Map<Object, Object> map = new HashMap<>();

                    for (int i = 0; i < ATTR_NUM; i++)
                        map.put(createKey(prefix, type, i), createValue(prefix, type, i));

                    Map<?, ?> res = ses.waitForAttributes(map.keySet(), WAIT_TIMEOUT);

                    assert res != null : "Failed to wait for attribute [keys=" + map.keySet() + ']';

                    for (Map.Entry<Object, Object> entry : map.entrySet()) {
                        Object obj = res.get(entry.getKey());

                        assert obj != null : "Failed to get value from result map [key=" + entry.getKey() + ']';
                        assert entry.getValue().equals(obj) : "Fount unexpected value [key=" + entry.getKey()
                            + ", val=" + obj + ", expected=" + entry.getValue();
                    }

                    break;
                }

                default: {
                    assert false : "Unknown session wait type.";
                }
            }
        }
        catch (InterruptedException e) {
            throw new GridException("Got interrupted while waiting for session attributes.", e);
        }
    }

    /** */
    @GridComputeTaskSessionFullSupport
    public static class TestSessionTask extends GridComputeTaskSplitAdapter<WaitAttributeType, Object> {
        /** {@inheritDoc} */
        @Override protected Collection<TestSessionJob> split(int gridSize, WaitAttributeType type) throws GridException {
            assert type != null;

            Collection<TestSessionJob> jobs = new ArrayList<>(JOB_NUM);

            for (int i = 0; i < JOB_NUM; i++)
                jobs.add(new TestSessionJob(type));

            return jobs;
        }

        /** {@inheritDoc} */
        @Override public Object reduce(List<GridComputeJobResult> results) throws GridException {
            return null;
        }
    }

    /** */
    public static class TestSessionJob extends GridComputeJobAdapter {
        /** */
        @GridTaskSessionResource private GridComputeTaskSession taskSes;

        /** */
        @GridJobContextResource private GridComputeJobContext jobCtx;

        /** Logger. */
        @GridLoggerResource private GridLogger log;

        /**
         * @param arg Wait attribute type.
         */
        public TestSessionJob(WaitAttributeType arg) {
            super(arg);
        }

        /** {@inheritDoc} */
        @Override public Serializable execute() throws GridException {
            WaitAttributeType m = argument(0);

            checkSessionAttributes(taskSes, "fut", m);

            GridUuid jobId = jobCtx.getJobId();

            for (int i = 0; i < ATTR_NUM; i ++) {
                String key = createKey(jobId.toString(), m, i);
                String val = createValue(jobId.toString(), m, i);

                taskSes.setAttribute(key, val);
            }

            // Check that attributes just set are present.
            checkSessionAttributes(taskSes, jobId.toString(), m);

            Collection<GridComputeJobSibling> siblings = taskSes.getJobSiblings();

            if (log.isInfoEnabled())
                log.info("Got siblings from job [size=" + siblings.size() + ", siblings=" + siblings + ']');

            // Check attributes from siblings.
            for (GridComputeJobSibling sibling : taskSes.getJobSiblings()) {
                if (!sibling.getJobId().equals(jobId))
                    checkSessionAttributes(taskSes, sibling.getJobId().toString(), m);
            }

            try {
                taskSes.waitForAttribute("done", true);
            }
            catch (InterruptedException e) {
                throw new GridException("Got interrupted while waiting for 'done' attribute.", e);
            }

            return null;
        }
    }
}
