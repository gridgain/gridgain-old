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

package org.gridgain.grid.cache;

import org.jetbrains.annotations.*;

/**
 * Cache transaction state.
 */
public enum GridCacheTxState {
    /** Transaction started. */
    ACTIVE,

    /** Transaction validating. */
    PREPARING,

    /** Transaction validation succeeded. */
    PREPARED,

    /** Transaction is marked for rollback. */
    MARKED_ROLLBACK,

    /** Transaction commit started (validating finished). */
    COMMITTING,

    /** Transaction commit succeeded. */
    COMMITTED,

    /** Transaction rollback started (validation failed). */
    ROLLING_BACK,

    /** Transaction rollback succeeded. */
    ROLLED_BACK,

    /** Transaction rollback failed or is otherwise unknown state. */
    UNKNOWN;

    /** Enumerated values. */
    private static final GridCacheTxState[] VALS = values();

    /**
     * Efficiently gets enumerated value from its ordinal.
     *
     * @param ord Ordinal value.
     * @return Enumerated value or {@code null} if ordinal out of range.
     */
    @Nullable public static GridCacheTxState fromOrdinal(int ord) {
        return ord >= 0 && ord < VALS.length ? VALS[ord] : null;
    }
}
