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

package org.gridgain.grid.kernal.visor.cmd.tasks;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.processors.task.*;
import org.gridgain.grid.kernal.visor.cmd.*;
import org.gridgain.grid.kernal.visor.cmd.dto.*;
import org.gridgain.grid.kernal.visor.cmd.tasks.VisorQueryTask.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.util.*;

import static org.gridgain.grid.kernal.visor.cmd.tasks.VisorQueryUtils.*;

/**
 *  Task for collecting next page previously executed SQL or SCAN query.
 */
@GridInternal
public class VisorQueryNextPageTask extends VisorOneNodeTask<GridBiTuple<String, Integer>, VisorQueryResult> {
    /** */
    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @Override protected VisorQueryNextPageJob job(GridBiTuple<String, Integer> arg) {
        return new VisorQueryNextPageJob(arg);
    }

    /**
     * Job for collecting next page previously executed SQL or SCAN query.
     */
    private static class VisorQueryNextPageJob extends VisorJob<GridBiTuple<String, Integer>, VisorQueryResult> {
        /** */
        private static final long serialVersionUID = 0L;

        /**
         * Create job with specified argument.
         *
         * @param arg Job argument.
         */
        private VisorQueryNextPageJob(GridBiTuple<String, Integer> arg) {
            super(arg);
        }

        /** {@inheritDoc} */
        @Override protected VisorQueryResult run(GridBiTuple<String, Integer> arg) throws GridException {
            return arg.get1().startsWith(SCAN_QRY_NAME) ? nextScanPage(arg) : nextSqlPage(arg);
        }

        /** Collect data from SQL query */
        private VisorQueryResult nextSqlPage(GridBiTuple<String, Integer> arg) throws GridException {
            GridNodeLocalMap<String, VisorFutureResultSetHolder<List<?>>> storage = g.nodeLocalMap();

            VisorFutureResultSetHolder<List<?>> t = storage.get(arg.get1());

            if (t == null)
                throw new GridInternalException("SQL query results are expired.");

            GridBiTuple<List<Object[]>, List<?>> nextRows = fetchSqlQueryRows(t.future(), t.next(), arg.get2());

            boolean hasMore = nextRows.get2() != null;

            if (hasMore)
                storage.put(arg.get1(), new VisorFutureResultSetHolder<>(t.future(), nextRows.get2(), true));
            else
                storage.remove(arg.get1());

            return new VisorQueryResult(nextRows.get1(), hasMore);
        }

        /** Collect data from SCAN query */
        private VisorQueryResult nextScanPage(GridBiTuple<String, Integer> arg) throws GridException {
            GridNodeLocalMap<String, VisorFutureResultSetHolder<Map.Entry<Object, Object>>> storage = g.nodeLocalMap();

            VisorFutureResultSetHolder<Map.Entry<Object, Object>> t = storage.get(arg.get1());

            if (t == null)
                throw new GridInternalException("Scan query results are expired.");

            GridBiTuple<List<Object[]>, Map.Entry<Object, Object>> rows =
                fetchScanQueryRows(t.future(), t.next(), arg.get2());

            Boolean hasMore = rows.get2() != null;

            if (hasMore)
                storage.put(arg.get1(), new VisorFutureResultSetHolder<>(t.future(), rows.get2(), true));
            else
                storage.remove(arg.get1());

            return new VisorQueryResult(rows.get1(), hasMore);
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(VisorQueryNextPageJob.class, this);
        }
    }
}
