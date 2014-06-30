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

package org.gridgain.grid;

import org.gridgain.grid.util.typedef.*;

import javax.swing.*;

/**
 * Starts test node.
 */
public class GridTestStoreNodeStartup {
    /**
     *
     */
    private GridTestStoreNodeStartup() {
        // No-op.
    }

    /**
     * @param args Arguments.
     * @throws Exception If failed.
     */
    public static void main(String[] args) throws Exception {
        try {
            Grid g = G.start("modules/core/src/test/config/spring-cache-teststore.xml");

            g.cache(null).loadCache(new P2<Object, Object>() {
                @Override public boolean apply(Object o, Object o1) {
                    System.out.println("Key=" + o + ", Val=" + o1);

                    return true;
                }
            }, 0, 15, 1);

            JOptionPane.showMessageDialog(null, "Press OK to stop test node.");
        }
        finally {
            G.stopAll(false);
        }
    }
}
