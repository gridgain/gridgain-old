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

#ifndef GRID_CLIENT_COMMAND_EXECUTOR_FACTORY_HPP_INCLUDED
#define GRID_CLIENT_COMMAND_EXECUTOR_FACTORY_HPP_INCLUDED

#include "gridgain/gridclientprotocolconfiguration.hpp"
#include "gridgain/impl/cmd/gridclientcommandexecutor.hpp"
#include "gridgain/impl/connection/gridclientconnectionpool.hpp"

/**
 * Command executor factory.
 */
class GridClientCommandExecutorFactory {
public:
    /**
     * Command executor factory method.
     *
     * @param protoCfg Protocol configuration.
     * @param pool Connection pool.
     * @return Shared pointer to a command executor.
     */
    static boost::shared_ptr<GridClientCommandExecutor> createCommandExecutor(
         const GridClientProtocolConfiguration& protoCfg, boost::shared_ptr<GridClientConnectionPool>& pool);

private:
    /** Default constructor - made private to disable instantiation. */
    GridClientCommandExecutorFactory();

    /**
     * Copy constructor - made private to disable instantiation.
     *
     * @param other Another instance of executor factory.
     */
    GridClientCommandExecutorFactory(const GridClientCommandExecutorFactory& other);

    /**
     * Assign operator - made private to disable copying.
     *
     * @param other Another instance of executor factory.
     */
    const GridClientCommandExecutorFactory& operator = (const GridClientCommandExecutorFactory& other);
};

#endif
