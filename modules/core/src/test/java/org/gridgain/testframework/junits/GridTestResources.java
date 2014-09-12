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

package org.gridgain.testframework.junits;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.processors.resource.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.marshaller.optimized.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.thread.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.config.*;
import org.gridgain.testframework.junits.logger.*;
import org.jetbrains.annotations.*;

import javax.management.*;
import java.io.*;
import java.lang.management.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Test resources for injection.
 */
public class GridTestResources {
    /** */
    private static final GridLogger rootLog = new GridTestLog4jLogger(false);

    /** */
    private final GridLogger log;

    /** Local host. */
    private final String locHost;

    /** */
    private final UUID nodeId;

    /** */
    private GridMarshaller marshaller;

    /** */
    private final MBeanServer jmx;

    /** */
    private final String home;

    /** */
    private ThreadPoolExecutor execSvc;

    /** */
    private GridResourceProcessor rsrcProc;

    /** */
    public GridTestResources() {
        log = rootLog.getLogger(getClass());
        nodeId = UUID.randomUUID();
        jmx = ManagementFactory.getPlatformMBeanServer();
        home = U.getGridGainHome();
        locHost = localHost();

        GridTestKernalContext ctx = new GridTestKernalContext();

        ctx.config().setGridLogger(log);

        rsrcProc = new GridResourceProcessor(ctx);
    }

    /**
     * @param jmx JMX server.
     */
    public GridTestResources(MBeanServer jmx) {
        assert jmx != null;

        this.jmx = jmx;

        log = rootLog.getLogger(getClass());

        nodeId = UUID.randomUUID();
        home = U.getGridGainHome();
        locHost = localHost();

        GridTestKernalContext ctx = new GridTestKernalContext();

        ctx.config().setGridLogger(log);

        rsrcProc = new GridResourceProcessor(ctx);
    }

    /**
     * @param log Logger.
     */
    public GridTestResources(GridLogger log) {
        assert log != null;

        this.log = log.getLogger(getClass());

        nodeId = UUID.randomUUID();
        jmx = ManagementFactory.getPlatformMBeanServer();
        home = U.getGridGainHome();
        locHost = localHost();

        GridTestKernalContext ctx = new GridTestKernalContext();

        ctx.config().setGridLogger(log);

        rsrcProc = new GridResourceProcessor(ctx);
    }

    /**
     * @return Local host.
     */
    @Nullable private String localHost() {
        try {
            return U.getLocalHost().getHostAddress();
        }
        catch (IOException e) {
            System.err.println("Failed to detect local host address.");

            e.printStackTrace();

            return null;
        }
    }

    /**
     * @param prestart Prestart flag.
     */
    public void startThreads(boolean prestart) {
        execSvc = new GridThreadPoolExecutor(nodeId.toString(), 40, 40, Long.MAX_VALUE,
            new LinkedBlockingQueue<Runnable>());

        // Improve concurrency for testing.
        if (prestart)
            execSvc.prestartAllCoreThreads();
    }

    /** */
    public void stopThreads() {
        if (execSvc != null) {
            U.shutdownNow(getClass(), execSvc, log);

            execSvc = null;
        }
    }

    /**
     * @param target Target.
     * @throws GridException If failed.
     */
    public void inject(Object target) throws GridException {
        assert target != null;
        assert getLogger() != null;
        assert getNodeId() != null;
        assert getMBeanServer() != null;
        assert getGridgainHome() != null;

        ExecutorService execSvc = getExecutorService();

        if (execSvc != null)
            rsrcProc.injectBasicResource(target, GridExecutorServiceResource.class, execSvc);

        rsrcProc.injectBasicResource(target, GridLoggerResource.class, getLogger().getLogger(target.getClass()));
        rsrcProc.injectBasicResource(target, GridMarshallerResource.class, getMarshaller());
        rsrcProc.injectBasicResource(target, GridLocalNodeIdResource.class, getNodeId());
        rsrcProc.injectBasicResource(target, GridMBeanServerResource.class, getMBeanServer());
        rsrcProc.injectBasicResource(target, GridHomeResource.class, getGridgainHome());
        rsrcProc.injectBasicResource(target, GridLocalHostResource.class, getLocalHost());
    }

    /**
     * @return Executor service.
     */
    public ExecutorService getExecutorService() {
        return execSvc;
    }

    /**
     * @return GridGain home.
     */
    public String getGridgainHome() {
        return home;
    }

    /**
     * @return MBean server.
     */
    public MBeanServer getMBeanServer() {
        return jmx;
    }

    /**
     * @return Logger for specified class.
     */
    public static GridLogger getLogger(Class<?> cls) {
        return rootLog.getLogger(cls);
    }

    /**
     * @return Logger.
     */
    public GridLogger getLogger() {
        return log;
    }

    /**
     * @return Node ID.
     */
    public UUID getNodeId() {
        return nodeId;
    }

    /**
     * @return Local host.
     */
    public String getLocalHost() {
        return locHost;
    }

    /**
     * @return Marshaller.
     * @throws GridException If failed.
     */
    @SuppressWarnings("unchecked")
    public synchronized GridMarshaller getMarshaller() throws GridException {
        if (marshaller == null) {
            String marshallerName = GridTestProperties.getProperty("marshaller.class");

            if (marshallerName == null)
                marshaller = new GridOptimizedMarshaller();
            else {
                try {
                    Class<? extends GridMarshaller> cls = (Class<? extends GridMarshaller>)Class.forName(marshallerName);

                    marshaller = cls.newInstance();
                }
                catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                    throw new GridException("Failed to create test marshaller [marshaller=" + marshallerName + ']', e);
                }
            }
        }

        return marshaller;
    }
}
