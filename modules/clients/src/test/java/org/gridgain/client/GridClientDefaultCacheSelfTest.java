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

package org.gridgain.client;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.testframework.junits.common.*;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;

import static org.gridgain.client.GridClientProtocol.*;
import static org.gridgain.grid.GridSystemProperties.*;

/**
 * Tests that client is able to connect to a grid with only default cache enabled.
 */
public class GridClientDefaultCacheSelfTest extends GridCommonAbstractTest {
    /** Path to jetty config configured with SSL. */
    private static final String REST_JETTY_CFG = "modules/clients/src/test/resources/jetty/rest-jetty.xml";

    /** IP finder. */
    private static final GridTcpDiscoveryIpFinder IP_FINDER = new GridTcpDiscoveryVmIpFinder(true);

    /** Host. */
    private static final String HOST = "127.0.0.1";

    /** Port. */
    private static final int TCP_PORT = 11211;

    /** Cached local node id. */
    private UUID locNodeId;

    /** Http port. */
    private static final int HTTP_PORT = 8081;

    /** Url address to send HTTP request. */
    private static final String TEST_URL = "http://" + HOST + ":" + HTTP_PORT + "/gridgain";

    /** Used to sent request charset. */
    private static final String CHARSET = StandardCharsets.UTF_8.name();

    /** Name of node local cache. */
    private static final String LOCAL_CACHE = "local";

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        System.setProperty(GG_JETTY_PORT, String.valueOf(HTTP_PORT));

        startGrid().localNode().id();
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        stopGrid();

        System.clearProperty(GG_JETTY_PORT);
    }

    @Override
    protected void beforeTest() throws Exception {
        locNodeId = grid().localNode().id();
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        assert cfg.getClientConnectionConfiguration() == null;

        GridClientConnectionConfiguration clientCfg = new GridClientConnectionConfiguration();

        clientCfg.setRestJettyPath(REST_JETTY_CFG);

        cfg.setClientConnectionConfiguration(clientCfg);

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setIpFinder(IP_FINDER);

        cfg.setDiscoverySpi(disco);

        GridCacheConfiguration cLocal = new GridCacheConfiguration();

        cLocal.setName(LOCAL_CACHE);

        cLocal.setCacheMode(GridCacheMode.LOCAL);

        cLocal.setAtomicityMode(GridCacheAtomicityMode.TRANSACTIONAL);

        cfg.setCacheConfiguration(defaultCacheConfiguration(), cLocal);

        return cfg;
    }

    /**
     * @return Client.
     * @throws GridClientException In case of error.
     */
    private GridClient clientTcp() throws GridClientException {
        GridClientConfiguration cfg = new GridClientConfiguration();

        cfg.setProtocol(TCP);
        cfg.setServers(getServerList(TCP_PORT));
        cfg.setDataConfigurations(Collections.singleton(new GridClientDataConfiguration()));

        GridClient gridClient = GridClientFactory.start(cfg);

        assert F.exist(gridClient.compute().nodes(), new GridPredicate<GridClientNode>() {
            @Override public boolean apply(GridClientNode n) {
                return n.nodeId().equals(locNodeId);
            }
        });

        return gridClient;
    }

    /**
     * Builds list of connection strings with few different ports.
     * Used to avoid possible failures in case of port range active.
     *
     * @param startPort Port to start list from.
     * @return List of client connection strings.
     */
    private Collection<String> getServerList(int startPort) {
        Collection<String> srvs = new ArrayList<>();

        for (int i = startPort; i < startPort + 10; i++)
            srvs.add(HOST + ":" + i);

        return srvs;
    }

    /**
     * Send HTTP request to Jetty server of node and process result.
     *
     * @param query Send query parameters.
     * @return Processed response string.
     */
    private String sendHttp(String query) {
        String res = "No result";

        try {
            URLConnection connection = new URL(TEST_URL + "?" + query).openConnection();

            connection.setRequestProperty("Accept-Charset", CHARSET);

            BufferedReader r = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            res = r.readLine();

            r.close();
        }
        catch (IOException e) {
            error("Failed to send HTTP request: " + TEST_URL + "?" + query, e);
        }

        // Cut node id from response.
        return res.substring(res.indexOf("\"response\""));
    }

    /**
     * @throws Exception If failed.
     */
    public void testTcp() throws Exception {
        try {
            boolean putRes = cache().putx("key", 1);

            assert putRes : "Put operation failed";

            GridClient client = clientTcp();

            Integer val = client.data().<String, Integer>get("key");

            assert val != null;

            assert val == 1;
        }
        finally {
            GridClientFactory.stopAll();
        }
    }

    /**
     * Json format string in cache should not transform to Json object on get request.
     */
    public void testSkipString2JsonTransformation() {
        // Put to cache JSON format string value.
        assertEquals("Incorrect query response", "\"response\":true,\"sessionToken\":\"\",\"successStatus\":0}",
                sendHttp("cmd=put&cacheName=" + LOCAL_CACHE +
                        "&key=a&val=%7B%22v%22%3A%22my%20Value%22%2C%22t%22%3A1422559650154%7D"));

        // Escape '\' symbols disappear from response string on transformation to JSON object.
        assertEquals("Incorrect query response",
                "\"response\":\"{\\\"v\\\":\\\"my Value\\\",\\\"t\\\":1422559650154}\",\"sessionToken\":\"\",\"successStatus\":0}",
                sendHttp("cmd=get&cacheName=" + LOCAL_CACHE + "&key=a"));
    }
}
