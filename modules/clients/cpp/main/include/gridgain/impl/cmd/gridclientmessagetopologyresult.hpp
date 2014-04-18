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

#ifndef GRID_CLEINT_MESSAGE_TOPOLOGY_RESULT_HPP_INCLUDED
#define GRID_CLEINT_MESSAGE_TOPOLOGY_RESULT_HPP_INCLUDED

#include "gridgain/impl/cmd/gridclientmessageresult.hpp"
#include "gridgain/gridclientnode.hpp"

/** Type definition for nodes list. */
typedef std::vector<GridClientNode> TNodesList;

/**
 * Topology result message.
 */
class GridClientMessageTopologyResult : public GridClientMessageResult {
private:
    /** List of nodes in topology. */
    TNodesList nodes_;
public:
    /**
     * Getter method for the list of nodes.
     *
     * @return list of nodes in the topology.
     */
    const TNodesList getNodes() const {
        return nodes_;
    }

    /**
     * Setter method for the list of nodes.
     *
     * @param nodes Collection of nodes - new value for the nodes in topology.
     */
    void setNodes(const TNodesList& nodes) {
        nodes_ = nodes;
    }
};
#endif
