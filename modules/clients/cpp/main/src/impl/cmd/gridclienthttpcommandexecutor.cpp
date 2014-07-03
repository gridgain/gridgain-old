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

#include <cstdio>
#include <cstdlib>

#include "gridgain/gridclientvariant.hpp"
#include "gridgain/impl/cmd/gridclienthttpcommandexecutor.hpp"
#include "gridgain/impl/connection/gridclienthttpconnection.hpp"

/**
 * Stops the command executor freeing all resources
 * and closing all connections.
 */
void GridClientHttpCommandExecutor::stop() {
    connPool->close();
}

/**
 * Sends a request to the specified host
 *
 * @param host Host/port pair to connect to.
 * @param msg Message to send.
 * @param respMsg Response message to fill.
 */
void GridClientHttpCommandExecutor::sendPacket(const GridClientSocketAddress& host,
        const TRequestParams& msg,
        TJson& respMsg) {
    std::shared_ptr<GridClientHttpConnection> conn =
            connPool->rentHttpConnection(host.host(), host.port());

    try {
        conn->send(msg, respMsg);

        connPool->turnBack(conn);
    }
    catch(GridClientException&) {
        connPool->steal(conn); // Throw this connection away.

        conn->close();

        throw;
    }
}

/**
 * Sends a log command to a remote host.
 *
 * @param host Host/port pair to connect to.
 * @param logCmd Log command to send.
 * @param rslt Log response message to fill.
 */
void GridClientHttpCommandExecutor::executeLogCmd(const GridClientSocketAddress& host,
        GridLogRequestCommand& logCmd, GridClientMessageLogResult& rslt) {
    executeCmd(host, logCmd, rslt);
}

/**
 * Sends a topology command to a remote host.
 *
 * @param host Host/port pair to connect to.
 * @param topCmd Topology command to send.
 * @param rslt Topology response message to fill.
 */
void GridClientHttpCommandExecutor::executeTopologyCmd(
        const GridClientSocketAddress& host, GridTopologyRequestCommand& topCmd,
        GridClientMessageTopologyResult& rslt) {

    executeCmd(host, topCmd, rslt);
}

/**
 * Sends a cache get command to a remote host.
 *
 * @param host Host/port pair to connect to.
 * @param cacheCmd Cache command to send.
 * @param rslt Cache get response message to fill.
 */
void GridClientHttpCommandExecutor::executeGetCacheCmd(
        const GridClientSocketAddress& host, GridCacheRequestCommand& cacheCmd,
        GridClientMessageCacheGetResult& rslt) {
    executeCmd(host, cacheCmd, rslt);
}

/**
 * Sends a cache modify command to a remote host.
 *
 * @param host Host/port pair to connect to.
 * @param cacheCmd Cache modify command to send.
 * @param rslt Cache modify response message to fill.
 */
void GridClientHttpCommandExecutor::executeModifyCacheCmd(
        const GridClientSocketAddress& host, GridCacheRequestCommand& cacheCmd,
        GridClientMessageCacheModifyResult& rslt) {
    executeCmd(host, cacheCmd, rslt);
}

/**
 * Sends a cache metrics command to a remote host.
 *
 * @param host Host/port pair to connect to.
 * @param cacheCmd Cache metrics command to send.
 * @param rslt Cache metrics response message to fill.
 */
void GridClientHttpCommandExecutor::executeGetCacheMetricsCmd(
        const GridClientSocketAddress& host, GridCacheRequestCommand& cacheCmd,
        GridClientMessageCacheMetricResult& rslt) {
    executeCmd(host, cacheCmd, rslt);
}

/**
 * Sends a task command to a remote host.
 *
 * @param host Host/port pair to connect to.
 * @param taskCmd task command to send.
 * @param rslt Task response message to fill.
 */
void GridClientHttpCommandExecutor::executeTaskCmd(const GridClientSocketAddress& host,
        GridTaskRequestCommand& taskCmd,
        GridClientMessageTaskResult& rslt) {
    executeCmd(host, taskCmd, rslt);
}

/**
 * Sends a general command to a remote host.
 *
 * @param host Host/port pair to connect to.
 * @param cmd Command to send.
 * @param rslt Response message to fill.
 */
template<class C, class R> void GridClientHttpCommandExecutor::executeCmd(
        const GridClientSocketAddress& host, C& cmd, R& rslt) {
    TRequestParams protoMsg;
    TJson respMsg;

    GridClientJsonMarshaller::wrap(cmd, protoMsg);

    sendPacket(host, protoMsg, respMsg);

    GridClientJsonMarshaller::unwrap(respMsg, rslt);
}
