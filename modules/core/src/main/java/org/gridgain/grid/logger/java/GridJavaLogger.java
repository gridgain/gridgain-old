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

import org.gridgain.grid.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.util.lang.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.logging.*;
import java.util.logging.Formatter;

import static java.util.logging.Level.*;
import static org.gridgain.grid.GridSystemProperties.*;

/**
 * Logger to use with Java logging. Implementation simply delegates to Java Logging.
 * <p>
 * Here is an example of configuring Java logger in GridGain configuration Spring
 * file to work over log4j implementation. Note that we use the same configuration file
 * as we provide by default:
 * <pre name="code" class="xml">
 *      ...
 *      &lt;property name="gridLogger"&gt;
 *          &lt;bean class="org.gridgain.grid.logger.java.GridJavaLogger"&gt;
 *              &lt;constructor-arg type="java.util.logging.Logger"&gt;
 *                  &lt;bean class="java.util.logging.Logger"&gt;
 *                      &lt;constructor-arg type="java.lang.String" value="global"/&gt;
 *                  &lt;/bean&gt;
 *              &lt;/constructor-arg&gt;
 *          &lt;/bean&gt;
 *      &lt;/property&gt;
 *      ...
 * </pre>
 * or
 * <pre name="code" class="xml">
 *      ...
 *      &lt;property name="gridLogger"&gt;
 *          &lt;bean class="org.gridgain.grid.logger.java.GridJavaLogger"/&gt;
 *      &lt;/property&gt;
 *      ...
 * </pre>
 * And the same configuration if you'd like to configure GridGain in your code:
 * <pre name="code" class="java">
 *      GridConfiguration cfg = new GridConfiguration();
 *      ...
 *      GridLogger log = new GridJavaLogger(Logger.global);
 *      ...
 *      cfg.setGridLogger(log);
 * </pre>
 * or which is actually the same:
 * <pre name="code" class="java">
 *      GridConfiguration cfg = new GridConfiguration();
 *      ...
 *      GridLogger log = new GridJavaLogger();
 *      ...
 *      cfg.setGridLogger(log);
 * </pre>
 * Please take a look at <a target=_new href="http://java.sun.com/j2se/1.4.2/docs/api20/java/util/logging/Logger.html>Logger javadoc</a>
 * for additional information.
 * <p>
 * It's recommended to use GridGain logger injection instead of using/instantiating
 * logger in your task/job code. See {@link GridLoggerResource} annotation about logger
 * injection.
 */
public class GridJavaLogger extends GridMetadataAwareAdapter implements GridLogger, GridLoggerNodeIdAware {
    /** */
    private static final long serialVersionUID = 0L;

    /** Maximum size of log file. */
    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024;

    /** Maximum number of files to use by logger. */
    private static final int MAX_BACKUP_IDX = 10;

