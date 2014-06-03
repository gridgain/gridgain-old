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

package org.gridgain.grid.util.lang;

import org.gridgain.grid.util.typedef.internal.*;

/**
 * Adapter for common interfaces in closures, reducers and predicates.
 */
public class GridPeerDeployAwareAdapter implements GridPeerDeployAware {
    /** */
    private static final long serialVersionUID = 0L;

    /** Peer deploy aware class. */
    protected transient GridPeerDeployAware pda;

    /**
     * Sets object that from which peer deployment information
     * will be copied, i.e. this lambda object will be peer deployed
     * using the same class loader as given object.
     * <p>
     * Note that in most cases GridGain attempts to automatically call this
     * method whenever lambda classes like closures and predicates are created that
     * wrap user object (the peer deploy information in such cases will be copied
     * from the user object).
     * <p>
     * In general, if user gets class not found exception during peer loading it is
     * very likely that peer deploy information was lost during wrapping of one object
     * into another.
     *
     * @param obj Peer deploy aware.
     */
    public void peerDeployLike(Object obj) {
        assert obj != null;

        pda = U.peerDeployAware(obj);
    }

    /** {@inheritDoc} */
    @Override public Class<?> deployClass() {
        if (pda == null)
            pda = U.detectPeerDeployAware(this);

        return pda.deployClass();
    }

    /** {@inheritDoc} */
    @Override public ClassLoader classLoader() {
        if (pda == null)
            pda = U.detectPeerDeployAware(this);

        return pda.classLoader();
    }
}
