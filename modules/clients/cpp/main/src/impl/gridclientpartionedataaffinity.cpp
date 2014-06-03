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

#include <map>
#include <vector>

#include "gridgain/impl/gridclientpartitionedaffinity.hpp"
#include "gridgain/gridclientnode.hpp"
#include "gridgain/impl/gridclientprojection.hpp"
#include "gridgain/gridclientvariant.hpp"
#include "gridgain/impl/hash/gridclientsimpletypehasheableobject.hpp"
#include "gridgain/impl/hash/gridclientvarianthasheableobject.hpp"

using namespace std;

const char* GridClientPartitionAffinity::DFLT_REPLICA_CNT_ATTR_NAME = "gg:affinity:node:replicas";

GridClientPartitionAffinity::GridClientPartitionAffinity() {
    parts = DFLT_PARTITION_CNT;
    replicas = DFLT_REPLICA_CNT;
    backups = DFLT_BACKUP_CNT;
    attrName = DFLT_REPLICA_CNT_ATTR_NAME;

    hashIdResolver = [] (const GridClientNode& node) {
        return node.getConsistentId();
    };
}

GridClientPartitionAffinity::~GridClientPartitionAffinity() {
}

int GridClientPartitionAffinity::getDefaultReplicas() const {
    return replicas;
}

void GridClientPartitionAffinity::setDefaultReplicas(int pReplicas) {
    replicas = pReplicas;
}

int GridClientPartitionAffinity::getKeyBackups() const {
    return backups;
}

void GridClientPartitionAffinity::setKeyBackups(int pBackups) {
    backups = pBackups;
}

int GridClientPartitionAffinity::getPartitions() const {
    return parts;
}

void GridClientPartitionAffinity::setPartitions(int pParts) {
    parts = pParts;
}

const TGridClientNodePredicatePtr GridClientPartitionAffinity::getBackupFilter() {
    return backupFilter;
}

void GridClientPartitionAffinity::setBackupFilter(const TGridClientNodePredicatePtr pBackupFilter) {
    backupFilter = pBackupFilter;
}

std::string GridClientPartitionAffinity::getReplicaCountAttributeName() const {
    return attrName;
}

void GridClientPartitionAffinity::setReplicaCountAttributeName(const string& pAttrName) {
    attrName = pAttrName;
}

GridHashIdResolver GridClientPartitionAffinity::getHashIdResolver() const {
    return hashIdResolver;
}

void GridClientPartitionAffinity::setHashIdResolver(GridHashIdResolver& pHashIdResolver) {
    hashIdResolver = pHashIdResolver;
}

int GridClientPartitionAffinity::getReplicas(const GridClientNode& n) {
    if (n.getReplicaCount() > 0)
        return n.getReplicaCount();

    TGridClientVariantMap attrs = n.getAttributes();

    if (attrs.count(attrName) > 0)
        return atoi(attrs[attrName].toString().c_str());

    return replicas;
}

void GridClientPartitionAffinity::add(const GridClientNode& node, int replicas) {
    nodeHash.addNode(
        NodeInfo(
            node.getNodeId(),
            std::shared_ptr<GridClientHasheableObject>(new GridClientVariantHasheableObject(hashIdResolver(node)))),
        replicas);

    addedNodes.insert(node.getNodeId());
}

TGridClientNodePtr GridClientPartitionAffinity::getNode(const TNodesSet& nodes, const GridClientHasheableObject& key) {
    set<GridClientUuid> newNodes;

    for (auto iter = nodes.begin(); iter != nodes.end(); ++iter)
        newNodes.insert(iter->getNodeId());

    GridClientUuid nodeId;

    {
        boost::lock_guard<boost::mutex> lock(mux);

        if (addedNodes != newNodes) {
            // Recreate the consistent hash ring.
            addedNodes.clear();
            nodeHash.clear();

            for (auto iter = nodes.begin(); iter != nodes.end(); ++iter) {
                GridClientNode n = *iter;

                add(n, getReplicas(n));
            }
        }

        int32_t part = abs(key.hashCode() % parts);

        std::set<NodeInfo> nInfos;

        for (TNodesSet::const_iterator i = nodes.begin(); i != nodes.end(); i++)
            nInfos.insert(NodeInfo(i->getNodeId(),
                std::shared_ptr<GridClientHasheableObject>(new GridClientVariantHasheableObject(hashIdResolver(*i)))));

        nodeId = nodeHash.node(GridInt32Hasheable(part), nInfos).id();
    }

    return findNode(nodeId, nodes);
}

TGridClientNodePtr GridClientPartitionAffinity::findNode(const GridClientUuid& id, const TNodesSet& nodes) const {
    TNodesSet::const_iterator iter = find_if(nodes.begin(), nodes.end(), [&id] (const GridClientNode& node) {
        return node.getNodeId() == id;
    });

    assert(iter != nodes.end());

    return TGridClientNodePtr(new GridClientNode(*iter));
}

void GridClientPartitionAffinity::checkRemoved(const GridClientNode& node) {
    boost::lock_guard<boost::mutex> lock(mux);

    addedNodes.erase(find_if(addedNodes.begin(), addedNodes.end(), [&node] (const GridClientUuid& id) {
        return id == node.getNodeId();
    }));

    nodeHash.removeNode(NodeInfo(node.getNodeId(),
        std::shared_ptr<GridClientHasheableObject>(new GridClientVariantHasheableObject(hashIdResolver(node)))));
}
