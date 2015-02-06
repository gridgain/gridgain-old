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

package org.gridgain.grid.kernal.visor.gui.dto;

import java.io.*;
import java.lang.management.*;

/**
 * Data transfer object for {@link LockInfo}.
 */
public class VisorThreadLockInfo implements Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    /**
     * Fully qualified name of the class of the lock object.
     */
    protected final String className;

    /**
     * Identity hash code of the lock object.
     */
    protected final Integer identityHashCode;

    /** Create thread lock info with given parameters. */
    public VisorThreadLockInfo(String className, Integer identityHashCode) {
        assert className != null;

        this.className = className;
        this.identityHashCode = identityHashCode;
    }

    /** Create data transfer object for given lock info. */
    public static VisorThreadLockInfo from(LockInfo li) {
        assert li != null;

        return new VisorThreadLockInfo(li.getClassName(), li.getIdentityHashCode());
    }

    /**
     * @return Fully qualified name of the class of the lock object.
     */
    public String className() {
        return className;
    }

    /**
     * @return Identity hash code of the lock object.
     */
    public Integer identityHashCode() {
        return identityHashCode;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return className + '@' + Integer.toHexString(identityHashCode);
    }
}
