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

#ifndef GRID_CLIENT_UUID_FILTER_HPP
#define GRID_CLIENT_UUID_FILTER_HPP

#include <set>
#include <string>

#include "gridgain/gridclienttypedef.hpp"
#include "gridgain/gridclientnode.hpp"
#include "gridgain/gridclientvariant.hpp"

/**
 * This filter accepts all nodes.
 */
class GridClientAllPassFilter: public TGridClientNodePredicate {
    /**
     * @return Always returns <tt>true</tt>.
     */
    bool apply(const GridClientNode&) const {
        return true;
    }
};

/**
 * The special filter for STL-filtering operations.
 */
class GridClientNodeUuidFilter: public TGridClientNodePredicate {
public:
    /** Default constructor. */
    GridClientNodeUuidFilter() {
    }

    /**
     * Constructor based on the list of nodes.
     *
     * @param nodes List of nodes.
     */
    GridClientNodeUuidFilter(const TGridClientNodeList& nodes) {
        for (size_t i = 0; i < nodes.size(); ++i)
            nodeIds.insert(nodes.at(i)->getNodeId());
    }

    /**
     * Constructor based on the list UUIDs.
     *
     * @param nodes List of UUIDs.
     */
    GridClientNodeUuidFilter(const std::vector<GridClientUuid>& uuids) {
        for (size_t i = 0; i < uuids.size(); ++i)
            nodeIds.insert(uuids[i].uuid());
    }

    /**
     * Constructor based on a single node.
     *
     * @param node Node.
     */
    GridClientNodeUuidFilter(const GridClientNode& node) {
        nodeIds.insert(node.getNodeId());
    }

    /**
     * Checks if there's a node with such UUID in the list.
     *
     * @param node Node to be checked.
     * @return <tt>true</tt> If this node's UUID exists in the filtering condition.
     */
    bool apply(const GridClientNode& node) const {
        return nodeIds.empty() || nodeIds.find(node.getNodeId()) != nodeIds.end();
    }

private:
    /** Collection of UUIDs. */
    std::set<GridClientUuid> nodeIds;
};

/**
 * This predicate evaluates to true for all nodes that have the given cache configured.
 */
struct GridCacheNameFilter: public TGridClientNodePredicate {
    /**
     * Constructor.
     *
     * @cacheName The name of the cache to evaluate against.
     */
    GridCacheNameFilter(const std::string& cacheName)
            : cacheName_(cacheName) {
    }

    /**
     * @param node The node to evaluate.
     * @return True if the node has the given cache configured.
     */
    bool apply(const GridClientNode& node) const {
        TGridClientVariantMap caches=node.getCaches();

        return caches.find(cacheName_) != caches.end();
    }

    const GridClientVariant cacheName_;
};

/**
 * This predicate evaluates to true for all nodes that do not exist in the given set.
 */
struct GridOneOfUuid: public TGridClientNodePredicate {
    /**
     * Constructor.
     *
     * @cacheName The name of the cache to evaluate against.
     */
    GridOneOfUuid(const std::set<GridClientUuid>& uuidSet)
            : uuidSet_(uuidSet) {
    }

    /**
     * @param node The node to evaluate.
     * @return True if the node is not present in the given set.
     */
    bool apply(const GridClientNode& node) const {
        return uuidSet_.find(node.getNodeId()) == uuidSet_.end();
    }

    const std::set<GridClientUuid>& uuidSet_;
};

#endif
