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

package org.gridgain.grid;

import java.net.*;
import java.util.*;

/**
 * Provides resolution between external and internal addresses. In some cases
 * network routers are configured to perform address mapping between external
 * and internal networks and the same mapping must be available to SPIs
 * in GridGain that perform communication over IP protocols.
 */
public interface GridAddressResolver {
    /**
     * Maps internal address to a collection of external addresses.
     *
     * @param addr Internal (local) address.
     * @return Collection of addresses that this local address is "known" outside.
     *      Note that if there are more than one external network the local address
     *      can be mapped differently to each and therefore may need to return
     *      multiple external addresses.
     * @throws GridException Thrown if any exception occurs.
     */
    public Collection<InetSocketAddress> getExternalAddresses(InetSocketAddress addr) throws GridException;
}
