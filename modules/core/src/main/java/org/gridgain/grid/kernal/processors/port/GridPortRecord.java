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

package org.gridgain.grid.kernal.processors.port;

import org.gridgain.grid.spi.*;
import org.gridgain.grid.util.typedef.internal.*;

/**
 * This class defines record about port use.
 */
public class GridPortRecord {
    /** Port. */
    private int port;

    /** Protocol. */
    private GridPortProtocol proto;

    /** Class which uses port. */
    private Class cls;

    /**
     * @param port Port.
     * @param proto Protocol.
     * @param cls Class.
     */
    GridPortRecord(int port, GridPortProtocol proto, Class cls) {
        this.port = port;
        this.proto = proto;
        this.cls = cls;
    }

    /**
     * @return Port.
     */
    public int port() {
        return port;
    }

    /**
     * @return Protocol.
     */
    public GridPortProtocol protocol() {
        return proto;
    }

    /**
     * @return Class.
     */
    public Class clazz() {
        return cls;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridPortRecord.class, this);
    }
}
