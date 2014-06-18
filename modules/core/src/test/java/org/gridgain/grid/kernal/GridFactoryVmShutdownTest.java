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
import org.gridgain.grid.util.typedef.*;
import org.jetbrains.annotations.*;

import java.util.concurrent.*;

import static org.gridgain.grid.GridGainState.*;

/**
 * Tests for {@link GridGain}.
 */
public class GridFactoryVmShutdownTest {
    /**
     *
     */
    private GridFactoryVmShutdownTest() {
        // No-op.
    }

    /**
     * @param args Args (optional).
     * @throws Exception If failed.
     */
    public static void main(String[] args) throws Exception {
        final ConcurrentMap<String, GridGainState> states = new ConcurrentHashMap<>();

        G.addListener(new GridGainListener() {
            @Override public void onStateChange(@Nullable String name, GridGainState state) {
                if (state == STARTED) {
                    GridGainState state0 = states.put(maskNull(name), STARTED);

                    assert state0 == null;
                }
                else {
                    assert state == STOPPED;

                    boolean replaced = states.replace(maskNull(name), STARTED, STOPPED);

                    assert replaced;
                }
            }
        });

        // Test with shutdown hook enabled and disabled.
        // System.setProperty(GridSystemProperties.GG_NO_SHUTDOWN_HOOK, "true");

        // Grid will start and add shutdown hook.
        G.start();

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override public void run() {
                GridConfiguration cfg = new GridConfiguration();

                cfg.setGridName("test1");

                try {
                    G.start(cfg);
                }
                catch (GridException e) {
                    throw new GridRuntimeException("Failed to start grid in shutdown hook.", e);
                }
                finally {
                    X.println("States: " + states);
                }
            }
        }));

        System.exit(0);
    }

    /**
     * Masks {@code null} string.
     *
     * @param s String to mask.
     * @return Mask value or string itself if it is not {@code null}.
     */
    private static String maskNull(String s) {
        return s != null ? s : "null-mask-8AE34BF8";
    }
}
