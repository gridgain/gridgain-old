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

import org.gridgain.grid.logger.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.util.*;

import static org.gridgain.grid.GridSystemProperties.*;

/**
 * Grid performance suggestions.
 */
public class GridPerformanceSuggestions {
    /** */
    private static final boolean disabled = Boolean.getBoolean(GG_PERFORMANCE_SUGGESTIONS_DISABLED);

    /** */
    private final Collection<String> perfs = !disabled ? new LinkedHashSet<String>() : null;

    /** */
    private final Collection<String> suppressed = !disabled ? new HashSet<String>() : null;

    /**
     * @param sug Suggestion to add.
     */
    public synchronized void add(String sug) {
        add(sug, false);
    }

    /**
     * @param sug Suggestion to add.
     * @param suppress {@code True} to suppress this suggestion.
     */
    public synchronized void add(String sug, boolean suppress) {
        if (disabled)
            return;

        if (!suppress)
            perfs.add(sug);
        else
            suppressed.add(sug);
    }

    /**
     * @param log Log.
     * @param gridName Grid name.
     */
    public synchronized void logSuggestions(GridLogger log, @Nullable String gridName) {
        if (disabled)
            return;

        if (!F.isEmpty(perfs) && !suppressed.containsAll(perfs)) {
            U.quietAndInfo(log, "Performance suggestions for grid " +
                (gridName == null ? "" : '\'' + gridName + '\'') + " (fix if possible)");
            U.quietAndInfo(log, "To disable, set -D" + GG_PERFORMANCE_SUGGESTIONS_DISABLED + "=true");

            for (String s : perfs)
                if (!suppressed.contains(s))
                    U.quietAndInfo(log, "  ^-- " + s);

            U.quietAndInfo(log, "");

            perfs.clear();
        }
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridPerformanceSuggestions.class, this);
    }
}
