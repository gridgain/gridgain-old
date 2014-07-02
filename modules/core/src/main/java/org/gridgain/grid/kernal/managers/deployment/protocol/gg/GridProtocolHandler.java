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

package org.gridgain.grid.kernal.managers.deployment.protocol.gg;

import org.gridgain.grid.kernal.managers.deployment.*;
import java.io.*;
import java.net.*;

/**
 * Custom stream protocol handler implementation.
 */
public class GridProtocolHandler extends URLStreamHandler {
    /** Deployment manager. */
    private static GridDeploymentManager mgr;

    /**
     * Registers deployment manager.
     * 
     * @param mgr Deployment manager.
     */
    public static void registerDeploymentManager(GridDeploymentManager mgr) {
        assert mgr != null;

        GridProtocolHandler.mgr = mgr;
    }

    /**
     * Deregisters deployment manager.
     */
    public static void deregisterDeploymentManager() {
         mgr = null;
    }

    /** {@inheritDoc} */
    @Override protected URLConnection openConnection(URL url) throws IOException {
        return new GridUrlConnection(url, mgr);
    }
}
