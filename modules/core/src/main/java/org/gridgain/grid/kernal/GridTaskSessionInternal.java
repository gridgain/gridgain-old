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

package org.gridgain.grid.kernal;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Internal task session interface.
 */
public interface GridTaskSessionInternal extends GridComputeTaskSession {
    /**
     * @return Checkpoint SPI name.
     */
    public String getCheckpointSpi();

    /**
     * @return Job ID (possibly <tt>null</tt>).
     */
    @Nullable public GridUuid getJobId();

    /**
     * @return {@code True} if task node.
     */
    public boolean isTaskNode();

    /**
     * Closes session.
     */
    public void onClosed();

    /**
     * @return Checks if session is closed.
     */
    public boolean isClosed();

    /**
     * @return Task session.
     */
    public GridTaskSessionInternal session();

    /**
     * @return {@code True} if checkpoints and attributes are enabled.
     */
    public boolean isFullSupport();

    /**
     * @return Subject ID.
     */
    public UUID subjectId();
}
