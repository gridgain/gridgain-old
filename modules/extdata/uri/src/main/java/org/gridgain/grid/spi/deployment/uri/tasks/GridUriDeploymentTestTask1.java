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

package org.gridgain.grid.spi.deployment.uri.tasks;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.springframework.beans.factory.xml.*;
import org.springframework.core.io.*;

import java.util.*;

/**
 * URI deployment test task which loads Spring bean definitions from spring1.xml configuration file.
 */
public class GridUriDeploymentTestTask1 extends GridComputeTaskSplitAdapter<Object, Object> {
    /** */
    @SuppressWarnings({"unchecked", "TypeMayBeWeakened"})
    public GridUriDeploymentTestTask1() {
        XmlBeanFactory factory = new XmlBeanFactory(new ClassPathResource("spring1.xml", getClass().getClassLoader()));

        factory.setBeanClassLoader(getClass().getClassLoader());

        Map map = (Map)factory.getBean("task.cfg");

        System.out.println("Loaded data from spring1.xml [map=" + map + ']');

        assert map != null;

        GridUriDeploymentDependency1 depend = new GridUriDeploymentDependency1();

        System.out.println("GridUriDeploymentTestTask1 dependency resolved [msg=" + depend.getMessage() + ']');
    }

    /**
     * {@inheritDoc}
     */
    @Override public Collection<? extends GridComputeJob> split(int gridSize, Object arg) throws GridException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Object reduce(List<GridComputeJobResult> results) throws GridException {
        return null;
    }
}
