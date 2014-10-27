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

package org.gridgain.grid.portables;

import org.jetbrains.annotations.*;

/**
 * Exception indicating that class needed for deserialization of portable object does not exist.
 * <p>
 * Thrown from {@link GridPortableObject#deserialize()} method.
 */
public class GridPortableInvalidClassException extends GridPortableException {
    /** */
    private static final long serialVersionUID = 0L;

    /**
     * Creates invalid class exception with error message.
     *
     * @param msg Error message.
     */
    public GridPortableInvalidClassException(String msg) {
        super(msg);
    }

    /**
     * Creates invalid class exception with {@link Throwable} as a cause.
     *
     * @param cause Cause.
     */
    public GridPortableInvalidClassException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates invalid class exception with error message and {@link Throwable} as a cause.
     *
     * @param msg Error message.
     * @param cause Cause.
     */
    public GridPortableInvalidClassException(String msg, @Nullable Throwable cause) {
        super(msg, cause);
    }
}
