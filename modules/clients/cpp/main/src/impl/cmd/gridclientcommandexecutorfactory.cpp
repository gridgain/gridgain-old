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
#include "gridgain/impl/utils/gridclientdebug.hpp"

#include "gridgain/gridclientvariant.hpp"
#include "gridgain/impl/cmd/gridclientcommandexecutorfactory.hpp"
#include "gridgain/impl/cmd/gridclienttcpcommandexecutor.hpp"

/**
 * Creates command executor based on protocol configuration.
 *
 * @param protoCfg protocol configuration
 * @param connPool connection pool
 * @return Command executor based on that configuration using that connection pool.
 */
boost::shared_ptr<GridClientCommandExecutor> GridClientCommandExecutorFactory::createCommandExecutor
    (const GridClientProtocolConfiguration& protoCfg, boost::shared_ptr<GridClientConnectionPool>& connPool) {
    boost::shared_ptr<GridClientCommandExecutor> cmdExecutor(new GridClientTcpCommandExecutor(connPool));

    return cmdExecutor;
}
