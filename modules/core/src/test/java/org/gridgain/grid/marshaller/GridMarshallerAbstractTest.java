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

package org.gridgain.grid.marshaller;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.affinity.*;
import org.gridgain.grid.cache.datastructures.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.events.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.executor.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.kernal.processors.cache.affinity.*;
import org.gridgain.grid.kernal.processors.cache.datastructures.*;
import org.gridgain.grid.kernal.processors.service.*;
import org.gridgain.grid.kernal.processors.streamer.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.marshaller.optimized.*;
import org.gridgain.grid.messaging.*;
import org.gridgain.grid.p2p.*;
import org.gridgain.grid.product.*;
import org.gridgain.grid.scheduler.*;
import org.gridgain.grid.service.*;
import org.gridgain.grid.streamer.*;
import org.gridgain.grid.streamer.window.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.common.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.events.GridEventType.*;

/**
 * Common test for marshallers.
 */
public abstract class GridMarshallerAbstractTest extends GridCommonAbstractTest implements Serializable {
    /** */
    private static final String CACHE_NAME = "namedCache";

    /** */
    private static GridMarshaller marsh;

    /** Closure job. */
    protected GridInClosure<String> c1 = new GridInClosure<String>() {
        @Override public void apply(String s) {
            // No-op.
        }
    };

    /** Closure job. */
    protected GridClosure<String, String> c2 = new GridClosure<String, String>() {
        @Override public String apply(String s) {
            return s;
        }
    };

    /** Argument producer. */
    protected GridOutClosure<String> c3 = new GridOutClosure<String>() {
        @Nullable @Override public String apply() {
            return null;
        }
    };

    /** Reducer. */
    protected GridReducer<String, Object> c4 = new GridReducer<String, Object>() {
        @Override public boolean collect(String e) {
            return true;
        }

        @Nullable @Override public Object reduce() {
            return null;
        }
    };

    /** */
    protected GridMarshallerAbstractTest() {
        super(/*start grid*/true);
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        GridCacheConfiguration namedCache = new GridCacheConfiguration();

        namedCache.setName(CACHE_NAME);
        namedCache.setAtomicityMode(TRANSACTIONAL);
        namedCache.setQueryIndexEnabled(true);

        cfg.setMarshaller(new GridOptimizedMarshaller(false));
        cfg.setStreamerConfiguration(streamerConfiguration());
        cfg.setCacheConfiguration(new GridCacheConfiguration(), namedCache);

        return cfg;
    }

    /**
     * @return Streamer configuration.
     */
    private static GridStreamerConfiguration streamerConfiguration() {
        Collection<GridStreamerStage> stages = F.<GridStreamerStage>asList(new GridStreamerStage() {
            @Override public String name() {
                return "name";
            }

            @Nullable @Override public Map<String, Collection<?>> run(GridStreamerContext ctx, Collection evts) {
                return null;
            }
        });

        GridStreamerConfiguration cfg = new GridStreamerConfiguration();

        cfg.setAtLeastOnce(true);
        cfg.setWindows(F.asList((GridStreamerWindow)new GridStreamerUnboundedWindow()));
        cfg.setStages(stages);

        return cfg;
    }

