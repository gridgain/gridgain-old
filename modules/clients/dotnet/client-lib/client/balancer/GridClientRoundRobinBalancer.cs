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

namespace GridGain.Client.Balancer {
    using System;
    using System.Threading;
    using System.Collections.ObjectModel;
    using System.Collections.Generic;

    /** <summary>Simple balancer that implements the round-robin balancing.</summary> */
    public class GridClientRoundRobinBalancer : IGridClientLoadBalancer, IGridClientTopologyListener {
        /** <summary>Nodes to share load.</summary> */
        private readonly LinkedList<Guid> nodeQueue = new LinkedList<Guid>();

        /** <inheritdoc /> */
        public IGridClientNode BalancedNode<TNode>(ICollection<TNode> nodes) where TNode : IGridClientNode {
            IDictionary<Guid, IGridClientNode> lookup = new Dictionary<Guid, IGridClientNode>(nodes.Count);

            foreach (IGridClientNode node in nodes)
                lookup.Add(node.Id, node);

            lock (nodeQueue) {
                IGridClientNode balanced = null;


                foreach (Guid nodeId in nodeQueue) {
                    balanced = lookup[nodeId];

                    if (balanced != null) {
                        nodeQueue.Remove(nodeId);

                        break;
                    }
                }

                if (balanced != null) {
                    nodeQueue.AddLast(balanced.Id);

                    return balanced;
                }

                throw new GridClientServerUnreachableException("Failed to get balanced node (topology does not have alive " +
                    "nodes): " + nodes);
            }
        }

        /** <inheritdoc /> */
        public void OnNodeAdded(IGridClientNode node) {
            lock (nodeQueue) {
                nodeQueue.AddFirst(node.Id);
            }
        }

        /** <inheritdoc /> */
        public void OnNodeRemoved(IGridClientNode node) {
            lock (nodeQueue) {
                nodeQueue.Remove(node.Id);
            }
        }
    }
}
