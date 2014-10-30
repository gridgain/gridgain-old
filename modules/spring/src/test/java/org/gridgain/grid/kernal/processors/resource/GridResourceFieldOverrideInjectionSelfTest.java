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

package org.gridgain.grid.kernal.processors.resource;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.testframework.*;
import org.gridgain.testframework.junits.common.*;
import org.springframework.beans.factory.support.*;
import org.springframework.context.support.*;

import java.io.*;
import java.util.*;

import static org.gridgain.grid.kernal.processors.resource.GridAbstractUserResource.*;
import static org.gridgain.grid.kernal.processors.resource.GridResourceTestUtils.*;

/**
 *
 */
@SuppressWarnings({"InnerClassFieldHidesOuterClassField"})
@GridCommonTest(group = "Resource Self")
public class GridResourceFieldOverrideInjectionSelfTest extends GridCommonAbstractTest {
    /** */
    private static final String SPRING_BEAN_RSRC_NAME = "test-bean";

    /** */
    public GridResourceFieldOverrideInjectionSelfTest() {
        super(/*start grid*/false);
    }

    /**
     * @throws Exception in the case of failures.
     */
    public void testFieldResourceOverride() throws Exception {
        Grid grid1 = null;
        Grid grid2 = null;

        try {
            GenericApplicationContext ctx = new GenericApplicationContext();

            RootBeanDefinition bf = new RootBeanDefinition();

            bf.setBeanClass(UserSpringBean.class);

            ctx.registerBeanDefinition(SPRING_BEAN_RSRC_NAME, bf);

            ctx.refresh();

            grid1 = startGrid(1, new GridSpringResourceContextImpl(ctx));
            grid2 = startGrid(2, new GridSpringResourceContextImpl(ctx));

            grid1.compute().execute(ResourceOverrideTask.class, null).get();

            checkUsageCount(createClss, UserResource.class, 2);
            checkUsageCount(deployClss, UserResource.class, 2);
        }
        finally {
            GridTestUtils.close(grid1, log());
            GridTestUtils.close(grid2, log());
        }

        checkUsageCount(undeployClss, UserResource.class, 2);
    }

    /**
     *
     */
    @SuppressWarnings("PublicInnerClass")
    public static class UserResource extends GridAbstractUserResource {
        // No-op.
    }

    /**
     *
     */
    private static class UserSpringBean {
        // No-op.
    }

    /** */
    private static class ResourceOverrideTask extends GridComputeTaskSplitAdapter<Object, Object> {
        /** */
        @GridLoggerResource
        private GridLogger log;

        /** */
        @GridUserResource
        private transient UserResource rsrc;

        /** */
        @GridSpringResource(resourceName = SPRING_BEAN_RSRC_NAME)
        private transient UserSpringBean springBean;

        /** */
        @GridTaskSessionResource
        private GridComputeTaskSession ses;

        /** */
        @GridJobContextResource
        private GridComputeJobContext jobCtx;

        /** {@inheritDoc} */
        @Override protected Collection<? extends GridComputeJob> split(int gridSize, Object arg) throws GridException {
            assert log != null;
            assert rsrc != null;
            assert springBean != null;

            // Job context is job resource, not task resource.
            assert jobCtx == null;

            log.info("Injected logger into task: " + log);
            log.info("Injected shared resource into task: " + rsrc);
            log.info("Injected session into task: " + ses);
            log.info("Injected spring bean into task: " + springBean);

            Collection<GridComputeJobAdapter> jobs = new ArrayList<>(gridSize);

            for (int i = 0; i < gridSize; i++) {
                jobs.add(new GridComputeJobAdapter() {
                    /** */
                    @GridUserResource
                    private transient UserResource rsrc;

                    /** */
                    @GridLoggerResource
                    private GridLogger log;

                    /** */
                    @GridTaskSessionResource
                    private GridComputeTaskSession ses;

                    /** */
                    @GridJobContextResource
                    private GridComputeJobContext jobCtx;

                    /** */
                    @GridSpringResource(resourceName = SPRING_BEAN_RSRC_NAME)
                    private transient UserSpringBean jobSpringBean;

                    /** {@inheritDoc} */
                    @SuppressWarnings({"ObjectEquality"})
                    @Override public Serializable execute() {
                        assert log != null;
                        assert rsrc != null;
                        assert jobSpringBean != null;

                        assert ResourceOverrideTask.this.log != null;
                        assert ResourceOverrideTask.this.rsrc != null;
                        //Job context is never setup on the task.
                        assert ResourceOverrideTask.this.jobCtx == null;

                        assert springBean != null;

                        assert rsrc == ResourceOverrideTask.this.rsrc;
                        assert ses == ResourceOverrideTask.this.ses;
                        assert jobCtx != null;
                        assert jobSpringBean == springBean;

                        log.info("Injected logger into job: " + log);
                        log.info("Injected shared resource into job: " + rsrc);
                        log.info("Injected session into job: " + ses);
                        log.info("Injected spring bean into job: " + jobSpringBean);

                        return null;
                    }
                });
            }

            return jobs;
        }

        /** {@inheritDoc} */
        @Override public Object reduce(List<GridComputeJobResult> results) throws GridException {
            assert log != null;
            assert rsrc != null;

            // Job context is job resource, not task resource.
            assert jobCtx == null;

            // Nothing to reduce.
            return null;
        }
    }
}
