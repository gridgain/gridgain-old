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

package org.gridgain.grid.p2p;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;

import java.io.*;
import java.util.*;

/**
 * Job is used in the GridP2PTestTask.
 */
public class GridP2PTestJob extends GridComputeJobAdapter {
    /** Injected job context. */
    @GridTaskSessionResource
    private GridComputeTaskSession taskSes;

    /** Injected logger. */
    @GridLoggerResource
    private GridLogger log;

    /** Injected grid. */
    @GridLocalNodeIdResource
    private UUID locNodeId;

    /**
     * @param arg is argument of GridP2PTestJob.
     */
    public GridP2PTestJob(Integer arg) {
        super(arg);
    }

    /** {@inheritDoc} */
    @Override public Serializable execute() throws GridException {
        assert taskSes != null;

        ClassLoader ldr = getClass().getClassLoader();

        if (log.isInfoEnabled())
            log.info("Executing job loaded by class loader: " + ldr.getClass().getName());

        if (argument(0) != null && locNodeId.equals(taskSes.getTaskNodeId())) {
            log.error("Remote job is executed on local node.");

            return -1;
        }

        Integer arg = argument(0);
        assert arg != null;

        // Check resource loading.
        String rsrc = "org/gridgain/grid/p2p/p2p.properties";

        InputStream in = ldr.getResourceAsStream(rsrc);

        if (in == null) {
            log.error("ResourceAsStream could not be loaded: " + rsrc);

            return -2;
        }

        // Test property file load.
        byte [] bytes = new byte[20];

        try {
            in.read(bytes);
        }
        catch (IOException e) {
            log.error("Failed to read from resource stream.", e);

            return -3;
        }

        String rsrcVal = new String(bytes).trim();

        if (log.isInfoEnabled())
            log.info("Remote resource content is : " + rsrcVal);

        if (!"resource=loaded".equals(rsrcVal)) {
            log.error("Invalid loaded resource value: " + rsrcVal);

            return -4;
        }

        /* Check class properties GG-1314. */
        Class cls;

        try {
            cls = Class.forName("java.math.BigInteger");
        }
        catch (ClassNotFoundException e) {
            log.error("Mandatory class can't be loaded: [java.math.BigInteger]",e);

            return -5;
        }

        if (cls != null && cls.getPackage() == null) {
            log.error("Wrong package within class: " + cls);

            return -6;
        }

        if (getClass().getPackage() == null) {
            log.error("Wrong package within class: " + getClass());

            return -6;
        }

        return arg * 10;
    }
}