    /** */
    private static final ThreadLocal<SimpleDateFormat> DATE_FORMATTER = new ThreadLocal<SimpleDateFormat>(){
        /** {@inheritDoc} */
        @Override protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("HH:mm:ss,SSS");
        }
    };

    /** Default log formatter. */
    private static final Formatter DFLT_FORMATTER = new Formatter() {
        /** {@inheritDoc} */
        @Override public String format(LogRecord record) {
            String threadName = Thread.currentThread().getName();

            String logName = record.getLoggerName();

            if (logName.contains("."))
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
    };

    /** */
    private static final Object mux = new Object();

    /** */
    private static volatile boolean inited;

    /** */
    private static volatile boolean quiet0;

    /** Java Logging implementation proxy. */
    @SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
    private Logger impl;

    /** Quiet flag. */
    private final boolean quiet;

    /** Node ID. */
    private volatile UUID nodeId;

    /**
     * Creates new logger.
     */
    public GridJavaLogger() {
        this(!isConfigured());
    }

    /**
     * Checks if logger is already configured within this VM or not.
     *
     * @return {@code True} if logger was already configured, {@code false} otherwise.
     */
    public static boolean isConfigured() {
        return System.getProperty("java.util.logging.config.file") != null;
    }

    /**
     * Creates new logger.
     *
     * @param init If {@code true}, then a default console appender will be created.
     *      If {@code false}, then no implicit initialization will take place,
     *      and java logger should be configured prior to calling this constructor.
     */
    public GridJavaLogger(boolean init) {
        impl = Logger.getLogger("");

        if (init) {
            // Implementation has already been inited, passing NULL.
            addDefaultHandlersIfNeeded(INFO, null);

            quiet = quiet0;
        }
        else
            quiet = true;
    }

    /**
     * Creates new logger with given implementation.
     *
     * @param impl Java Logging implementation to use.
     */
    public GridJavaLogger(final Logger impl) {
        assert impl != null;

        addDefaultHandlersIfNeeded(null, impl);

        quiet = quiet0;
    }

    /** {@inheritDoc} */
    @Override public GridLogger getLogger(Object ctgr) {
        return new GridJavaLogger(ctgr == null ? Logger.getLogger("") : Logger.getLogger(
            ctgr instanceof Class ? ((Class)ctgr).getName() : String.valueOf(ctgr)));
    }

    /**
     * Adds default logger handlers when needed.
     *
     * @param logLevel Optional log level.
     * @param initImpl Optional log implementation.
     */
    private void addDefaultHandlersIfNeeded(@Nullable Level logLevel, @Nullable Logger initImpl) {
        if (inited) {
            if (initImpl != null)
                // Do not init.
                impl = initImpl;

            return;
        }

        synchronized (mux) {
            if (inited) {
                if (initImpl != null)
                    // Do not init.
                    impl = initImpl;

                return;
            }

            if (initImpl != null)
                // Init logger impl.
                impl = initImpl;

            boolean quiet = Boolean.valueOf(System.getProperty(GG_QUIET, "true"));

            if (isConfigured()) {
                boolean consoleHndFound = findHandler(impl, ConsoleHandler.class) != null;

                // User configured console appender, thus log is not quiet.
                quiet0 = !consoleHndFound;
                inited = true;

                return;
            }

            if (Boolean.valueOf(System.getProperty(GG_CONSOLE_APPENDER, "true"))) {
                Handler[] handlers = Logger.getLogger("").getHandlers();

                // Remove predefined default console handler.
                if  (!F.isEmpty(handlers)) {
                    for (Handler h : handlers) {
                        if (h instanceof ConsoleHandler)
                            impl.removeHandler(h);
                    }
                }

                addDefaultConsoleHandler(impl, quiet ? SEVERE : INFO);

                if (logLevel != null)
                    impl.setLevel(logLevel);
            }

            quiet0 = quiet;
            inited = true;
        }
    }

    /**
     * Adds default console and file handlers.
     *
     * @param log Logger.
     * @param lvl From passed in level and higher.
     */
    private void addDefaultConsoleHandler(Logger log, Level lvl) {
        assert lvl != null;

        log.setLevel(INFO);

        if (F.isEmpty(log.getHandlers())) {
            ConsoleHandler consoleHnd = new ConsoleHandler();

            consoleHnd.setFormatter(DFLT_FORMATTER);

            consoleHnd.setLevel(lvl);

            log.addHandler(consoleHnd);
        }
    }

    /**
     * Adds default console and file handlers.
     *
     * @param log Logger.
     * @throws GridException If failed to configure logger.
     */
    private void addDefaultFileHandler(Logger log) throws GridException {
        assert Thread.holdsLock(mux);

        // Skip if file handler has been already configured.
        if (findHandler(impl, FileHandler.class) != null)
            return;

        try {
            File workDir = U.resolveWorkDirectory("log", false);

            String logFile = new File(workDir, "gridgain.log").getAbsolutePath();

            String filePtrn = logFile.replace("gridgain.log", "gridgain-" + U.id8(nodeId) + ".%g.log");

            FileHandler fileHnd = new FileHandler(filePtrn, MAX_FILE_SIZE, MAX_BACKUP_IDX);

            fileHnd.setLevel(INFO);

            fileHnd.setFormatter(DFLT_FORMATTER);

            log.addHandler(fileHnd);
        }
        catch (IOException e) {
            warning("Failed to configure default file logger.", e);
        }
    }

    /** {@inheritDoc} */
    @Override public void trace(String msg) {
        if (!impl.isLoggable(FINEST))
            warning("Logging at TRACE level without checking if TRACE level is enabled: " + msg);

        impl.finest(msg);
    }

    /** {@inheritDoc} */
    @Override public void debug(String msg) {
        if (!impl.isLoggable(FINE))
            warning("Logging at DEBUG level without checking if DEBUG level is enabled: " + msg);

        impl.fine(msg);
    }

    /** {@inheritDoc} */
    @Override public void info(String msg) {
        if (!impl.isLoggable(INFO))
            warning("Logging at INFO level without checking if INFO level is enabled: " + msg);

        impl.info(msg);
    }

    /** {@inheritDoc} */
    @Override public void warning(String msg) {
        impl.warning(msg);
    }

    /** {@inheritDoc} */
    @Override public void warning(String msg, @Nullable Throwable e) {
        impl.log(WARNING, msg, e);
    }

    /** {@inheritDoc} */
    @Override public void error(String msg) {
        impl.severe(msg);
    }

    /** {@inheritDoc} */
    @Override public boolean isQuiet() {
        return quiet;
    }

    /** {@inheritDoc} */
    @Override public void error(String msg, @Nullable Throwable e) {
        impl.log(SEVERE, msg, e);
    }

    /** {@inheritDoc} */
    @Override public boolean isTraceEnabled() {
        return impl.isLoggable(FINEST);
    }

    /** {@inheritDoc} */
    @Override public boolean isDebugEnabled() {
        return impl.isLoggable(FINE);
    }

    /** {@inheritDoc} */
    @Override public boolean isInfoEnabled() {
        return impl.isLoggable(INFO);
    }

    /** {@inheritDoc} */
    @Nullable @Override public String fileName() {
        FileHandler fileHnd = findHandler(impl, FileHandler.class);

        if (fileHnd != null) {
            try {
                return (String)U.field(fileHnd, "pattern");
            }
            catch (Exception ignored) {
                return null;
            }
        }

        return null;
    }

    /** {@inheritDoc} */
    @Override public void setNodeId(UUID nodeId) {
        A.notNull(nodeId, "nodeId");

        if (this.nodeId != null)
            return;

        synchronized (mux) {
            // Double check.
            if (this.nodeId == null) {
                this.nodeId = nodeId;

                try {
                    addDefaultFileHandler(impl);
                }
                catch (GridException e) {
                    throw new RuntimeException("Failed to create default file handler.", e);
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override public UUID getNodeId() {
        return nodeId;
    }

    /**
     * Returns first found handler of specified class type or {@code null} if that handler isn't configured.
     *
     * @param log Logger.
     * @param cls Class.
     * @param <T> Class type.
     * @return First found handler of specified class type or {@code null} if that handler isn't configured.
     */
    private static <T> T findHandler(Logger log, Class<T> cls) {
        while (log != null) {
            for (Handler hnd : log.getHandlers()) {
                if (cls.isInstance(hnd))
                    return (T)hnd;
            }

            log = log.getParent();
        }

        return null;
    }
}
