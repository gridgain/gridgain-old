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

package org.gridgain.grid.kernal.visor.gui.tasks;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.processors.task.*;
import org.gridgain.grid.kernal.visor.cmd.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.net.*;
import java.util.*;

/**
 * Task that resolve host name for specified IP address from node.
 */
@GridInternal
public class VisorResolveHostNameTask extends VisorOneNodeTask<Void, Map<String, String>> {
    /** */
    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @Override protected VisorResolveHostNameJob job(Void arg) {
        return new VisorResolveHostNameJob(arg);
    }

    /**
     * Job that resolve host name for specified IP address.
     */
    private static class VisorResolveHostNameJob extends VisorJob<Void, Map<String, String>> {
        /** */
        private static final long serialVersionUID = 0L;

        /**
         * Create job.
         *
         * @param arg List of IP address for resolve.
         */
        private VisorResolveHostNameJob(Void arg) {
            super(arg);
        }

        /** {@inheritDoc} */
        @Override protected Map<String, String> run(Void arg) throws GridException {
            Map<String, String> res = new HashMap<>();

            try {
                GridBiTuple<Collection<String>, Collection<String>> addrs =
                    GridUtils.resolveLocalAddresses(InetAddress.getByName("0.0.0.0"));

                assert(addrs.get1() != null);
                assert(addrs.get2() != null);

                Iterator<String> ipIt = addrs.get1().iterator();
                Iterator<String> hostIt = addrs.get2().iterator();

                while(ipIt.hasNext() && hostIt.hasNext()) {
                    String ip = ipIt.next();

                    String hostName = hostIt.next();

                    if (hostName == null || hostName.trim().isEmpty()) {
                        try {
                            if (InetAddress.getByName(ip).isLoopbackAddress())
                                res.put(ip, "localhost");
                        }
                        catch (Exception ignore) {
                            //no-op
                        }
                    }
                    else if (!hostName.equals(ip))
                        res.put(ip, hostName);
                }
            }
            catch (Throwable e) {
                throw new GridException("Failed to resolve host name", e);
            }

            return res;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(VisorResolveHostNameJob.class, this);
        }
    }
}
