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

namespace GridGain.Client {
    using System;
    using System.Collections.Generic;

    /**
     * <summary>
     * Interface that will determine which node should be connected by the client when
     * operation on a key is requested.
     * <para/>
     * If implementation of data affinity implements <see cref="IGridClientTopologyListener"/> interface as well,
     * then affinity will be added to topology listeners on client start before firs connection is established
     * and will be removed after last connection is closed.</summary>
     */
    public interface IGridClientDataAffinity {
        /**
         * <summary>
         * Gets affinity nodes for a key. In case of replicated cache, all returned
         * nodes are updated in the same manner. In case of partitioned cache, the returned
         * list should contain only the primary and back up nodes with primary node being
         * always first.</summary>
         *
         * <param name="key">Key to get affinity for.</param>
         * <param name="nodes">Nodes to choose from.</param>
         * <returns>Affinity nodes for the given partition.</returns>
         */
        T Node<T>(Object key, ICollection<T> nodes) where T : IGridClientNode;
    }
}
