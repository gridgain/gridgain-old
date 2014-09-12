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

package org.gridgain.grid.kernal.websession;

import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.*;

/**
 * Web sessions caching test.
 */
public class GridWebSessionTest {
    /** */
    private static final AtomicReference<String> SES_ID = new AtomicReference<>();

    /**
     * @param args Arguments.
     * @throws Exception In case of error.
     */
    public static void main(String[] args) throws Exception {
        doRequest(8091, false);
        doRequest(8092, true);
        doRequest(8092, true);
        doRequest(8092, true);
    }

    /**
     * @param port Port.
     * @param addCookie Whether to add cookie to request.
     * @throws IOException In case of I/O error.
     */
    private static void doRequest(int port, boolean addCookie) throws IOException {
        URLConnection conn = new URL("http://localhost:" + port + "/ggtest/test").openConnection();

        if (addCookie)
            conn.addRequestProperty("Cookie", "JSESSIONID=" + SES_ID.get());

        conn.connect();

        BufferedReader rdr = new BufferedReader(new InputStreamReader(conn.getInputStream()));

        if (!addCookie)
            SES_ID.set(rdr.readLine());
        else
            rdr.read();
    }
}
