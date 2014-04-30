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

#include <map>
#include <vector>
#include <string>
#include <cstdio>

#include <boost/uuid/uuid_io.hpp>
#include <boost/test/unit_test.hpp>
#include <boost/lexical_cast.hpp>

#include <gridgain/gridgain.hpp>

#include "gridclientfactoryfixture.hpp"
#include "gridtestcommon.hpp"
#include "gridgain/impl/connection/gridclienttcpconnection.hpp"
#include "gridgain/impl/cmd/gridclientmessageauthrequestcommand.hpp"
#include "gridgain/impl/cmd/gridclientmessageauthresult.hpp"
#include "gridgain/impl/marshaller/protobuf/gridclientprotobufmarshaller.hpp"

using namespace org::gridgain::grid::kernal::processors::rest::client::message;

/**
 * A test TCP connection with special logic for checking several
 * possible issues.
 */
class GridClientTestTcpConnection: public GridClientAsyncTcpConnection {
public:
    /**
     * Tries to authenticate using a random destination ID.
     * This should fail.
     *
     * @param clientId Client ID.
     * @param cred Authentication credentials.
     */
    void authenticate(const string& clientId, const string& creds) {
        ObjectWrapper protoMsg;

        GridAuthenticationRequestCommand authReq;
        GridClientMessageAuthenticationResult authResult;

        authReq.setClientId(clientId);
        authReq.credentials(creds);
        authReq.setRequestId(1);
        authReq.setDestinationId(GridClientUuid::randomUuid()); // Random unexistent ID.

        GridClientProtobufMarshaller::wrap(authReq, protoMsg);

        GridClientTcpPacket tcpPacket;
        GridClientTcpPacket tcpResponse;
        ProtoRequest req;

        tcpPacket.setData(protoMsg);
        tcpPacket.setAdditionalHeaders(authReq);

        send(tcpPacket, tcpResponse);

        ObjectWrapper respMsg = tcpResponse.getData();

        GridClientProtobufMarshaller::unwrap(respMsg, authResult);

        sessToken = authResult.sessionToken();
    }
};

BOOST_AUTO_TEST_SUITE(GridRouterSelfTest)

using namespace std;

BOOST_AUTO_TEST_CASE(testAbsentDestinationId) {
    GridClientTestTcpConnection conn;

    GridClientConfiguration config = clientConfig();
    std::vector<GridClientSocketAddress> routers = config.routers();
    GridClientProtocolConfiguration protoCfg = config.protocolConfiguration();

    conn.connect(routers[0].host(), boost::lexical_cast<int>(routers[0].port()));

    BOOST_CHECK_THROW( conn.authenticate(protoCfg.uuid().uuid(), protoCfg.credentials()), GridClientException);

    conn.close();
}

BOOST_AUTO_TEST_SUITE_END()

