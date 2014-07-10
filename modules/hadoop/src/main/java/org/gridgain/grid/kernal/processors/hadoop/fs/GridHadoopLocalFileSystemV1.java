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

package org.gridgain.grid.kernal.processors.hadoop.fs;

import org.apache.hadoop.fs.*;

import java.io.*;

/**
 * Local file system replacement for Hadoop jobs.
 */
public class GridHadoopLocalFileSystemV1 extends LocalFileSystem {
    /**
     * Creates new local file system.
     */
    public GridHadoopLocalFileSystemV1() {
        super(new GridHadoopRawLocalFileSystem());
    }

    /** {@inheritDoc} */
    @Override public File pathToFile(Path path) {
        return ((GridHadoopRawLocalFileSystem)getRaw()).convert(path);
    }
}
