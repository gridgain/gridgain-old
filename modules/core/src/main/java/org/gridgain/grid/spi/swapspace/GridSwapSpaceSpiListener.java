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

package org.gridgain.grid.spi.swapspace;

import org.gridgain.grid.events.*;
import org.jetbrains.annotations.*;

/**
 * Swap space SPI eviction listener.
 */
public interface GridSwapSpaceSpiListener {
    /**
     * Notification for swap space events.
     *
     * @param evtType Event type. See {@link GridSwapSpaceEvent}
     * @param spaceName Space name for this event or {@code null} for default space.
     * @param keyBytes Key bytes of affected entry. Not {@code null} only for evict notifications.
     */
    public void onSwapEvent(int evtType, @Nullable String spaceName, @Nullable byte[] keyBytes);
}
