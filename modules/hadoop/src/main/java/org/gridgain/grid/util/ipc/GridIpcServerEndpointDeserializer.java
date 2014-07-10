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

package org.gridgain.grid.util.ipc;

import org.gridgain.grid.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.ipc.loopback.*;
import org.gridgain.grid.util.ipc.shmem.*;

import java.util.*;

/**
 * Grid GridIpcServerEndpoint configuration deserializer.
 */
public class GridIpcServerEndpointDeserializer {
    /**
     * Deserializes IPC server endpoint config into concrete
     * instance of {@link GridIpcServerEndpoint}.
     *
     * @param endpointCfg Map with properties of the IPC server endpoint config.
     * @return Deserialized instance of {@link GridIpcServerEndpoint}.
     * @throws GridException If any problem with configuration properties setting has happened.
     */
    public static GridIpcServerEndpoint deserialize(Map<String,String> endpointCfg) throws GridException {
        A.notNull(endpointCfg, "endpointCfg");

        String endpointType = endpointCfg.get("type");

        if (endpointType == null)
            throw new GridException("Failed to create server endpoint (type is not specified)");

        switch (endpointType) {
            case "shmem": {
                GridIpcSharedMemoryServerEndpoint endpoint = new GridIpcSharedMemoryServerEndpoint();

                endpoint.setupConfiguration(endpointCfg);

                return endpoint;
            }
            case "tcp": {
                GridIpcServerTcpEndpoint endpoint = new GridIpcServerTcpEndpoint();

                endpoint.setupConfiguration(endpointCfg);

                return endpoint;
            }
            default:
                throw new GridException("Failed to create server endpoint (type is unknown): " + endpointType);
        }
    }
}
