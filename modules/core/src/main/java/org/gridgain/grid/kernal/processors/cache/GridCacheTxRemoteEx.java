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

package org.gridgain.grid.kernal.processors.cache;

import java.util.*;

/**
 * Local transaction API.
 */
public interface GridCacheTxRemoteEx<K, V> extends GridCacheTxEx<K, V> {
    /**
     * @return Remote thread ID.
     */
    public long remoteThreadId();

    /**
     * @param baseVer Base version.
     * @param committedVers Committed version.
     * @param rolledbackVers Rolled back version.
     * @param pendingVers Pending versions.
     */
    public void doneRemote(GridCacheVersion baseVer, Collection<GridCacheVersion> committedVers,
        Collection<GridCacheVersion> rolledbackVers, Collection<GridCacheVersion> pendingVers);

    /**
     * @param e Sets write value for pessimistic transactions.
     * @return {@code True} if entry was found.
     */
    public boolean setWriteValue(GridCacheTxEntry<K, V> e);

    /**
     * Adds remote candidates and completed versions to all involved entries.
     *
     * @param cands Candidates.
     * @param committedVers Committed versions.
     * @param rolledbackVers Rolled back versions.
     */
    public void addRemoteCandidates(
        Map<K, Collection<GridCacheMvccCandidate<K>>> cands,
        Collection<GridCacheVersion> committedVers,
        Collection<GridCacheVersion> rolledbackVers);
}
