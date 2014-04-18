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

#include "gridclientapiexample.hpp"

#include <vector>

using namespace std;

GridClientConfiguration clientConfiguration() {
    GridClientConfiguration clientConfig;

    vector<GridClientSocketAddress> servers;

//    To enable communication with GridGain instance by HTTP, not by TCP, uncomment the following lines
//    and comment push_back with TCP.
//    ================================
//    GridClientProtocolConfiguration protoCfg;
//
//    protoCfg.protocol(HTTP);
//
//    clientConfig.setProtocolConfiguration(protoCfg);
//
//    servers.push_back(GridSocketAddress(SERVER_ADDRESS, GridClientProtocolConfiguration::DFLT_HTTP_PORT));

    for (int i = TCP_PORT; i < TCP_PORT + MAX_NODES; i++)
        servers.push_back(GridClientSocketAddress(SERVER_ADDRESS, i));

    clientConfig.servers(servers);

    return clientConfig;
}
