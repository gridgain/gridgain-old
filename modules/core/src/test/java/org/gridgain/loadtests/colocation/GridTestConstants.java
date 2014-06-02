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

package org.gridgain.loadtests.colocation;

/**
 * Set of constants for this project.
 */
public class GridTestConstants {
    /** Number of modulo regions to partition keys into. */
    public static final int MOD_COUNT = 1024;

    /** Number of entries to put in cache. */
    public static final int ENTRY_COUNT = 2000000;

    /** Cache init size - add some padding to avoid resizing. */
    public static final int CACHE_INIT_SIZE = (int)(1.5 * ENTRY_COUNT);

    /** Number of threads to load cache. */
    public static final int LOAD_THREADS = Runtime.getRuntime().availableProcessors() * 2;
}
