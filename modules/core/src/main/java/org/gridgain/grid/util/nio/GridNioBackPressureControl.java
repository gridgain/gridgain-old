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

package org.gridgain.grid.util.nio;

import org.gridgain.grid.thread.*;

/**
 * Utility class that allows to ignore back-pressure control for threads that are processing messages.
 */
public class GridNioBackPressureControl {
    /** Thread local flag indicating that thread is processing message. */
    private static ThreadLocal<Boolean> threadProcMsg = new ThreadLocal<Boolean>() {
        @Override protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

    /**
     * @return Flag indicating whether current thread is processing message.
     */
    public static boolean threadProcessingMessage() {
        Thread th = Thread.currentThread();

        if (th instanceof GridThread)
            return ((GridThread)th).processingMessage();

        return threadProcMsg.get();
    }

    /**
     * @param processing Flag indicating whether current thread is processing message.
     */
    public static void threadProcessingMessage(boolean processing) {
        Thread th = Thread.currentThread();

        if (th instanceof GridThread)
            ((GridThread)th).processingMessage(processing);
        else
            threadProcMsg.set(processing);
    }
}
