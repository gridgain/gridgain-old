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

package org.gridgain.grid.kernal.processors.rest.protocols;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.processors.rest.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;
import sun.misc.*;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.security.*;
import java.util.*;

/**
 * Abstract protocol adapter.
 */
public abstract class GridRestProtocolAdapter implements GridRestProtocol {
    /** UTF-8 charset. */
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    /** Context. */
    protected final GridKernalContext ctx;

    /** Logger. */
    protected final GridLogger log;

    /** Secret key. */
    protected final String secretKey;

    /** Host used by this protocol. */
    protected InetAddress host;

    /** Port used by this protocol. */
    protected int port;

    /**
     * @param ctx Context.
     */
    @SuppressWarnings({"OverriddenMethodCallDuringObjectConstruction"})
    protected GridRestProtocolAdapter(GridKernalContext ctx) {
        assert ctx != null;

        this.ctx = ctx;

        log = ctx.log(getClass());

        secretKey = ctx.config().getRestSecretKey();
    }

    /**
     * Authenticates current request.
     * <p>
     * Token consists of 2 parts separated by semicolon:
     * <ol>
     *     <li>Timestamp (time in milliseconds)</li>
     *     <li>Base64 encoded SHA1 hash of {1}:{secretKey}</li>
     * </ol>
     *
     * @param tok Authentication token.
     * @return {@code true} if authentication info provided in request is correct.
     */
    protected boolean authenticate(@Nullable String tok) {
        if (F.isEmpty(secretKey))
            return true;

        if (F.isEmpty(tok))
            return false;

        StringTokenizer st = new StringTokenizer(tok, ":");

        if (st.countTokens() != 2)
            return false;

        String ts = st.nextToken();
        String hash = st.nextToken();

        String s = ts + ':' + secretKey;

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");

            BASE64Encoder enc = new BASE64Encoder();

            md.update(s.getBytes(UTF_8));

            String compHash = enc.encode(md.digest());

            return hash.equalsIgnoreCase(compHash);
        }
        catch (NoSuchAlgorithmException e) {
            U.error(log, "Failed to check authentication signature.", e);
        }

        return false;
    }

    /**
     * @return Start information string.
     */
    protected String startInfo() {
        return "Command protocol successfully started [name=" + name() + ", host=" + host + ", port=" + port + ']';
    }

    /**
     * @return Stop information string.
     */
    protected String stopInfo() {
        return "Command protocol successfully stopped: " + name();
    }

    /**
     * @param cond Condition to check.
     * @param condDesc Error message.
     * @throws GridException If check failed.
     */
    protected final void assertParameter(boolean cond, String condDesc) throws GridException {
        if (!cond)
            throw new GridException("REST protocol parameter failed condition check: " + condDesc);
    }

    /** {@inheritDoc} */
    @Override public Collection<GridBiTuple<String, Object>> getProperties() {
        try {
            // All addresses for wildcard endpoint, `null` without.
            GridBiTuple<Collection<String>, Collection<String>> addrs = host != null ?
                U.resolveLocalAddresses(host) : null;

            return Arrays.asList(
                F.<String, Object>t(getAddressPropertyName(), addrs.get1()),
                F.<String, Object>t(getHostNamePropertyName(), addrs.get2()),
                F.<String, Object>t(getPortPropertyName(), port)
            );
        }
        catch (GridException | IOException ignored) {
            return Arrays.asList(
                F.t(getAddressPropertyName(), null),
                F.t(getHostNamePropertyName(), null),
                F.<String, Object>t(getPortPropertyName(), port)
            );
        }
    }

    /**
     * Return node attribute name to store used address.
     *
     * @return Node attribute name.
     */
    protected abstract String getAddressPropertyName();

    /**
     * Return node attribute name to store used host name.
     *
     * @return Node attribute name.
     */
    protected abstract String getHostNamePropertyName();

    /**
     * Return node attribute name to store used port number.
     *
     * @return Node attribute name.
     */
    protected abstract String getPortPropertyName();
}
