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

package org.gridgain.grid.logger.java;

import org.gridgain.grid.util.typedef.internal.*;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.logging.Formatter;
import java.util.logging.*;

/**
 * Formatter for JUL logger.
 */
public class GridJavaLoggerFormatter extends Formatter {
    /** Name for anonymous loggers. */
    public static final String ANONYMOUS_LOGGER_NAME = "UNKNOWN";

    /** */
    private static final ThreadLocal<SimpleDateFormat> DATE_FORMATTER = new ThreadLocal<SimpleDateFormat>() {
        /** {@inheritDoc} */
        @Override protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("HH:mm:ss,SSS");
        }
    };

    /** {@inheritDoc} */
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    @Override public String format(LogRecord record) {
        String threadName = Thread.currentThread().getName();

        String logName = record.getLoggerName();

        if (logName == null)
            logName = ANONYMOUS_LOGGER_NAME;
        else if (logName.contains("."))
            logName = logName.substring(logName.lastIndexOf('.') + 1);

        String ex = null;

        if (record.getThrown() != null) {
            StringWriter sw = new StringWriter();

            record.getThrown().printStackTrace(new PrintWriter(sw));

            String stackTrace = sw.toString();

            ex = "\n" + stackTrace;
        }

        return "[" + DATE_FORMATTER.get().format(new Date(record.getMillis())) + "][" +
            record.getLevel() + "][" +
            threadName + "][" +
            logName + "] " +
            record.getMessage() +
            (ex == null ? "\n" : ex);
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridJavaLoggerFormatter.class, this);
    }
}
