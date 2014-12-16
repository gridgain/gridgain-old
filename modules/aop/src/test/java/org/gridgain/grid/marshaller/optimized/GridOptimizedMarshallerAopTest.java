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

package org.gridgain.grid.marshaller.optimized;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.gridify.*;
import org.gridgain.grid.events.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.junits.common.*;

import java.util.concurrent.atomic.*;

import static org.gridgain.grid.events.GridEventType.*;

/**
 * Test use GridOptimizedMarshaller and AspectJ AOP.
 *
 * The following configuration needs to be applied to enable AspectJ byte code
 * weaving.
 * <ul>
 * <li>
 *      JVM configuration should include:
 *      <tt>-javaagent:[GRIDGAIN_HOME]/libs/aspectjweaver-1.7.2.jar</tt>
 * </li>
 * <li>
 *      Classpath should contain the <tt>[GRIDGAIN_HOME]/modules/tests/config/aop/aspectj</tt> folder.
 * </li>
 * </ul>
 */
public class GridOptimizedMarshallerAopTest extends GridCommonAbstractTest {
    /** */
    private static final AtomicInteger cntr = new AtomicInteger();

    /**
     * Constructs a test.
     */
    public GridOptimizedMarshallerAopTest() {
        super(false /* start grid. */);
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        GridConfiguration cfg = new GridConfiguration();

        cfg.setMarshaller(new GridOptimizedMarshaller());

        G.start(cfg);

        assert G.allGrids().size() == 1;
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        stopAllGrids();

        assert G.allGrids().isEmpty();
    }

    /**
     * JUnit.
     *
     * @throws Exception If failed.
     */
    public void testUp() throws Exception {
        G.grid().events().localListen(new GridPredicate<GridEvent>() {
            @Override public boolean apply(GridEvent evt) {
                cntr.incrementAndGet();

                return true;
            }
        }, EVT_TASK_FINISHED);

        gridify1();

        assertEquals("Method gridify() wasn't executed on grid.", 1, cntr.get());
    }

    /**
     * Method grid-enabled with {@link Gridify} annotation.
     * <p>
     * Note that default {@code Gridify} configuration is used, so this method
     * will be executed on remote node with the same argument.
     */
    @Gridify
    private void gridify1() {
        X.println("Executes on grid");
    }
}
