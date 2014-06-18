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

package org.gridgain.grid.streamer;

import java.util.*;

/**
 * Streamer failure listener. Asynchronous callback passed to user in case of any failure determined by streamer.
 *
 * @see GridStreamer#addStreamerFailureListener(GridStreamerFailureListener)
 *
 */
public interface GridStreamerFailureListener {
    /**
     * Callback invoked when unrecoverable failure is detected by streamer.
     * <p>
     * If {@link GridStreamerConfiguration#isAtLeastOnce()} is set to {@code false}, then this callback
     * will be invoked on node on which failure occurred. If {@link GridStreamerConfiguration#isAtLeastOnce()}
     * is set to {@code true}, then this callback will be invoked on node on which
     * {@link GridStreamer#addEvents(Collection)} or its variant was called. Callback will be called if maximum
     * number of failover attempts exceeded or failover cannot be performed (for example, if router
     * returned {@code null}).
     *
     * @param stageName Failed stage name.
     * @param evts Failed set of events.
     * @param err Error cause.
     */
    public void onFailure(String stageName, Collection<Object> evts, Throwable err);
}
