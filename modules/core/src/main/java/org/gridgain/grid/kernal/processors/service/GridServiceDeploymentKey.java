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

package org.gridgain.grid.kernal.processors.service;

import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.util.typedef.internal.*;

/**
 * Service configuration key.
 */
public class GridServiceDeploymentKey extends GridCacheUtilityKey<GridServiceDeploymentKey> {
    /** */
    private static final long serialVersionUID = 0L;

    /** Service name. */
    private final String name;

    /**
     * @param name Service ID.
     */
    public GridServiceDeploymentKey(String name) {
        assert name != null;

        this.name = name;
    }

    /**
     * @return Service name.
     */
    public String name() {
        return name;
    }

    /** {@inheritDoc} */
    @Override protected boolean equalsx(GridServiceDeploymentKey that) {
        return name.equals(that.name);
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return name.hashCode();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridServiceDeploymentKey.class, this);
    }
}
