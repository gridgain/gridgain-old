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

#ifndef GRIDCLIENTCACHEFLAG_HPP_
#define GRIDCLIENTCACHEFLAG_HPP_

/**
 * Cache projection flags that specify projection behaviour.
 */
enum GridClientCacheFlag {
    /** Skips store, i.e. no read-through and no write-through behavior. */
    SKIP_STORE = 1,

    /** Skip swap space for reads and writes. */
    SKIP_SWAP = 1 << 1,

    /** Synchronous commit. */
    SYNC_COMMIT = 1 << 2,

    /** Synchronous rollback. */
    SYNC_ROLLBACK = 1 << 3,

    /**
     * Switches a cache projection to work in {@code 'invalidation'} mode.
     * Instead of updating remote entries with new values, small invalidation
     * messages will be sent to set the values to {@code null}.
     */
    INVALIDATE = 1 << 4
};


#endif /* GRIDCLIENTCACHEFLAG_HPP_ */
