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
    /**
     * <summary>
     * Listener interface for notifying on nodes joining or leaving remote grid.
     * <para/>
     * Since the topology refresh is performed in background, the listeners will
     * not be notified immediately after the node leaves grid, but the maximum time
     * window between remote grid detects node leaving and client receives topology
     * update is <see cref="IGridClientConfiguration.TopologyRefreshFrequency"/>.</summary>
     */
    public interface IGridClientTopologyListener {
        /**
         * <summary>
         * Callback for new nodes joining the remote grid.</summary>
         *
         * <param name="node">New remote node.</param>
         */
        void OnNodeAdded(IGridClientNode node);

        /**
         * <summary>
         * Callback for nodes leaving the remote grid.</summary>
         *
         * <param name="node">Left node.</param>
         */
        void OnNodeRemoved(IGridClientNode node);
    }
}
