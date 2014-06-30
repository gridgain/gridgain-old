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
    using System.Collections.Generic;

    /**
     * <summary>
     * Interface that defines a selection logic of a server node for a particular operation
     * (e.g. task run or cache operation in case of pinned mode).</summary>
     */
    public interface IGridClientLoadBalancer {
        /**
         * <summary>
         * Gets next node for executing client command.</summary>
         * 
         * <param name="nodes">Nodes to pick from.</param>
         * <returns>Next node to pick.</returns>
         * <exception cref="GridGain.Client.GridClientServerUnreachableException">
         *     If none of the nodes given to the balancer can be reached.</exception>
         */
        IGridClientNode BalancedNode<TNode>(ICollection<TNode> nodes) where TNode : IGridClientNode;
    }
}
