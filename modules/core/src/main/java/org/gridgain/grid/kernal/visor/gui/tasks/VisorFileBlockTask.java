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
import org.gridgain.grid.kernal.processors.task.*;
import org.gridgain.grid.kernal.visor.cmd.*;
import org.gridgain.grid.kernal.visor.gui.dto.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.io.*;
import java.net.*;
import java.nio.file.*;

import static org.gridgain.grid.kernal.visor.gui.VisorTaskUtilsEnt.*;

/**
 * Task to read file block.
 */
@GridInternal
public class VisorFileBlockTask extends VisorOneNodeTask<VisorFileBlockTask.VisorFileBlockArg,
    GridBiTuple<? extends IOException, VisorFileBlock>> {
    /** */
    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @Override protected VisorFileBlockJob job(VisorFileBlockArg arg) {
        return new VisorFileBlockJob(arg);
    }

    /**
     * Arguments for {@link VisorFileBlockTask}
     */
    @SuppressWarnings("PublicInnerClass")
    public static class VisorFileBlockArg implements Serializable {
        /** */
        private static final long serialVersionUID = 0L;

        /** Log file path. */
        private final String path;

        /** Log file offset. */
        private final long offset;

        /** Block size. */
        private final int blockSz;

        /** Log file last modified timestamp. */
        private final long lastModified;

        /**
         * @param path Log file path.
         * @param offset Offset in file.
         * @param blockSz Block size.
         * @param lastModified Log file last modified timestamp.
         */
        public VisorFileBlockArg(String path, long offset, int blockSz, long lastModified) {
            this.path = path;
            this.offset = offset;
            this.blockSz = blockSz;
            this.lastModified = lastModified;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(VisorFileBlockArg.class, this);
        }
    }

    /**
     * Job that read file block on node.
     */
    private static class VisorFileBlockJob
        extends VisorJob<VisorFileBlockArg, GridBiTuple<? extends IOException, VisorFileBlock>> {
        /** */
        private static final long serialVersionUID = 0L;

        /**
         * @param arg Descriptor of file block to read.
         */
        private VisorFileBlockJob(VisorFileBlockArg arg) {
            super(arg);
        }

        /** {@inheritDoc} */
        @Override protected GridBiTuple<? extends IOException, VisorFileBlock> run(VisorFileBlockArg arg) throws GridException {
            try {
                URL url = U.resolveGridGainUrl(arg.path);

                if (url == null)
                    return new GridBiTuple<>(new NoSuchFileException("File path not found: " + arg.path), null);

                VisorFileBlock block = readBlock(new File(url.toURI()), arg.offset, arg.blockSz, arg.lastModified);

                return new GridBiTuple<>(null, block);
            }
            catch (IOException e) {
                return new GridBiTuple<>(e, null);
            }
            catch (URISyntaxException ignored) {
                return new GridBiTuple<>(new NoSuchFileException("File path not found: " + arg.path), null);
            }
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(VisorFileBlockJob.class, this);
        }
    }
}
