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

package org.gridgain.grid.gridify;

import org.gridgain.grid.compute.gridify.aop.spring.*;
import org.gridgain.testframework.junits.common.*;

/**
 * Spring AOP test.
 */
@GridCommonTest(group="AOP")
public class GridSpringAopSelfTest extends GridAbstractAopTest {
    /** {@inheritDoc} */
    @Override protected Object target() {
        return GridifySpringEnhancer.enhance(new GridTestAopTarget());
    }

    /** {@inheritDoc} */
    @Override public String getTestGridName() {
        return "GridTestAopTargetInterface";
    }

    /** {@inheritDoc} */
    @Override protected Object targetWithUserClassLoader() throws Exception {
        Object res = super.targetWithUserClassLoader();

        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        Thread.currentThread().setContextClassLoader(res.getClass().getClassLoader());

        res = GridifySpringEnhancer.enhance(res);

        Thread.currentThread().setContextClassLoader(cl);

        return res;
    }
}
