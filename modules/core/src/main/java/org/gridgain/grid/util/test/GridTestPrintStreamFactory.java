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

package org.gridgain.grid.util.test;

import java.io.*;

/**
 * Factory that allow to acquire/release Print Stream for test logging.
 */
public final class GridTestPrintStreamFactory {
    /** */
    private static final PrintStream sysOut = System.out;

    /** */
    private static final PrintStream sysErr = System.err;

    /** */
    private static GridTestPrintStream testOut;

    /** */
    private static GridTestPrintStream testErr;

    /** */
    private static long outCnt = 0;

    /** */
    private static long errCnt = 0;

    /**
     * Enforces singleton.
     */
    private GridTestPrintStreamFactory() {
        // No-op.
    }

    /**
     * Gets original standard out.
     *
     * @return Original standard out.
     */
    public static synchronized PrintStream getStdOut() {
        return sysOut;
    }

    /**
     * Gets original standard error.
     *
     * @return Original standard error.
     */
    public static synchronized PrintStream getStdErr() {
        return sysErr;
    }

    /**
     * Acquires output stream for logging tests.
     *
     * @return Junit out print stream.
     */
    public static synchronized GridTestPrintStream acquireOut() {
        // Lazy initialization is required here to ensure that parent
        // thread group is picked off correctly by implementation.
        if (testOut == null)
            testOut = new GridTestPrintStream(sysOut);

        if (outCnt == 0)
            System.setOut(testOut);

        outCnt++;

        return testOut;
    }

    /**
     * Acquires output stream for logging errors in tests.
     *
     * @return Junit error print stream.
     */
    public static synchronized GridTestPrintStream acquireErr() {
        // Lazy initialization is required here to ensure that parent
        // thread group is picked off correctly by implementation.
        if (testErr == null)
            testErr = new GridTestPrintStream(sysErr);

        if (errCnt == 0)
            System.setErr(testErr);

        errCnt++;

        return testErr;
    }

    /**
     * Releases standard out. If there are no more acquired standard outs,
     * then it is reset to its original value.
     */
    public static synchronized void releaseOut() {
        outCnt--;

        if (outCnt == 0)
            System.setOut(sysOut);
    }

    /**
     * Releases standard error. If there are no more acquired standard errors,
     * then it is reset to its original value.
     */
    public static synchronized void releaseErr() {
        errCnt--;

        if (errCnt == 0)
            System.setErr(sysErr);
    }
}
