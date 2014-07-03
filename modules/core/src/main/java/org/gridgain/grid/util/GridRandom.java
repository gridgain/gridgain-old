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

package org.gridgain.grid.util;

import java.util.*;
import java.util.concurrent.*;

/**
 * Random to be used from a single thread. Compatible with {@link Random} but faster.
 */
public class GridRandom extends Random {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private long rnd;

    /**
     * Default constructor.
     */
    public GridRandom() {
        this(ThreadLocalRandom.current().nextLong());
    }

    /**
     * @param seed Seed.
     */
    public GridRandom(long seed) {
        setSeed(seed);
    }

    /** {@inheritDoc} */
    @Override public void setSeed(long seed) {
        rnd = (seed ^ 0x5DEECE66DL) & ((1L << 48) - 1);
    }

    /** {@inheritDoc} */
    @Override protected int next(int bits) {
        rnd = (rnd * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
        return (int)(rnd >>> (48 - bits));
    }
}
