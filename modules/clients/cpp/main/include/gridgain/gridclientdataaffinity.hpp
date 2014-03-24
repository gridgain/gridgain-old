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

#ifndef GRIDCLIENTDATAAFFINITY_HPP_INCLUDED
#define GRIDCLIENTDATAAFFINITY_HPP_INCLUDED

#include <gridgain/gridclientuuid.hpp>
#include <gridgain/gridconf.hpp>
#include <gridgain/gridclientnode.hpp>

class GridClientHasheableObject;

/**
 * Interface that will determine which node should be connected by the client when
 * operation on a key is requested.
 * <p>
 * If implementation of data affinity implements {@link GridClientTopologyListener} interface as well,
 * then affinity will be added to topology listeners on client start before firs connection is established
 * and will be removed after last connection is closed.
 */
class GRIDGAIN_API GridClientDataAffinity {
public:
    /** Destructor. */
    virtual ~GridClientDataAffinity() {};

     /**
      * Gets affinity node for a key for the given topology snapshot. In case of replicated cache, all returned
      * nodes are updated in the same manner. In case of partitioned cache, the returned
      * list should contain only the primary and back up nodes with primary node being
      * always first.
      *
      * @param nodes The current topology snapshot.
      * @param key Key to get affinity for.
      *
      * @return Affinity nodes for the given partition.
      */
      virtual TGridClientNodePtr getNode(const TNodesSet& nodes, const GridClientHasheableObject& key) = 0;

};

#endif // GRIDCLIENTDATAAFFINITY_HPP_INCLUDED
