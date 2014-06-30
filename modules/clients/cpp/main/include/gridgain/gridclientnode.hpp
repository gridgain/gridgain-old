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

#ifndef GRID_CLIENT_NODE_HPP_INCLUDED
#define GRID_CLIENT_NODE_HPP_INCLUDED

#include <string>
#include <set>

#include <gridgain/gridconf.hpp>
#include <gridgain/gridclientnodemetricsbean.hpp>
#include <gridgain/gridclientuuid.hpp>
#include <gridgain/gridclienttypedef.hpp>
#include <gridgain/gridsocketaddress.hpp>
#include <gridgain/gridclientprotocol.hpp>

/**
 * Grid client node bean.
 */
class GRIDGAIN_API GridClientNode {
public:
    /** Default constructor. */
    GridClientNode();

    /**
     * Copy constructor.

     * @param other Node instance.
     */
    GridClientNode(const GridClientNode& other);

    /**
     * Assignment operator override.
     *
     * @param rhs Right-hand side of the assignment operator.
     * @return This instance of the class.
     */
    GridClientNode& operator=(const GridClientNode& rhs);

    /** Destructor */
    virtual ~GridClientNode();

    /**
     * Gets node ID.
     *
     * @return Node Id.
     */
    GridClientUuid getNodeId() const;

    /**
     * Gets node consistent ID.
     *
     * @return Node consistent Id.
     */
    GridClientVariant getConsistentId() const;

    /**
     * Gets REST TCP server addresses.
     *
     * @return List of address strings.
     */
    const std::vector<GridClientSocketAddress> & getTcpAddresses() const;

    /**
     * Gets REST HTTP server addresses.
     *
     * @return List of address strings.
     */
    const std::vector<GridClientSocketAddress> & getJettyAddresses() const;

    /**
     * Gets metrics.
     *
     * @return Metrics.
     */
    GridClientNodeMetricsBean getMetrics() const;

    /**
     * Gets attributes.
     *
     * @return Attributes.
     */
    TGridClientVariantMap getAttributes() const;


    /**
     * Gets configured node caches.
     *
     * @return std::map where key is cache name and value is cache mode ("LOCAL", "REPLICATED", "PARTITIONED").
     */
    TGridClientVariantMap getCaches() const;

    /**
     * Gets mode for cache with null name.
     *
     * @return Default cache mode.
     */
    std::string getDefaultCacheMode() const;

    /**
     * Returns a list of available addresses by protocol.
     *
     * @param proto Protocol - TCP or HTTP
     * @return List of host/port pairs.
     */
    const std::vector<GridClientSocketAddress> & availableAddresses(GridClientProtocol proto) const;

    /**
     * Returns the router TCP address.
     *
     * @return Router address (host name and port).
     */
    const GridClientSocketAddress & getRouterTcpAddress() const;

    /**
     * Returns the router Jetty address.
     *
     * @return Router address (host name and port).
     */
    const GridClientSocketAddress & getRouterJettyAddress() const;

    /**
     * Returns the number of replicas for this node.
     *
     * @return Replicas count.
     */
    int getReplicaCount() const;

    /**
     * Returns a string representation of this node, useful for debug
     * and monitoring.
     *
     * @return A string representation.
     */
    std::string toString() const;

private:
    class Impl;
    Impl* pimpl;

    friend class GridClientNodeMarshallerHelper;
};

/**
 * Prints node to stream
 *
 * @param out Stream to output node to.
 * @param n Node.
 */
GRIDGAIN_API std::ostream& operator<<(std::ostream &out, const GridClientNode &n);

/** Client node comparator for set. */
struct ClientNodeComparator {
    bool operator() (const GridClientNode& n1, const GridClientNode& n2) const {
        return n1.getNodeId() < n2.getNodeId();
    }
};

/** Typedef for node set. */
typedef std::set<GridClientNode, ClientNodeComparator> TNodesSet;

#endif
