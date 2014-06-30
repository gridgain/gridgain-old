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

#ifndef GRIDTOPOLOGY_REQUEST_COMMAND_HPP_INCLUDED
#define GRIDTOPOLOGY_REQUEST_COMMAND_HPP_INCLUDED

#include <string>

#include "gridgain/impl/cmd/gridclientmessagecommand.hpp"

/**
 * Topology request command.
 */
class GridTopologyRequestCommand : public GridClientMessageCommand {
public:
    /**
     * Getter method for include metrics flag.
     *
     * @return Include metrics flag.
     */
    bool getIncludeMetrics() const {
        return includeMetrics;
    }

    /**
     * Setter method for include metrics flag.
     *
     * @param pIncludeMetrics Include metrics flag.
     */
    void setIncludeMetrics(bool pIncludeMetrics) {
        includeMetrics = pIncludeMetrics;
    }

    /**
     * Getter method for include attributes flag.
     *
     * @return Include node attributes flag.
     */
     bool getIncludeAttributes() const {
        return includeAttrs;
     }

    /**
     * Setter method for include attributes flag.
     *
     * @param pIncludeAttrs Include node attributes flag.
     */
     void setIncludeAttributes(bool pIncludeAttrs) {
        includeAttrs = pIncludeAttrs;
     }

    /**
     * Getter method for node id.
     *
     * @return Node identifier, if specified, empty string otherwise.
     */
     std::string getNodeId() const {
        return nodeId;
     }

    /**
     * Setter method for node id.
     *
     * @param pNodeId Node identifier to lookup.
     */
     void setNodeId(const std::string& nodeId) {
        this->nodeId = nodeId;
     }

    /**
     * Getter method for node id.
     *
     * @return Node ip address if specified, empty string otherwise.
     */
     std::string getNodeIp() const {
        return nodeIp;
     }

    /**
     * Setter method for node id.
     *
     * @param pNodeIp Node ip address to lookup.
     */
     void setNodeIp(const std::string& nodeIp) {
        this->nodeIp = nodeIp;
     }

private:
    /** Id of requested node. */
     std::string nodeId;

    /** IP address of requested node. */
     std::string nodeIp;

    /** Include metrics flag. */
     bool includeMetrics;

    /** Include node attributes flag. */
     bool includeAttrs;
};

#endif
