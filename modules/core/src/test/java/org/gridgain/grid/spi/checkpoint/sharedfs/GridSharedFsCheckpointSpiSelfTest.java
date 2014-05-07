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

package org.gridgain.grid.spi.checkpoint.sharedfs;

import org.gridgain.grid.spi.checkpoint.*;
import org.gridgain.grid.util.typedef.F;
import org.gridgain.testframework.junits.spi.*;
import java.io.*;
import java.util.Collection;

/**
 * Grid shared file system checkpoint SPI self test.
 */
@GridSpiTest(spi = GridSharedFsCheckpointSpi.class, group = "Checkpoint SPI")
public class GridSharedFsCheckpointSpiSelfTest extends GridCheckpointSpiAbstractTest<GridSharedFsCheckpointSpi> {
    /** */
    private static final String PATH = GridSharedFsCheckpointSpi.DFLT_DIR_PATH + "/" +
        GridSharedFsCheckpointSpiSelfTest.class.getSimpleName();

    /**
     * @return Paths.
     */
    @GridSpiTestConfig(setterName="setDirectoryPaths")
    public Collection<String> getDirectoryPaths() {
        return F.asList(PATH);
    }

    /**
     * @throws Exception If failed.
     */
    @Override protected void afterSpiStopped() throws Exception {
        File dir = new File(PATH);

        if (!dir.exists() || (!dir.isDirectory()))
            return;

        File[] files = dir.listFiles(new FileFilter() {
            @Override public boolean accept(File pathName) {
                return !pathName.isDirectory() && pathName.getName().endsWith(".gcp");
            }
        });

        if (files != null && files.length > 0)
            for (File file : files)
                file.delete();

        dir.delete();
    }
}
