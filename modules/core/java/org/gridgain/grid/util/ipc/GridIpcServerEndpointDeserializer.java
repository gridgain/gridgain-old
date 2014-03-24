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

import net.sf.json.*;
import org.gridgain.grid.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.ipc.loopback.*;
import org.gridgain.grid.util.ipc.shmem.*;

/**
 * Grid GridIpcServerEndpoint configuration JSON deserializer.
 */
public class GridIpcServerEndpointDeserializer {
    /**
     * Deserializes JSON-formatted IPC server endpoint config into concrete
     * instance of {@link GridIpcServerEndpoint}.
     *
     * @param endpointCfg JSON-formatted IPC server endpoint config.
     * @return Deserialized instance of {@link GridIpcServerEndpoint}.
     * @throws GridException If any problem with JSON parsing has happened.
     */
    public static GridIpcServerEndpoint deserialize(String endpointCfg) throws GridException {
        A.notNull(endpointCfg, "endpointCfg");

        try {
            JSONObject jsonObj = JSONObject.fromObject(endpointCfg);

            String endpointType = jsonObj.getString("type");

            Class endpointCls;

            switch (endpointType) {
                case "shmem":
                    endpointCls = GridIpcSharedMemoryServerEndpoint.class; break;
                case "tcp":
                    endpointCls = GridIpcServerTcpEndpoint.class; break;
                default:
                    throw new GridException("Failed to create server endpoint (type is unknown): " + endpointType);
            }

            // Remove 'type' entry cause there should not be such field in GridIpcServerEndpoint implementations.
            jsonObj.discard("type");

            return (GridIpcServerEndpoint)JSONObject.toBean(jsonObj, endpointCls);
        }
        catch (JSONException e) {
            throw new GridException("Failed to parse server endpoint.", e);
        }
    }
}
