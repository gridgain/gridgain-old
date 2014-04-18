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
#include "gridgain/impl/utils/gridclientdebug.hpp"

#include "gridgain/impl/gridclienttopology.hpp"

void GridClientTopology::update(const TNodesSet& updatedNodeSet) {
    boost::lock_guard<boost::shared_mutex> lock(mux_);

    nodes_.clear();

    nodes_.insert(updatedNodeSet.begin(), updatedNodeSet.end());
}

void GridClientTopology::remove(const TNodesSet& deletedNodes) {
    boost::lock_guard<boost::shared_mutex> lock(mux_);

    for (auto it = deletedNodes.begin(); it != deletedNodes.end(); ++it)
        nodes_.erase(*it);
}

const TGridClientNodePtr GridClientTopology::node(const GridClientUuid& uuid) const {
    boost::shared_lock<boost::shared_mutex> lock(mux_);

    for (auto it = nodes_.begin(); it != nodes_.end(); ++it) {
        if (uuid.uuid() == (*it).getNodeId().uuid())
            return TGridClientNodePtr(new GridClientNode(*it));
    }

    return TGridClientNodePtr((GridClientNode *) 0);
}

TNodesSet GridClientTopology::nodes() const {
    boost::shared_lock<boost::shared_mutex> lock(mux_);

    return nodes_;
}

void GridClientTopology::reset() {
    boost::lock_guard<boost::shared_mutex> lock(mux_);

    nodes_.clear();
}
