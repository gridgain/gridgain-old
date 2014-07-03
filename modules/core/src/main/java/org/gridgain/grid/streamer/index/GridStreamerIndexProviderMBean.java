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

package org.gridgain.grid.streamer.index;

import org.gridgain.grid.util.mbean.*;
import org.jetbrains.annotations.*;

/**
 * Streamer window index provider MBean.
 */
public interface GridStreamerIndexProviderMBean {
    /**
     * Index name.
     *
     * @return Index name.
     */
    @GridMBeanDescription("Index name.")
    @Nullable public String name();

    /**
     * Gets index updater class name.
     *
     * @return Index updater class.
     */
    @GridMBeanDescription("Index updater class name.")
    public String updaterClass();

    /**
     * Gets index unique flag.
     *
     * @return Index unique flag.
     */
    @GridMBeanDescription("Index unique flag.")
    public boolean unique();

    /**
     * Returns {@code true} if index supports sorting and therefore can perform range operations.
     *
     * @return Index sorted flag.
     */
    @GridMBeanDescription("Index sorted flag.")
    public boolean sorted();

    /**
     * Gets index policy.
     *
     * @return Index policy.
     */
    @GridMBeanDescription("Index policy.")
    public GridStreamerIndexPolicy policy();

    /**
     * Gets current index size.
     *
     * @return Current index size.
     */
    @GridMBeanDescription("Current index size.")
    public int size();
}
