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

#ifndef GRID_CLIENT_MESSAGE_TASK_REQUEST_COMMAND_HPP_INCLUDED
#define GRID_CLIENT_MESSAGE_TASK_REQUEST_COMMAND_HPP_INCLUDED

#include <string>

#include "gridgain/gridclienttypedef.hpp"
#include "gridgain/impl/cmd/gridclientmessagecommand.hpp"

/**
 * Generic message result.
 */
class GridTaskRequestCommand : public GridClientMessageCommand {
public:
    /**
     * Method for retrieving task arguments.
     *
     * @return Task argument.
     */
    GridClientVariant getArg() const {
        return arg;
    }

    /**
     * Method for retrieving task name.
     *
     * @return Task name.
     */
    std::string getTaskName() const {
        return taskName;
    }

    /**
     * Set new task argument.
     *
     * @param pArg Task argument.
     */
    void setArg(const GridClientVariant& pArg) {
        arg = pArg;
    }

    /**
     * Set new task name.
     *
     * @param pTaskname Task name.
     */
    void setTaskName(const std::string& pTaskName) {
        taskName = pTaskName;
    }

private:
    /** Task name. */
    std::string taskName;

    /** Task argument. */
    GridClientVariant arg;
};

#endif
