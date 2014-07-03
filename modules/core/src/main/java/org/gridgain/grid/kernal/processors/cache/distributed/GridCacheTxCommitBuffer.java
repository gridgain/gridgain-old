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

package org.gridgain.grid.kernal.processors.cache.distributed;

import org.gridgain.grid.kernal.processors.cache.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Buffer that stores transaction commit values in order to restore them in case of originating node crash.
 */
public interface GridCacheTxCommitBuffer<K, V> {
    /**
     * Adds committed transaction to commit buffer.
     *
     * @param tx Committed transaction.
     */
    public void addCommittedTx(GridCacheTxEx<K, V> tx);

    /**
     * Gets transaction from commit buffer.
     *
     * @param originatingTxVer Originating tx version.
     * @param nodeId Originating node ID.
     * @param threadId Originating thread ID.
     * @return Committed info, if any.
     */
    @Nullable public GridCacheCommittedTxInfo<K, V> committedTx(GridCacheVersion originatingTxVer, UUID nodeId,
        long threadId);

    /**
     * Callback called when lode left grid. Used to eventually cleanup the queue from committed tx info from
     * left node.
     *
     * @param nodeId Left node ID.
     */
    public void onNodeLeft(UUID nodeId);

    /**
     * @return Buffer size.
     */
    public int size();
}
