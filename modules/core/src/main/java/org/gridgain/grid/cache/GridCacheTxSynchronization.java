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
 * Synchronization callback for transaction. You can subscribe to receive transaction
 * state change callbacks by registering transaction synchronization via
 * {@link GridCache#txSynchronize(GridCacheTxSynchronization)} method.
 */
public interface GridCacheTxSynchronization {
    /**
     * State change callback for transaction. Note that unless transaction has been
     * completed, it is possible to mark it for <tt>rollbackOnly</tt> by calling
     * {@link GridCacheTx#setRollbackOnly()} on the passed in transaction.
     * You can check the return value of {@link GridCacheTx#setRollbackOnly() setRollbackOnly()}
     * method to see if transaction was indeed marked for rollback or not.
     *
     * @param prevState Previous state of the transaction. If transaction has just been
     *      started, then previous state is {@code null}.
     * @param newState New state of the transaction. In majority of the cases this will be the
     *      same as {@link GridCacheTx#state() tx.state()}, but it is also possible
     *      that transaction may be marked for rollback concurrently with this method
     *      invocation, and in that case <tt>newState</tt> reflects the actual state of the
     *      transition this callback is associated with.
     * @param tx Transaction whose state has changed.
     */
    public void onStateChanged(@Nullable GridCacheTxState prevState, GridCacheTxState newState, GridCacheTx tx);
}
