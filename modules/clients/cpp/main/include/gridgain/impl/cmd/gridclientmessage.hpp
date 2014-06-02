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

#ifndef GRID_CLIENT_MESSAGE_HPP_INCLUDED
#define GRID_CLIENT_MESSAGE_HPP_INCLUDED

#include <cstdlib>
#include <cstdint>

#include <string>
#include <iostream>

#include "gridgain/gridclientuuid.hpp"

/**
 * Generic message.
 */
class GridClientMessage {
public:
    GridClientMessage(): reqId(1) {
    }

    /** Virtual destructor. */
    virtual ~GridClientMessage() {};

    /**
     * This method is used to match request and response messages.
     *
     * @return request ID.
     */
    int64_t getRequestId() const { return reqId; }

    /**
     * Sets request id for outgoing packets.
     *
     * @param reqId request ID.
     */
    void setRequestId(int64_t pReqId) { reqId = pReqId; }

    /**
     * Gets client identifier from which this request comes.
     *
     * @return Client identifier.
     */
    GridClientUuid getClientId() const { return clientId; }

    /**
     * Sets client identifier from which this request comes.
     *
     * @param id Client identifier.
     */
    void setClientId(const GridClientUuid& id) {
        clientId = id;
    }

    /**
     * Sets client session token.
     *
     * @return Session token.
     */
    std::string sessionToken() const {
        return sessTok;
    }

    /**
     * Gets client session token.
     *
     * @param pSessTok Session token.
     */
    void sessionToken(const std::string& pSessTok) {
        sessTok = pSessTok;
    }

    /**
     * Gets the client destination id.
     *
     * @return destination id.
     */
    GridClientUuid getDestinationId() const {
        return destinationId;
    }

    /**
     * Sets the client destination id.
     *
     * @param destinationId Destination id.
     */
    void setDestinationId(const GridClientUuid& destinationId) {
        this->destinationId = destinationId;
    }

    /**
     * Generate the new id for the request.
     *
     * @return newId - int - The new generated value for the request.
     */
    static int generateNewId() {
         return rand();
    }
private:
    /** Message client id. */
    GridClientUuid clientId;

    /** Message sessio token. */
    std::string sessTok;

    /** Message request id. */
    int64_t reqId;

    /** Message destination id. */
    GridClientUuid destinationId;
};

#endif
