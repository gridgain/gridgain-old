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

package org.gridgain.grid.startup.servlet;

import org.gridgain.testframework.config.*;
import org.gridgain.testframework.junits.common.*;
import javax.management.*;
import javax.management.remote.*;
import java.io.*;
import java.util.*;

/**
 * Servlet loader test.
 *
 * 1. Create folder where all GridGain jar files will be placed.
 * For example: /home/ggdev/apache-tomcat-6.0.14/gridgain
 *
 * 2. Add in {@code $TOMCAT_HOME/conf/catalina.properties} for property {@code common.loader}
 * value {@code ,${catalina.home}/gridgain/*.jar}
 * For example, {@code common.loader=${catalina.home}/lib,${catalina.home}/lib/*.jar,${catalina.home}/gridgain/*.jar}
 *
 * 3. Add in {@code $TOMCAT_HOME/conf/web.xml}
 *          <pre class="snippet">
 *          &lt;servlet&gt;
 *              &lt;servlet-name&gt;GridGain&lt;/servlet-name&gt;
 *              &lt;servlet-class&gt;org.gridgain.grid.loaders.servlet.GridServletLoader&lt;/servlet-class&gt;
 *              &lt;init-param&gt;
 *                  &lt;param-name&gt;cfgFilePath&lt;/param-name&gt;
 *                  &lt;param-value&gt;config/default-config.xml&lt;/param-value&gt;
 *              &lt;/init-param&gt;
 *              &lt;load-on-startup&gt;5&lt;/load-on-startup&gt;
 *          &lt;/servlet&gt;</pre>
 *
 * 4. Change ports in {@code $TOMCAT_HOME/conf/server.xml} to 8006, 8084, 8446.
 *
 * 5. Add in {@code $TOMCAT_HOME/bin/catalina.sh} where script {@code start} argument handled
 * {@code JAVA_OPTS="${JAVA_OPTS} "-Dcom.sun.management.jmxremote.port=1097" "-Dcom.sun.management.jmxremote.ssl=false" "-Dcom.sun.management.jmxremote.authenticate=false" "}
 */
@GridCommonTest(group = "Loaders")
public class GridServletLoaderTest extends GridCommonAbstractTest {
    /** */
    public static final int JMX_RMI_CONNECTOR_PORT = 1097;

    /** */
    public static final int WAIT_DELAY = 5000;

    /**
     * @throws Exception If failed.
     */
    @SuppressWarnings({"unchecked"})
    public void testLoader() throws Exception {
        JMXConnector jmx = null;

        try {
            while (true) {
                try {
                    jmx = getJMXConnector("localhost",
                        Integer.valueOf(GridTestProperties.getProperty("tomcat.jmx.rmi.connector.port")));

                    if (jmx != null)
                        break;
                }
                catch (IOException e) {
                    log().warning("Failed to connect to server (will try again).", e);
                }

                Thread.sleep(WAIT_DELAY);
            }

            assert jmx != null;

            String query = "*:*";

            ObjectName queryName = new ObjectName(query);

            boolean found = false;

            ObjectName kernal = null;

            int i = 0;

            while (found == false) {
                info("Attempt to find GridKernal MBean [num=" + i + ']');

                Set<ObjectName> names = jmx.getMBeanServerConnection().queryNames(queryName, null);

                if (names.isEmpty() == false) {
                    for (ObjectName objectName : names) {
                        info("Found MBean for node: " + objectName);

                        String kernalName = objectName.getKeyProperty("name");

                        if ("GridKernal".equals(kernalName)) {
                            kernal = objectName;

                            found = true;
                        }
                    }
                }

                if (kernal == null) {
                    System.out.println("Node GridKernal MBean was not found.");

                    Thread.sleep(WAIT_DELAY);
                }

                i++;
            }

            UUID nodeId = (UUID)jmx.getMBeanServerConnection().getAttribute(kernal, "LocalNodeId");

            assert nodeId != null : "Failed to get Grid nodeId.";

            info("Found grid node with id: " + nodeId);
        }
        finally {
            if (jmx != null) {
                try {
                    jmx.close();

                    info("JMX connection closed.");
                }
                catch (IOException e) {
                    System.out.println("Failed to close JMX connection (will ignore): " + e.getMessage());
                }
            }
        }
    }

    /**
     * @param host JMX host.
     * @param port JMX port.
     * @return JMX connector.
     * @throws IOException If failed.
     */
    private static JMXConnector getJMXConnector(String host, int port) throws IOException {
        assert host != null;
        assert port > 0;

        JMXServiceURL serviceURL = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + host + ':' + port + "/jmxrmi");

        Map<String, Object> props = new HashMap<>();

        props.put(JMXConnectorFactory.PROTOCOL_PROVIDER_PACKAGES, "com.sun.jmx.remote.protocol");

        System.out.println("Try to connect to JMX server [props=" + props + ", url=" + serviceURL + ']');

        return JMXConnectorFactory.connect(serviceURL, props);
    }

    /** {@inheritDoc} */
    @Override protected long getTestTimeout() {
        // Wait for 5 minutes.
        return 5 * 60 * 1000;
    }
}
