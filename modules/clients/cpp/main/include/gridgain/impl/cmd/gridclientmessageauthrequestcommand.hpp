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

#ifndef GRIDLOG_AUTH_COMMAND_HPP_INCLUDED
#define GRIDLOG_AUTH_COMMAND_HPP_INCLUDED

#include <string>

#include "gridgain/impl/cmd/gridclientmessagecommand.hpp"

/**
 * Authentication request command.
 */
class GridAuthenticationRequestCommand : public GridClientMessageCommand {
public:
    /**
     * Set credentials method.
     *
     * @param creds Credentials.
     */
    void credentials(const std::string& creds) {
        creds_ = creds;
    }

    /**
     * Retrieve current credentials.
     *
     * @return Credentials.
     */
    const std::string credentials() const {
        return creds_;
    }

private:
    /** Credentials. */
    std::string creds_;
};
#endif
