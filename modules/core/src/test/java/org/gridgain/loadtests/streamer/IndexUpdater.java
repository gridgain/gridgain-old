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

package org.gridgain.loadtests.streamer;

import org.gridgain.grid.streamer.index.*;
import org.jetbrains.annotations.*;

/**
 * Streamer benchmark window index updater.
 */
class IndexUpdater implements GridStreamerIndexUpdater<Integer, Integer, Long> {
    /** {@inheritDoc} */
    @Override public Integer indexKey(Integer evt) {
        return evt;
    }

    /** {@inheritDoc} */
    @Nullable @Override public Long onAdded(GridStreamerIndexEntry<Integer, Integer, Long> entry, Integer evt) {
        return entry.value() + 1;
    }

    /** {@inheritDoc} */
    @Nullable @Override public Long onRemoved(GridStreamerIndexEntry<Integer, Integer, Long> entry, Integer evt) {
        return entry.value() - 1 == 0 ? null : entry.value() - 1;
    }

    /** {@inheritDoc} */
    @Override public Long initialValue(Integer evt, Integer key) {
        return 1L;
    }
}