    /**
     * @return Grid marshaller.
     */
    protected abstract GridMarshaller createMarshaller();

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        marsh = createMarshaller();
    }

    /**
     * @throws Exception If failed.
     */
    public void testDefaultCache() throws Exception {
        GridCache<String, String> cache = grid().cache(null);

        cache.putx("key", "val");

        GridMarshallerTestBean inBean = newTestBean(cache);

        byte[] buf = marshal(inBean);

        GridMarshallerTestBean outBean = unmarshal(buf);

        assert inBean.getObjectField() != null;
        assert outBean.getObjectField() != null;

        assert inBean.getObjectField().getClass().equals(GridCacheProxyImpl.class);
        assert outBean.getObjectField().getClass().equals(GridCacheProxyImpl.class);

        assert inBean != outBean;

        GridCache<String, String> cache0 = (GridCache<String, String>)outBean.getObjectField();

        assertNull(cache0.name());
        assertEquals("val", cache0.get("key"));

        outBean.checkNullResources();
    }

    /**
     * @throws Exception If failed.
     */
    public void testNamedCache() throws Exception {
        GridCache<String, String> cache = grid().cache(CACHE_NAME);

        cache.putx("key", "val");

        GridMarshallerTestBean inBean = newTestBean(cache);

        byte[] buf = marshal(inBean);

        GridMarshallerTestBean outBean = unmarshal(buf);

        assert inBean.getObjectField() != null;
        assert outBean.getObjectField() != null;

        assert inBean.getObjectField().getClass().equals(GridCacheProxyImpl.class);
        assert outBean.getObjectField().getClass().equals(GridCacheProxyImpl.class);

        assert inBean != outBean;

        GridCache<String, String> cache0 = (GridCache<String, String>)outBean.getObjectField();

        assertEquals(CACHE_NAME, cache0.name());
        assertEquals("val", cache0.get("key"));

        outBean.checkNullResources();
    }

    /**
     * Tests marshalling.
     *
     * @throws Exception If test failed.
     */
    public void testMarshalling() throws Exception {
        GridMarshallerTestBean inBean = newTestBean(new Object());

        byte[] buf = marshal(inBean);

        GridMarshallerTestBean outBean = unmarshal(buf);

        assert inBean.getObjectField() != null;
        assert outBean.getObjectField() != null;

        assert inBean.getObjectField().getClass().equals(Object.class);
        assert outBean.getObjectField().getClass().equals(Object.class);

        assert inBean.getObjectField() != outBean.getObjectField();

        assert inBean != outBean;

        assert inBean.equals(outBean);

        outBean.checkNullResources();
    }

    /**
     * Tests marshal anonymous class instance.
     *
     * @throws Exception If test failed.
     */
    public void testMarshallingAnonymousClassInstance() throws Exception {
        final Grid g = grid();

        GridMarshallerTestBean inBean = newTestBean(new GridClosure() {
            /** */
            private Iterable<GridNode> nodes = g.nodes();

            /** {@inheritDoc} */
            @Override public Object apply(Object o) {
                return nodes;
            }
        });

        byte[] buf = marshal(inBean);

        GridMarshallerTestBean outBean = unmarshal(buf);

        assert inBean.getObjectField() != null;
        assert outBean.getObjectField() != null;

        assert GridClosure.class.isAssignableFrom(inBean.getObjectField().getClass());
        assert GridClosure.class.isAssignableFrom(outBean.getObjectField().getClass());

        assert inBean.getObjectField() != outBean.getObjectField();

        assert inBean != outBean;

        assert inBean.equals(outBean);

        outBean.checkNullResources();
    }

    /**
     * Tests marshal local class instance.
     *
     * @throws Exception If test failed.
     */
    public void testMarshallingLocalClassInstance() throws Exception {
        /**
         * Local class.
         */
        class LocalRunnable implements Runnable, Serializable {
            /** {@inheritDoc} */
            @Override public void run() {
                // No-op.
            }
        }

        GridMarshallerTestBean inBean = newTestBean(new LocalRunnable());

        byte[] buf = marshal(inBean);

        GridMarshallerTestBean outBean = unmarshal(buf);

        assert inBean.getObjectField() != null;
        assert outBean.getObjectField() != null;

        assert Runnable.class.isAssignableFrom(inBean.getObjectField().getClass());
        assert Runnable.class.isAssignableFrom(outBean.getObjectField().getClass());

        assert inBean.getObjectField() != outBean.getObjectField();

        assert inBean != outBean;

        assert inBean.equals(outBean);

        outBean.checkNullResources();
    }

    /**
     * Tests marshal nested class instance.
     *
     * @throws Exception If test failed.
     */
    public void testMarshallingNestedClassInstance() throws Exception {
        GridMarshallerTestBean inBean = newTestBean(new NestedClass());

        byte[] buf = marshal(inBean);

        GridMarshallerTestBean outBean = unmarshal(buf);

        assert inBean.getObjectField() != null;
        assert outBean.getObjectField() != null;

        assert inBean.getObjectField().getClass().equals(NestedClass.class);
        assert outBean.getObjectField().getClass().equals(NestedClass.class);

        assert inBean.getObjectField() != outBean.getObjectField();

        assert inBean != outBean;

        assert inBean.equals(outBean);

        outBean.checkNullResources();
    }

    /**
     * Tests marshal static nested class instance.
     *
     * @throws Exception If test failed.
     */
    public void testMarshallingStaticNestedClassInstance() throws Exception {
        GridMarshallerTestBean inBean = newTestBean(new StaticNestedClass());

        byte[] buf = marshal(inBean);

        GridMarshallerTestBean outBean = unmarshal(buf);

        assert inBean.getObjectField() != null;
        assert outBean.getObjectField() != null;

        assert inBean.getObjectField().getClass().equals(StaticNestedClass.class);
        assert outBean.getObjectField().getClass().equals(StaticNestedClass.class);

        assert inBean.getObjectField() != outBean.getObjectField();

        assert inBean != outBean;

        assert inBean.equals(outBean);

        outBean.checkNullResources();
    }

    /**
     * Tests marshal {@code null}.
     *
     * @throws Exception If test failed.
     */
    public void testMarshallingNullObject() throws Exception {
        GridMarshallerTestBean inBean = newTestBean(null);

        byte[] buf = marshal(inBean);

        GridMarshallerTestBean outBean = unmarshal(buf);

        assert inBean.getObjectField() == null;
        assert outBean.getObjectField() == null;

        assert inBean != outBean;

        assert inBean.equals(outBean);

        outBean.checkNullResources();
    }

    /**
     * Tests marshal arrays of primitives.
     *
     * @throws GridException If marshalling failed.
     */
    @SuppressWarnings({"ZeroLengthArrayAllocation"})
    public void testMarshallingArrayOfPrimitives() throws GridException {
        char[] inChars = "vasya".toCharArray();

        char[] outChars = unmarshal(marshal(inChars));

        assertTrue(Arrays.equals(inChars, outChars));

        boolean[][] inBools = new boolean[][]{
            {true}, {}, {true, false, true}
        };

        boolean[][] outBools = unmarshal(marshal(inBools));

        assertEquals(inBools.length, outBools.length);

        for (int i = 0; i < inBools.length; i++)
            assertTrue(Arrays.equals(inBools[i], outBools[i]));

        int[] inInts = new int[] {1,2,3,4,5,6,7};

        int[] outInts = unmarshal(marshal(inInts));

        assertEquals(inInts.length, outInts.length);

        assertTrue(Arrays.equals(inInts, outInts));
    }

    /**
     * Tests marshal classes
     *
     * @throws Exception If test failed.
     */
    public void testExternalClassesMarshalling() throws Exception {
        ClassLoader tstClsLdr = new GridTestClassLoader(
            Collections.singletonMap("org/gridgain/grid/p2p/p2p.properties", "resource=loaded"),
            getClass().getClassLoader(),
            GridP2PTestTask.class.getName(), GridP2PTestJob.class.getName()
            );

        GridComputeTask<?, ?> inTask = (GridComputeTask<?, ?>)tstClsLdr.loadClass(GridP2PTestTask.class.getName()).
            newInstance();

        byte[] buf = marsh.marshal(inTask);

        GridComputeTask<?, ?> outTask = marsh.unmarshal(buf, tstClsLdr);

        assert inTask != outTask;
        assert inTask.getClass().equals(outTask.getClass());
    }

    /**
     * Tests marshal {@link GridKernal} instance.
     *
     * @throws Exception If test failed.
     */
    public void testGridKernalMarshalling() throws Exception {
        GridMarshallerTestBean inBean = newTestBean(grid());

        byte[] buf = marshal(inBean);

        GridMarshallerTestBean outBean = unmarshal(buf);

        assert inBean.getObjectField() != null;
        assert outBean.getObjectField() != null;

        assert inBean.getObjectField().getClass().equals(GridKernal.class);
        assert outBean.getObjectField().getClass().equals(GridKernal.class);

        assert inBean != outBean;

        assert inBean.equals(outBean);

        outBean.checkNullResources();
    }

    /**
     * Tests marshal {@link GridProjection} instance.
     *
     * @throws Exception If test failed.
     */
    public void testSubgridMarshalling() throws Exception {
        final Grid grid = grid();

        GridMarshallerTestBean inBean = newTestBean(grid.forPredicate(new GridPredicate<GridNode>() {
            @Override public boolean apply(GridNode n) {
                return n.id().equals(grid.localNode().id());
            }
        }));

        byte[] buf = marshal(inBean);

        GridMarshallerTestBean outBean = unmarshal(buf);

        assert inBean.getObjectField() != null;
        assert outBean.getObjectField() != null;

        assert inBean.getObjectField().getClass().equals(GridProjectionAdapter.class);
        assert outBean.getObjectField().getClass().equals(GridProjectionAdapter.class);

        assert inBean != outBean;
        assert inBean.equals(outBean);

        outBean.checkNullResources();
    }

    /**
     * Tests marshal {@link GridLogger} instance.
     *
     * @throws Exception If test failed.
     */
    public void testLoggerMarshalling() throws Exception {
        GridMarshallerTestBean inBean = newTestBean(grid().log());

        byte[] buf = marshal(inBean);

        GridMarshallerTestBean outBean = unmarshal(buf);

        assert inBean.getObjectField() != null;
        assert outBean.getObjectField() != null;

        assert GridLogger.class.isAssignableFrom(inBean.getObjectField().getClass());
        assert GridLogger.class.isAssignableFrom(outBean.getObjectField().getClass());

        assert inBean != outBean;
        assert inBean.equals(outBean);

        outBean.checkNullResources();
    }

    /**
     * Tests marshal {@link GridNodeLocalMap} instance.
     *
     * @throws Exception If test failed.
     */
    @SuppressWarnings("unchecked")
    public void testNodeLocalMarshalling() throws Exception {
        GridNodeLocalMap<String, String> loc = grid().nodeLocalMap();

        String key = "test-key";
        String val = "test-val";

        loc.put(key, val);

        GridMarshallerTestBean inBean = newTestBean(loc);

        byte[] buf = marshal(inBean);

        GridMarshallerTestBean outBean = unmarshal(buf);

        assert inBean.getObjectField() != null;
        assert outBean.getObjectField() != null;

        assert inBean.getObjectField().getClass().equals(GridNodeLocalMapImpl.class);
        assert outBean.getObjectField().getClass().equals(GridNodeLocalMapImpl.class);

        assert inBean != outBean;
        assert inBean.equals(outBean);

        outBean.checkNullResources();

        loc = (GridNodeLocalMap<String, String>)outBean.getObjectField();

        assert loc.size() == 1;
        assert val.equals(loc.get(key));
    }

    /**
     * Tests marshal {@link GridExecutorService} instance.
     *
     * @throws Exception If test failed.
     */
    public void testExecutorServiceMarshalling() throws Exception {
        ExecutorService inSrvc = grid().compute().executorService();

        GridMarshallerTestBean inBean = newTestBean(inSrvc);

        byte[] buf = marshal(inBean);

        GridMarshallerTestBean outBean = unmarshal(buf);

        assert inBean.getObjectField() != null;
        assert outBean.getObjectField() != null;

        assert inBean.getObjectField().getClass().equals(GridExecutorService.class);
        assert outBean.getObjectField().getClass().equals(GridExecutorService.class);

        assert inBean != outBean;
        assert inBean.equals(outBean);

        outBean.checkNullResources();

        ExecutorService outSrvc = (ExecutorService)outBean.getObjectField();

        assert inSrvc.isShutdown() == outSrvc.isShutdown();
        assert inSrvc.isTerminated() == outSrvc.isTerminated();
    }

    /**
     * Tests marshal {@link GridKernalContext} instance.
     *
     * @throws Exception If test failed.
     */
    public void testKernalContext() throws Exception {
        GridMarshallerTestBean inBean = newTestBean(GridKernalTestUtils.context(grid()));

        byte[] buf = marshal(inBean);

        GridMarshallerTestBean outBean = unmarshal(buf);

        assert inBean.getObjectField() != null;
        assert outBean.getObjectField() != null;

        assert inBean.getObjectField().getClass().equals(GridKernalContextImpl.class);
        assert outBean.getObjectField().getClass().equals(GridKernalContextImpl.class);

        assert inBean != outBean;
        assert inBean.equals(outBean);

        outBean.checkNullResources();
    }

    /**
     * @throws Exception If failed.
     */
    public void testScheduler() throws Exception {
        GridScheduler scheduler = grid().scheduler();

        GridFuture<?> fut = scheduler.runLocal(new Runnable() {
            @Override public void run() {
                // No-op.
            }
        });

        fut.get();

        GridMarshallerTestBean inBean = newTestBean(scheduler);

        byte[] buf = marshal(inBean);

        GridMarshallerTestBean outBean = unmarshal(buf);

        assert inBean.getObjectField() != null;
        assert outBean.getObjectField() != null;

        assert inBean.getObjectField().getClass().equals(GridSchedulerImpl.class);
        assert outBean.getObjectField().getClass().equals(GridSchedulerImpl.class);

        assert inBean != outBean;
        assert inBean.equals(outBean);

        outBean.checkNullResources();
    }

    /**
     * @throws Exception If failed.
     */
    public void testCompute() throws Exception {
        GridConfiguration cfg = optimize(getConfiguration("g1"));

        try (Grid g1 = G.start(cfg)) {
            GridCompute compute = grid().forNode(g1.localNode()).compute();

            GridFuture<?> fut = compute.run(new Runnable() {
                @Override public void run() {
                    // No-op.
                }
            });

            fut.get();

            GridMarshallerTestBean inBean = newTestBean(compute);

            byte[] buf = marshal(inBean);

            GridMarshallerTestBean outBean = unmarshal(buf);

            assert inBean.getObjectField() != null;
            assert outBean.getObjectField() != null;

            assert inBean.getObjectField().getClass().equals(GridComputeImpl.class);
            assert outBean.getObjectField().getClass().equals(GridComputeImpl.class);

            assert inBean != outBean;
            assert inBean.equals(outBean);

            GridProjection inPrj = compute.projection();
            GridProjection outPrj = ((GridComputeImpl)outBean.getObjectField()).projection();

            assert inPrj.getClass().equals(outPrj.getClass());
            assert F.eqNotOrdered(inPrj.nodes(), outPrj.nodes());

            outBean.checkNullResources();
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testEvents() throws Exception {
        GridConfiguration cfg = optimize(getConfiguration("g1"));

        try (Grid g1 = G.start(cfg)) {
            GridEvents events = grid().forNode(g1.localNode()).events();

            events.localListen(new GridPredicate<GridEvent>() {
                @Override public boolean apply(GridEvent gridEvent) {
                    return true;
                }
            }, EVTS_CACHE);

            grid().cache(null).put(1, 1);

            GridMarshallerTestBean inBean = newTestBean(events);

            byte[] buf = marshal(inBean);

            GridMarshallerTestBean outBean = unmarshal(buf);

            assert inBean.getObjectField() != null;
            assert outBean.getObjectField() != null;

            assert inBean.getObjectField().getClass().equals(GridEventsImpl.class);
            assert outBean.getObjectField().getClass().equals(GridEventsImpl.class);

            assert inBean != outBean;
            assert inBean.equals(outBean);

            GridProjection inPrj = events.projection();
            GridProjection outPrj = ((GridEventsImpl)outBean.getObjectField()).projection();

            assert inPrj.getClass().equals(outPrj.getClass());
            assert F.eqNotOrdered(inPrj.nodes(), outPrj.nodes());

            outBean.checkNullResources();
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testMessaging() throws Exception {
        GridConfiguration cfg = optimize(getConfiguration("g1"));

        try (Grid g1 = G.start(cfg)) {
            GridMessaging messaging = grid().forNode(g1.localNode()).message();

            messaging.send(null, "test");

            GridMarshallerTestBean inBean = newTestBean(messaging);

            byte[] buf = marshal(inBean);

            GridMarshallerTestBean outBean = unmarshal(buf);

            assert inBean.getObjectField() != null;
            assert outBean.getObjectField() != null;

            assert inBean.getObjectField().getClass().equals(GridMessagingImpl.class);
            assert outBean.getObjectField().getClass().equals(GridMessagingImpl.class);

            assert inBean != outBean;
            assert inBean.equals(outBean);

            GridProjection inPrj = messaging.projection();
            GridProjection outPrj = ((GridMessagingImpl)outBean.getObjectField()).projection();

            assert inPrj.getClass().equals(outPrj.getClass());
            assert F.eqNotOrdered(inPrj.nodes(), outPrj.nodes());

            outBean.checkNullResources();
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testServices() throws Exception {
        GridConfiguration cfg = optimize(getConfiguration("g1"));

        try (Grid g1 = G.start(cfg)) {
            GridServices services = grid().forNode(g1.localNode()).services();

            GridFuture<?> fut = services.deployNodeSingleton("test", new DummyService());

            fut.get();

            GridMarshallerTestBean inBean = newTestBean(services);

            byte[] buf = marshal(inBean);

            GridMarshallerTestBean outBean = unmarshal(buf);

            assert inBean.getObjectField() != null;
            assert outBean.getObjectField() != null;

            assert inBean.getObjectField().getClass().equals(GridServicesImpl.class);
            assert outBean.getObjectField().getClass().equals(GridServicesImpl.class);

            assert inBean != outBean;
            assert inBean.equals(outBean);

            GridProjection inPrj = services.projection();
            GridProjection outPrj = ((GridServicesImpl)outBean.getObjectField()).projection();

            assert inPrj.getClass().equals(outPrj.getClass());
            assert F.eqNotOrdered(inPrj.nodes(), outPrj.nodes());

            outBean.checkNullResources();
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testDataStructures() throws Exception {
        GridCacheDataStructures dataStructures = grid().cache(CACHE_NAME).dataStructures();

        GridCacheAtomicLong atomicLong = dataStructures.atomicLong("test", 0, true);

        assert atomicLong != null;

        atomicLong.addAndGet(1);

        GridMarshallerTestBean inBean = newTestBean(dataStructures);

        byte[] buf = marshal(inBean);

        GridMarshallerTestBean outBean = unmarshal(buf);

        assert inBean.getObjectField() != null;
        assert outBean.getObjectField() != null;

        assert inBean.getObjectField().getClass().equals(GridCacheDataStructuresProxy.class);
        assert outBean.getObjectField().getClass().equals(GridCacheDataStructuresProxy.class);

        assert inBean != outBean;
        assert inBean.equals(outBean);

        outBean.checkNullResources();
    }

    /**
     * @throws Exception If failed.
     */
    public void testAffinity() throws Exception {
        GridCache<String, String> cache = grid().cache(CACHE_NAME);

        GridCacheAffinity<String> affinity = cache.affinity();

        cache.putx("tst", "test");

        GridMarshallerTestBean inBean = newTestBean(affinity);

        byte[] buf = marshal(inBean);

        GridMarshallerTestBean outBean = unmarshal(buf);

        assert inBean.getObjectField() != null;
        assert outBean.getObjectField() != null;

        assert inBean.getObjectField().getClass().equals(GridCacheAffinityProxy.class);
        assert outBean.getObjectField().getClass().equals(GridCacheAffinityProxy.class);

        assert inBean != outBean;
        assert inBean.equals(outBean);

        outBean.checkNullResources();
    }

    /**
     * @throws Exception If failed.
     */
    public void testStreamer() throws Exception {
        GridStreamer streamer = grid().streamer(null);

        streamer.addEvent("test");

        GridMarshallerTestBean inBean = newTestBean(streamer);

        byte[] buf = marshal(inBean);

        GridMarshallerTestBean outBean = unmarshal(buf);

        assert inBean.getObjectField() != null;
        assert outBean.getObjectField() != null;

        assert inBean.getObjectField().getClass().equals(GridStreamerImpl.class);
        assert outBean.getObjectField().getClass().equals(GridStreamerImpl.class);

        assert inBean != outBean;
        assert inBean.equals(outBean);

        outBean.checkNullResources();
    }

    /**
     * @throws Exception If failed.
     */
    public void testProduct() throws Exception {
        GridProduct product = grid().product();

        GridMarshallerTestBean inBean = newTestBean(product);

        byte[] buf = marshal(inBean);

        GridMarshallerTestBean outBean = unmarshal(buf);

        assert inBean.getObjectField() != null;
        assert outBean.getObjectField() != null;

        assert inBean.getObjectField().getClass().equals(GridProductImpl.class);
        assert outBean.getObjectField().getClass().equals(GridProductImpl.class);

        assert inBean != outBean;
        assert inBean.equals(outBean);

        outBean.checkNullResources();
    }

    /**
     * @param obj Object field to use.
     * @return New test bean.
     */
    static GridMarshallerTestBean newTestBean(@Nullable Object obj) {
        GridByteArrayList buf = new GridByteArrayList(1);

        buf.add((byte)321);

        StringBuilder str = new StringBuilder(33 * 1024);

        // 31KB as jboss is failing at 32KB due to a bug.
        for (int i = 0; i < 33 * 1024; i++)
            str.append('A');

        return new GridMarshallerTestBean(obj, str.toString(), 123, buf, Integer.class, String.class);
    }

    /**
     * @param bean Object to marshal.
     * @return Byte buffer.
     * @throws GridException Thrown if any exception occurs while marshalling.
     */
    protected static byte[] marshal(Object bean) throws GridException {
        return marsh.marshal(bean);
    }

    /**
     * @param buf Byte buffer to unmarshal.
     * @return Unmarshalled object.
     * @throws GridException Thrown if any exception occurs while unmarshalling.
     */
    @SuppressWarnings({"RedundantTypeArguments"})
    protected static <T> T unmarshal(byte[] buf) throws GridException {
        return marsh.<T>unmarshal(buf, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Nested class.
     */
    @SuppressWarnings({"InnerClassMayBeStatic"})
    private class NestedClass implements Serializable {
        // No-op.
    }

    /**
     * Static nested class.
     */
    private static class StaticNestedClass implements Serializable {
        // No-op.
    }

    /**
     * @throws Exception If failed.
     */
    public void testReadArray() throws Exception {
        byte[] arr = new byte[10];

        for (int i = 0; i < arr.length; i++)
            arr[i] = (byte)i;

        arr[5] = -1;

        ReadArrayTestClass obj = unmarshal(marshal(new ReadArrayTestClass(arr, false)));

        assertTrue(Arrays.equals(arr, obj.arr));
    }

    /**
     * @throws Exception If failed.
     */
    public void testReadFully() throws Exception {
        byte[] arr = new byte[10];

        for (int i = 0; i < arr.length; i++)
            arr[i] = (byte)i;

        arr[5] = -1;

        ReadArrayTestClass obj = unmarshal(marshal(new ReadArrayTestClass(arr, true)));

        assertTrue(Arrays.equals(arr, obj.arr));
    }

    /**
     *
     */
    private static class ReadArrayTestClass implements Externalizable {
        /** */
        private byte[] arr;

        /** */
        private boolean fully;

        /**
         *
         */
        public ReadArrayTestClass() {
            // No-op.
        }

        /**
         * @param arr Array.
         * @param fully Read fully flag.
         */
        private ReadArrayTestClass(byte[] arr, boolean fully) {
            this.arr = arr;
            this.fully = fully;
        }

        /** {@inheritDoc} */
        @Override public void writeExternal(ObjectOutput out) throws IOException {
            out.writeBoolean(fully);
            out.writeInt(arr.length);
            out.write(arr);
        }

        /** {@inheritDoc} */
        @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            fully = in.readBoolean();

            arr = new byte[in.readInt()];

            if (fully)
                in.readFully(arr);
            else
                in.read(arr);
        }
    }
}
