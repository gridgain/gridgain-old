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
import org.gridgain.grid.security.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.util.*;

import static org.gridgain.grid.kernal.GridProductImpl.ENT;

/**
 * Collects security information from node.
 */
@GridInternal
public class VisorSecurityCollectorTask extends VisorOneNodeTask<Void, Collection<GridSecuritySubject>> {
    /** */
    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @Override protected VisorSecurityCollectorJob job(@Nullable Void arg) {
        return new VisorSecurityCollectorJob(arg);
    }

    /**
     * Job that collect security information form node.
     */
    private static class VisorSecurityCollectorJob extends VisorJob<Void, Collection<GridSecuritySubject>> {
        /** */
        private static final long serialVersionUID = 0L;

        /**
         * @param arg Node ID to collect security information.
         */
        protected VisorSecurityCollectorJob(@Nullable Void arg) {
            super(arg);
        }

        /** {@inheritDoc} */
        @Override protected Collection<GridSecuritySubject> run(Void arg) throws GridException {
            if (ENT)
                return g.security().authenticatedSubjects();
            else
                return Collections.emptyList();
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(VisorSecurityCollectorJob.class, this);
        }
    }
}
