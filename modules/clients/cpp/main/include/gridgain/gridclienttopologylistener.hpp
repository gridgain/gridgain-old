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

#ifndef GRIDCLIENTTOPOLOGYLISTENER_HPP_INCLUDED
#define GRIDCLIENTTOPOLOGYLISTENER_HPP_INCLUDED

#include <gridgain/gridconf.hpp>

class GridClientNode;

/**
 * Listener interface for notifying on nodes joining or leaving remote grid.
 * <p>
 * Since the topology refresh is performed in background, the listeners will not be notified
 * immediately after the node leaves grid. The maximum time window between the remote grid detects
 * topology change and client receives topology update is {@link GridClientConfiguration#getTopologyRefreshFrequency()}.
 */
class GRIDGAIN_API GridClientTopologyListener {
public:
    /** Virtual destructor. */
    virtual ~GridClientTopologyListener() {}
    /**
     * Callback for new nodes joining the remote grid.
     *
     * @param node New remote node.
     */
    virtual void onNodeAdded(const GridClientNode& node) = 0;

    /**
     * Callback for nodes leaving the remote grid.
     *
     * @param node Left node.
     */
    virtual void onNodeRemoved(const GridClientNode& node) = 0;
};

#endif // GRIDCLIENTTOPOLOGYLISTENER_HPP_INCLUDED
