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

#ifndef GRID_CLIENT_TOPOLOGY_HPP_INCLUDE
#define GRID_CLIENT_TOPOLOGY_HPP_INCLUDE

#include <set>

#include <boost/thread.hpp>

#include "gridgain/gridclientnode.hpp"

class GridClientUuid;

/**
 * Topology holder class.
 */
class GridClientTopology {
public:
    /** Updates current topology with new nodes information. */
    void update(const TNodesSet& pNodes);

    /** Removes some nodes from current topology. */
    void remove(const TNodesSet& pNodes);

    /**
     * Returns current list of nodes in the topology.
     *
     * @return A copy of the topology node cache.
     */
    TNodesSet nodes() const;

    /**
     * Retrieves a node from topology by id.
     *
     * @param uuid Node id.
     * @return Shared pointer to a node.
     */
    const TGridClientNodePtr node(const GridClientUuid& uuid) const;

    /**
     * Empties the topology cache. Next topology event will repopulate the cache with the current grid nodes.
     */
    void reset();

protected:
    mutable boost::shared_mutex mux_;

    /** Nodes. */
    TNodesSet nodes_;
};

#endif
