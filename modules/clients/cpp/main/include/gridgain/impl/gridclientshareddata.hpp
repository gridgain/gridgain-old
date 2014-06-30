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

#ifndef GRID_CLEINT_SHARED_DATA_HPP_INCLUDED
#define GRID_CLEINT_SHARED_DATA_HPP_INCLUDED

#include <cassert>

#include "gridgain/gridclienttypedef.hpp"
#include "gridgain/gridclientprotocolconfiguration.hpp"
#include "gridgain/impl/gridclienttopology.hpp"
#include "gridgain/gridclientconfiguration.hpp"
#include "gridgain/loadbalancer/gridclientloadbalancer.hpp"
#include "gridgain/impl/cmd/gridclientcommandexecutorprivate.hpp"

class GridClientCommandExecutor;

typedef std::shared_ptr<GridClientTopology> TGridClientTopologyPtr;
typedef std::shared_ptr<GridClientCommandExecutor> TGridClientCommandExecutorPtr;

/**
 * Common client data.
 */
class GridClientSharedData {
public:
    /** Public constructor.
     *
     * @param pClientId Client id.
     * @param cfg Grid client configuration.
     * @param exec Command executor.
     */
    GridClientSharedData(const GridClientUuid& pClientId, const GridClientConfiguration& cfg,
            std::shared_ptr<GridClientCommandExecutorPrivate> exec) :
            clientId(pClientId), top(new GridClientTopology()), clientCfg(cfg) {
        assert(exec.get() != NULL);

        executor_ = exec;
    }

    /** Public destructor. */
    virtual ~GridClientSharedData() {}

    /**
     * Returns the unique id of the client.
     *
     * @return Client UUID in string form.
     */
    std::string clientUniqueId() const {
        return clientId.uuid();
    }

    /**
     * Returns the unique id of the client.
     *
     * @return Client UUID in GridClientUuid form.
     */
    GridClientUuid & clientUniqueUuid() {
        return clientId;
    }

    /**
     * Method for accessing client UUID.
     *
     * @return Client UUID.
     */
    GridClientUuid clientUuid() const {
        return clientId;
    }

    /**
     * Returns the actual version of the topology.
     *
     * @return Client topology.
     */
    TGridClientTopologyPtr topology() const {
        return top;
    }

    /**
     * Returns the reference to the command executor - class which allows to execute any client commands
     * based on the configured communication layer.
     *
     * @return Actual executor.
     */
    std::shared_ptr<GridClientCommandExecutorPrivate> executor() const {
        return executor_;
    }

    /**
     * Method for retrieving current load balancer.
     *
     * @return Actual load balancer.
     */
    const TGridClientLoadBalancerPtr loadBalancer() const {
        return clientCfg.loadBalancer();
    }

    /**
     * Method for retrieving current protocol configuration.
     *
     * @return Actual protocol configuration.
     */
    const GridClientProtocolConfiguration protocolCfg() const {
        return clientCfg.protocolConfiguration();
    }

    /**
     * Return protocol currently used.
     *
     * @return Current protocol.
     */
    GridClientProtocol protocol() const {
        return protocolCfg().protocol();
    }

    /**
     * Get the list of servers, specified by client for initial connection.
     *
     * @return List of host/port pairs.
     */
    std::vector<GridClientSocketAddress> servers() const {
        return clientCfg.servers();
    }

    /**
     * Returns a constant reference to current client configuration.
     */
    const GridClientConfiguration& clientConfiguration() const {
        return clientCfg;
    }

protected:
    /** Unique Id of the client */
    GridClientUuid clientId;

    /** Actual version of the topology. */
    TGridClientTopologyPtr top;

    /** Configuration of the client */
    GridClientConfiguration clientCfg;

    /** The main interface for execution of commands via the communication layer. **/
    std::shared_ptr<GridClientCommandExecutorPrivate> executor_;
};

#endif
