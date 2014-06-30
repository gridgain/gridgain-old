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

package org.gridgain.grid.util;

import org.jetbrains.annotations.*;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Utility class for debugging.
 */
public class GridDebug {
    /** */
    private static final AtomicReference<ConcurrentLinkedQueue<Item>> que =
        new AtomicReference<>(new ConcurrentLinkedQueue<Item>());

    /** */
    private static final SimpleDateFormat DEBUG_DATE_FMT = new SimpleDateFormat("HH:mm:ss,SSS");

    /** */
    private static final FileOutputStream out;

    /** */
    private static final Charset charset = Charset.forName("UTF-8");

    /**
     * On Ubuntu:
     * sudo mkdir /ramdisk
     * sudo mount -t tmpfs -o size=2048M tmpfs /ramdisk
     */
    private static final String LOGS_PATH = null;// "/ramdisk/";

    /** */
    private static boolean allowLog;

    /** */
    static {
        if (LOGS_PATH != null) {
            File log = new File(LOGS_PATH + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-").format(new Date()) +
                    ManagementFactory.getRuntimeMXBean().getName() + ".log");

            assert !log.exists();

            try {
                out = new FileOutputStream(log, false);
            }
            catch (FileNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }
        else {
            out = null;
        }
    }

    /**
     * Gets collected debug items queue.
     *
     * @return Items queue.
     */
    public static ConcurrentLinkedQueue<Item> queue() {
        return que.get();
    }

    /**
     * @param allow Write log.
     */
    public static synchronized void allowWriteLog(boolean allow) {
        allowLog = allow;
    }

    /**
     * Writes to log file which should reside on ram disk.
     *
     * @param x Data to log.
     */
    public static synchronized void write(Object ... x) {
        if (!allowLog)
            return;

        Thread th = Thread.currentThread();

        try {
            out.write((formatEntry(System.currentTimeMillis(), th.getName(), th.getId(), x) + "\n").getBytes(charset));
            out.flush();
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Add the data to debug queue.
     *
     * @param x Debugging data.
     */
    public static void debug(Object ... x) {
        ConcurrentLinkedQueue<Item> q = que.get();

        if (q != null)
            q.add(new Item(x));
    }

    /**
     * Hangs for 5 minutes if stopped.
     */
    public static void hangIfStopped() {
        if (que.get() == null)
            try {
                Thread.sleep(300000);
            }
            catch (InterruptedException ignored) {
                // No-op.
            }
    }

    /**
     * @return Object which will dump thread stack on toString call.
     */
    public static Object dumpStack() {
        final Throwable t = new Throwable();

        return new Object() {
            @Override public String toString() {
                StringWriter errors = new StringWriter();

                t.printStackTrace(new PrintWriter(errors));

                return errors.toString();
            }
        };
    }

    /**
     * Dump collected data to stdout.
     */
    public static void dump() {
        dump(que.get());
    }

    /**
     * Dump given queue to stdout.
     *
     * @param que Queue.
     */
    @SuppressWarnings("TypeMayBeWeakened")
    public static void dump(ConcurrentLinkedQueue<Item> que) {
        if (que == null)
            return;

        int start = -1;// que.size() - 5000;

        int x = 0;

        for (Item i : que) {
            if (x++ > start)
                System.out.println(i);
        }
    }

    /**
     * Dump existing queue to stdout and atomically replace it with null so that no subsequent logging is possible.
     *
     * @param x Parameters.
     * @return Empty string (useful for assertions like {@code assert x == 0 : D.dumpWithStop();} ).
     */
    public static String dumpWithStop(Object... x) {
        debug(x);
        return dumpWithReset(null);
    }

    /**
     * Dump existing queue to stdout and atomically replace it with new queue.
     *
     * @return Empty string (useful for assertions like {@code assert x == 0 : D.dumpWithReset();} ).
     */
    public static String dumpWithReset() {
        return dumpWithReset(new ConcurrentLinkedQueue<Item>());
    }

    /**
     * Dump existing queue to stdout and atomically replace it with given.
     *
     * @param q2 Queue.
     * @return Empty string.
     */
    private static String dumpWithReset(@Nullable ConcurrentLinkedQueue<Item> q2) {
        ConcurrentLinkedQueue<Item> q;

        do {
            q = que.get();

            if (q == null)
                break; // Stopped.
        }
        while (!que.compareAndSet(q, q2));

        dump(q);

        return "";
    }

    /**
     * Reset queue to empty one.
     */
    public static void reset() {
        ConcurrentLinkedQueue<Item> old = que.get();

        if (old != null) // Was not stopped.
            que.compareAndSet(old, new ConcurrentLinkedQueue<Item>());
    }

    /**
     * Formats log entry string.
     *
     * @param ts Timestamp.
     * @param threadName Thread name.
     * @param threadId Thread ID.
     * @param data Data.
     * @return String.
     */
    private static String formatEntry(long ts, String threadName, long threadId, Object... data) {
        return "<" + DEBUG_DATE_FMT.format(new Date(ts)) + "><~DBG~><" + threadName + " id:" + threadId + "> " +
            Arrays.toString(data);
    }

    /**
     * Debug info queue item.
     */
    @SuppressWarnings({"PublicInnerClass", "PublicField"})
    public static class Item {
        /** */
        public final long ts = System.currentTimeMillis();

        /** */
        public final String threadName;

        /** */
        public final long threadId;

        /** */
        public final Object[] data;

        /**
         * Constructor.
         *
         * @param data Debugging data.
         */
        public Item(Object[] data) {
            this.data = data;
            Thread th = Thread.currentThread();

            threadName = th.getName();
            threadId = th.getId();
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return formatEntry(ts, threadName, threadId, data);
        }
    }
}
