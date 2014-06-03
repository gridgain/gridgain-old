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

package org.gridgain.grid.kernal;

import org.gridgain.grid.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;

/**
 * Ensures that system properties required by Visor are always passed to node attributes.
 */
public class GridNodeVisorAttributesSelfTest extends GridCommonAbstractTest {
    /** System properties required by Visor. */
    private static final String[] SYSTEM_PROPS = new String[] {
        "java.version",
        "java.vm.name",
        "os.arch",
        "os.name",
        "os.version"
    };

    /** GridGain-specific properties required by Visor. */
    private static final String[] GG_PROPS = new String[] {
        "org.gridgain.jvm.pid"
    };

    /** System and environment properties to include. */
    private String[] inclProps;

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        cfg.setIncludeProperties(inclProps);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();
    }

    /**
     * Start grid node and ensure that Visor-related node attributes are set properly.
     *
     * @throws Exception If grid start failed.
     */
    private void startGridAndCheck() throws Exception {
        Grid g = startGrid();

        Map<String, Object> attrs = g.localNode().attributes();

        for (String prop : SYSTEM_PROPS) {
            assert attrs.containsKey(prop);

            assertEquals(System.getProperty(prop), attrs.get(prop));
        }

        for (String prop : GG_PROPS)
            assert attrs.containsKey(prop);
    }

    /**
     * Test with 'includeProperties' configuration parameter set to {@code null}.
     *
     * @throws Exception If failed.
     */
    public void testIncludeNull() throws Exception {
        inclProps = null;

        startGridAndCheck();
    }

    /**
     * Test with 'includeProperties' configuration parameter set to empty array.
     *
     * @throws Exception If failed.
     */
    @SuppressWarnings("ZeroLengthArrayAllocation")
    public void testIncludeEmpty() throws Exception {
        inclProps = new String[] {};

        startGridAndCheck();
    }

    /**
     * Test with 'includeProperties' configuration parameter set to array with some values.
     *
     * @throws Exception If failed.
     */
    public void testIncludeNonEmpty() throws Exception {
        inclProps = new String[] {"prop1", "prop2"};

        startGridAndCheck();
    }
}
