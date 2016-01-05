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

package org.gridgain.grid.kernal.visor.gui.tasks;

import org.gridgain.grid.*;
import org.gridgain.grid.ggfs.*;
import org.gridgain.grid.kernal.processors.ggfs.*;
import org.gridgain.grid.kernal.visor.cmd.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.io.*;
import java.net.*;
import java.nio.file.*;

import static org.gridgain.grid.ggfs.GridGgfsConfiguration.*;

/**
 * Contains utility methods for Visor tasks and jobs.
 */
@SuppressWarnings("ExtendsUtilityClass")
public class VisorGgfsTaskUtils extends VisorTaskUtils {
    /**
     * Resolve GGFS profiler logs directory.
     *
     * @param ggfs GGFS instance to resolve logs dir for.
     * @return {@link Path} to log dir or {@code null} if not found.
     * @throws GridException if failed to resolve.
     */
    public static Path resolveGgfsProfilerLogsDir(GridGgfs ggfs) throws GridException {
        String logsDir;

        if (ggfs instanceof GridGgfsEx)
            logsDir = ((GridGgfsEx) ggfs).clientLogDirectory();
        else if (ggfs == null)
            throw new GridException("Failed to get profiler log folder (GGFS instance not found)");
        else
            throw new GridException("Failed to get profiler log folder (unexpected GGFS instance type)");

        URL logsDirUrl = U.resolveGridGainUrl(logsDir != null ? logsDir : DFLT_GGFS_LOG_DIR);

        return logsDirUrl != null ? new File(logsDirUrl.getPath()).toPath() : null;
    }
}
