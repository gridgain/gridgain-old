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

#ifndef GRID_CLIENT_MESSAGE_PROJECTION_CLOSURE_HPP_INCLUDED
#define GRID_CLIENT_MESSAGE_PROJECTION_CLOSURE_HPP_INCLUDED

#include <string>

#include "gridgain/impl/cmd/gridclientmessage.hpp"
#include "gridgain/impl/projectionclosure/gridclientprojectionclosure.hpp"
#include "gridgain/impl/cmd/gridclientmessagecommand.hpp"

/**
 * Base class for message closures.
 */
class ClientMessageProjectionClosure : public ClientProjectionClosure {
public:
    /**
     * Generic public constructor.
     *
     * @param pClientId Client id.
     */
    ClientMessageProjectionClosure(std::string pClientId) :
        clientId(pClientId) {};

    /**
     * Generic public constructor.
     *
     * @param clientId Client id as uuid.
     */
    ClientMessageProjectionClosure(GridClientUuid & clientId) :
        clientId(clientId) {};

    /**
     * Fills generic message command fields.
     *
     * @param cmd Message command.
     */
    void fillRequestHeader(GridClientMessageCommand& cmd, TGridClientNodePtr node) {
        int requestId = cmd.generateNewId();

        cmd.setRequestId(requestId);
        cmd.setClientId(clientId);
        cmd.setDestinationId(node->getNodeId());
    }

private:
    /** Client id. */
    GridClientUuid clientId;
};

#endif
