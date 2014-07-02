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

#ifndef GRID_CLIENT_MESSAGE_RESULT_HPP_INCLUDED
#define GRID_CLIENT_MESSAGE_RESULT_HPP_INCLUDED

#include <string>

#include "gridgain/impl/cmd/gridclientmessage.hpp"

/**
 * Generic message result.
 */
class GridClientMessageResult : public GridClientMessage {
public:
    /** Status code. */
    enum StatusCode {
        /** Command succeeded. */
        STATUS_SUCCESS = 0,

        /** Command failed. */
        STATUS_FAILED = 1,

        /** Authentication failure. */
        STATUS_AUTH_FAILURE = 2
    };

    /**
     * Retrieves error message if any.
     *
     * @return Operation error message.
     */
    std::string getErrorMsg() const {
        return errorMsg;
    }

    /**
     * Indicator was the operation successful or not.
     *
     * @return true If it was successful, false otherwise.
     */
    bool isSuccess() const {
        return status == STATUS_SUCCESS;
    }

    /**
     * Sets a new error message.
     *
     * @param pErrorMsg New error message.
     */
    void setErrorMsg(const std::string& errorMsg) {
        this->errorMsg = errorMsg;
    }

    /**
     * Sets the message result status code.
     *
     * @param code New status code.
     */
    void setStatus(StatusCode code) {
        assert(code >= STATUS_SUCCESS && code <= STATUS_AUTH_FAILURE);

        status = code;
    }

private:
    /** Success flag */
    StatusCode status;

    /** Error message, if any. */
    std::string errorMsg;
};

#endif
