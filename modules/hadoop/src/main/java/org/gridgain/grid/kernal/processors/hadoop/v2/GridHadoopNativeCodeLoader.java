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

package org.gridgain.grid.kernal.processors.hadoop.v2;

import org.apache.hadoop.classification.*;
import org.apache.hadoop.conf.*;

/**
 * A fake helper to load the native hadoop code i.e. libhadoop.so.
 */
@InterfaceAudience.Private
@InterfaceStability.Unstable
public class GridHadoopNativeCodeLoader {
    /**
     * Check if native-hadoop code is loaded for this platform.
     *
     * @return <code>true</code> if native-hadoop is loaded,
     *         else <code>false</code>
     */
    public static boolean isNativeCodeLoaded() {
        return false;
    }

    /**
     * Returns true only if this build was compiled with support for snappy.
     */
    public static boolean buildSupportsSnappy() {
        return false;
    }

    /**
     * @return Library name.
     */
    public static String getLibraryName() {
        throw new IllegalStateException();
    }

    /**
     * Return if native hadoop libraries, if present, can be used for this job.
     * @param conf configuration
     *
     * @return <code>true</code> if native hadoop libraries, if present, can be
     *         used for this job; <code>false</code> otherwise.
     */
    public boolean getLoadNativeLibraries(Configuration conf) {
        return false;
    }

    /**
     * Set if native hadoop libraries, if present, can be used for this job.
     *
     * @param conf configuration
     * @param loadNativeLibraries can native hadoop libraries be loaded
     */
    public void setLoadNativeLibraries(Configuration conf, boolean loadNativeLibraries) {
        // No-op.
    }
}

