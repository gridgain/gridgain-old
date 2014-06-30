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

package org.gridgain.grid.kernal.processors.ggfs;

import org.gridgain.grid.util.typedef.internal.*;

/**
 * Tests for IPC endpoint configured with TCP.
 */
public class GridGgfsIpcEndpointTcpSelfTest extends GridGgfsIpcEndpointAbstractSelfTest {
    /**
     * Constructor.
     */
    public GridGgfsIpcEndpointTcpSelfTest() {
        super(true);
    }

    /** {@inheritDoc} */
    @Override public void testEndpointEnabledEndpointSetTypeNotSetPortSet() throws Exception {
        if (U.isWindows())
            super.testEndpointEnabledEndpointSetTypeNotSetPortSet();
    }

    /** {@inheritDoc} */
    @Override public void testEndpointEnabledEndpointSetTypeNotSetPortNotSet() throws Exception {
        if (U.isWindows())
            super.testEndpointEnabledEndpointSetTypeNotSetPortNotSet();
    }

    /** {@inheritDoc} */
    @Override public void testEndpointEnabledEndpointNotSetTypeSetPortSet() throws Exception {
        if (U.isWindows())
            super.testEndpointEnabledEndpointNotSetTypeSetPortSet();
    }

    /** {@inheritDoc} */
    @Override public void testEndpointEnabledEndpointNotSetTypeSetPortNotSet() throws Exception {
        if (U.isWindows())
            super.testEndpointEnabledEndpointNotSetTypeSetPortNotSet();
    }

    /**
     * Test the following use case:
     * - endpoint is enabled;
     * - endpoint configuration is set;
     * - file system endpoint type is set;
     * - file system endpoint host is set;
     * - file system endpoint port is set.
     *
     * @throws Exception If failed.
     */
    public void testEndpointEnabledEndpointSetTypeSetPortSetHostSet() throws Exception {
        check(true, true, true, true, true);
    }

    /**
     * Test the following use case:
     * - endpoint is enabled;
     * - endpoint configuration is set;
     * - file system endpoint type is set;
     * - file system endpoint host is set;
     * - file system endpoint port is not set.
     *
     * @throws Exception If failed.
     */
    public void testEndpointEnabledEndpointSetTypeSetPortNotSetHostSet() throws Exception {
        check(true, true, true, true, false);
    }

    /**
     * Test the following use case:
     * - endpoint is enabled;
     * - endpoint configuration is set;
     * - file system endpoint type is not set;
     * - file system endpoint host is set;
     * - file system endpoint port is set.
     *
     * @throws Exception If failed.
     */
    public void testEndpointEnabledEndpointSetTypeNotSetPortSetHostSet() throws Exception {
        if (U.isWindows())
            check(true, true, false, true, true);
    }

    /**
     * Test the following use case:
     * - endpoint is enabled;
     * - endpoint configuration is set;
     * - file system endpoint type is not set;
     * - file system endpoint host is set;
     * - file system endpoint port is not set.
     *
     * @throws Exception If failed.
     */
    public void testEndpointEnabledEndpointSetTypeNotSetPortNotSetHostSet() throws Exception {
        if (U.isWindows())
            check(true, true, false, true, false);
    }

    /**
     * Test the following use case:
     * - endpoint is enabled;
     * - endpoint configuration is not set;
     * - file system endpoint type is set;
     * - file system endpoint host is set;
     * - file system endpoint port is set.
     *
     * @throws Exception If failed.
     */
    public void testEndpointEnabledEndpointNotSetTypeSetPortSetHostSet() throws Exception {
        if (U.isWindows())
            check(true, false, true, true, true);
    }

    /**
     * Test the following use case:
     * - endpoint is enabled;
     * - endpoint configuration is not set;
     * - file system endpoint type is set;
     * - file system endpoint host is set;
     * - file system endpoint port is not set.
     *
     * @throws Exception If failed.
     */
    public void testEndpointEnabledEndpointNotSetTypeSetPortNotSetHostSet() throws Exception {
        if (U.isWindows())
            check(true, false, true, true, false);
    }

    /**
     * Test the following use case:
     * - endpoint is enabled;
     * - endpoint configuration is not set;
     * - file system endpoint type is not set;
     * - file system endpoint host is set;
     * - file system endpoint port is set.
     *
     * @throws Exception If failed.
     */
    public void testEndpointEnabledEndpointNotSetTypeNotSetPortSetHostSet() throws Exception {
        check(true, false, false, true, true);
    }

    /**
     * Test the following use case:
     * - endpoint is enabled;
     * - endpoint configuration is not set;
     * - file system endpoint type is not set;
     * - file system endpoint host is set;
     * - file system endpoint port is not set.
     *
     * @throws Exception If failed.
     */
    public void testEndpointEnabledEndpointNotSetTypeNotSetPortNotSetHostSet() throws Exception {
        check(true, false, false, true, false);
    }
}
