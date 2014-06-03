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

#ifndef GRID_MARSHALLER_ACCESSOR_INCLUDED
#define GRID_MARSHALLER_ACCESSOR_INCLUDED

#include <string>
#include <vector>

#include "gridgain/gridclienttypedef.hpp"

class GridClientUuid;
class GridClientNodeMetricsBean;

class GridClientNodeMarshallerHelper {
public:
    GridClientNodeMarshallerHelper(const GridClientNode& node) : node_(node){}

    /**
     * Sets node ID.
     *
     * @param pNodeId Node ID.
     */
    void setNodeId(const GridClientUuid& pNodeId);

    /**
     * Sets node consistent ID.
     *
     * @param pId Node consistent ID.
     */
    void setConsistentId(const GridClientVariant& pId);

    /**
     * Sets REST TCP server addresses.
     *
     * @param pIntAddrs List of address strings.
     */
    void setTcpAddresses(std::vector < GridClientSocketAddress >& pIntAddrs);

    /**
     * Sets REST HTTP server addresses.
     *
     * @param pExtAddrs List of address strings.
     */
    void setJettyAddresses(std::vector < GridClientSocketAddress >& pExtAddrs);

    /**
     * Sets metrics.
     *
     * @param pMetrics Metrics.
     */
    void setMetrics(const GridClientNodeMetricsBean& pMetrics);

    /**
     * Sets attributes.
     *
     * @param pAttrs Attributes.
     */
    void setAttributes(const TGridClientVariantMap& pAttrs);

    /**
     * Sets configured node caches.
     *
     * @param pCaches std::map where key is cache name and value is cache mode ("LOCAL", "REPLICATED", "PARTITIONED").
     */
    void setCaches(const TGridClientVariantMap& pCaches);

    /**
     * Sets mode for default cache.
     *
     * @param dfltCacheMode Default cache mode.
     */
    void setDefaultCacheMode(const std::string& dfltCacheMode);

    /**
     * Sets the router TCP address.
     *
     * @param routerAddress Router address.
     */
    void setRouterJettyAddress(GridClientSocketAddress& routerAddress);

    /**
     * Sets the router HTTP address.
     *
     * @param routerAddress Router address.
     */
    void setRouterTcpAddress(GridClientSocketAddress& routerAddress);

    /**
     * Sets the number of replicas for this node.
     *
     * @param count Replicas count.
     */
    void setReplicaCount(int count);

private:
    const GridClientNode& node_;
};

#endif // GRID_MARSHALLER_ACCESSOR_INCLUDED
