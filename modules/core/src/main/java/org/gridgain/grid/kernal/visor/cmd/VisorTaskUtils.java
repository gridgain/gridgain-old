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

package org.gridgain.grid.kernal.visor.cmd;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

import static java.lang.System.*;

/**
 * Contains utility methods for Visor tasks and jobs.
 */
public class VisorTaskUtils {
    /** Default substitute for {@code null} names. */
    private static final String DFLT_EMPTY_NAME = "<default>";

    /** Debug date format. */
    private static final ThreadLocal<SimpleDateFormat> DEBUG_DATE_FMT = new ThreadLocal<SimpleDateFormat>() {
        /** {@inheritDoc} */
        @Override protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("HH:mm:ss,SSS");
        }
    };

    /** Visor debug task flag. */
    private static final String VISOR_DEBUG_KEY = "VISOR_DEBUG_KEY";

    /**
     * @param name Grid-style nullable name.
     * @return Name with {@code null} replaced to &lt;default&gt;.
     */
    public static String escapeName(@Nullable String name) {
        return name == null ? DFLT_EMPTY_NAME : name;
    }

    /**
     * @param a First name.
     * @param b Second name.
     * @return {@code true} if both names equals.
     */
    public static boolean safeEquals(@Nullable String a, @Nullable String b) {
        return (a != null && b != null) ? a.equals(b) : (a == null && b == null);
    }

    /**
     * Concat arrays in one.
     *
     * @param arrays Arrays.
     * @return Summary array.
     */
    public static int[] concat(int[] ... arrays) {
        assert arrays != null;
        assert arrays.length > 1;

        int length = 0;

        for (int[] a : arrays)
            length += a.length;

        int[] r = Arrays.copyOf(arrays[0], length);

        for (int i = 1, shift = 0; i < arrays.length; i++) {
            shift += arrays[i - 1].length;
            System.arraycopy(arrays[i], 0, r, shift, arrays[i].length);
        }

        return r;
    }

    /**
     * Returns compact class host.
     *
     * @param obj Object to compact.
     * @return String.
     */
    @Nullable public static Object compactObject(Object obj) {
        if (obj == null)
            return null;

        if (obj instanceof Enum)
            return obj.toString();

        if (obj instanceof String || obj instanceof Boolean || obj instanceof Number)
            return obj;

        if (obj instanceof Collection) {
            Collection col = (Collection)obj;

            Object[] res = new Object[col.size()];

            int i = 0;

            for (Object elm : col) {
                res[i++] = compactObject(elm);
            }

            return res;
        }

        if (obj.getClass().isArray()) {
            Class<?> arrType = obj.getClass().getComponentType();

            if (arrType.isPrimitive()) {
                if (obj instanceof boolean[])
                    return Arrays.toString((boolean[])obj);
                if (obj instanceof byte[])
                    return Arrays.toString((byte[])obj);
                if (obj instanceof short[])
                    return Arrays.toString((short[])obj);
                if (obj instanceof int[])
                    return Arrays.toString((int[])obj);
                if (obj instanceof long[])
                    return Arrays.toString((long[])obj);
                if (obj instanceof float[])
                    return Arrays.toString((float[])obj);
                if (obj instanceof double[])
                    return Arrays.toString((double[])obj);
            }

            Object[] arr = (Object[])obj;

            int iMax = arr.length - 1;

            StringBuilder sb = new StringBuilder("[");

            for (int i = 0; i <= iMax; i++) {
                sb.append(compactObject(arr[i]));

                if (i != iMax)
                    sb.append(", ");
            }

            sb.append("]");

            return sb.toString();
        }

        return U.compact(obj.getClass().getName());
    }

    /**
     * Compact class names.
     *
     * @param obj Object for compact.
     * @return Compacted string.
     */
    @Nullable public static String compactClass(Object obj) {
        if (obj == null)
            return null;

        return U.compact(obj.getClass().getName());
    }

    /**
     * Joins array elements to string.
     *
     * @param arr Array.
     * @return String.
     */
    @Nullable public static String compactArray(Object[] arr) {
        if (arr == null || arr.length == 0)
            return null;

        String sep = ", ";

        StringBuilder sb = new StringBuilder();

        for (Object s: arr)
            sb.append(s).append(sep);

        if (sb.length() > 0)
            sb.setLength(sb.length() - sep.length());

        return U.compact(sb.toString());
    }

    /**
     * Returns boolean value from system property or provided function.
     *
     * @param propName System property name.
     * @param dflt Function that returns {@code Integer}.
     * @return {@code Integer} value
     */
    public static Integer intValue(String propName, Integer dflt) {
        String sysProp = getProperty(propName);

        return (sysProp != null && !sysProp.isEmpty()) ? Integer.getInteger(sysProp) : dflt;
    }

    /**
     * Returns boolean value from system property or provided function.
     *
     * @param propName System property host.
     * @param dflt Function that returns {@code Boolean}.
     * @return {@code Boolean} value
     */
    public static boolean boolValue(String propName, boolean dflt) {
        String sysProp = getProperty(propName);

        return (sysProp != null && !sysProp.isEmpty()) ? Boolean.getBoolean(sysProp) : dflt;
    }

    /**
     * Pretty-formatting for duration.
     *
     * @param ms Millisecond to format.
     * @return Formatted presentation.
     */
    private static String formatDuration(long ms) {
        assert ms >= 0;

        if (ms == 0)
            return "< 1 ms";

        SB sb = new SB();

        long dd = ms / 1440000; // 1440 mins = 60 mins * 24 hours

        if (dd > 0)
            sb.a(dd).a(dd == 1 ? " day " : " days ");

        ms %= 1440000;

        long hh = ms / 60000;

        if (hh > 0)
            sb.a(hh).a(hh == 1 ? " hour " : " hours ");

        long min = ms / 60000;

        if (min > 0)
            sb.a(min).a(min == 1 ? " min " : " mins ");

        ms %= 60000;

        if (ms > 0)
            sb.a(ms).a(" ms ");

        return sb.toString().trim();
    }

    /**
     *
     * @param log Logger.
     * @param time Time.
     * @param msg Message.
     */
    private static void log0(@Nullable GridLogger log, long time, String msg) {
        if (log != null) {
            if (log.isDebugEnabled())
                log.debug(msg);
            else
                log.warning(msg);
        }
        else
            X.println("[" + DEBUG_DATE_FMT.get().format(time) + "]" +
                String.format("%30s %s", "<" + Thread.currentThread().getName() + ">", msg));
    }

    /**
     * Log start.
     *
     * @param log Logger.
     * @param clazz Class.
     * @param start Start time.
     */
    public static void logStart(@Nullable GridLogger log, Class<?> clazz, long start) {
        log0(log, start, "[" + clazz.getSimpleName() + "]: STARTED");
    }

    /**
     * Log finished.
     *
     * @param log Logger.
     * @param clazz Class.
     * @param start Start time.
     */
    public static void logFinish(@Nullable GridLogger log, Class<?> clazz, long start) {
        final long end = U.currentTimeMillis();

        log0(log, end, String.format("[%s]: FINISHED, duration: %s", clazz.getSimpleName(), formatDuration(end - start)));
    }

    /**
     * Log task mapped.
     *
     * @param log Logger.
     * @param clazz Task class.
     * @param nodes Mapped nodes.
     */
    public static void logMapped(@Nullable GridLogger log, Class<?> clazz, Collection<GridNode> nodes) {
        log0(log, U.currentTimeMillis(),
            String.format("[%s]: MAPPED: %s", clazz.getSimpleName(), U.toShortString(nodes)));
    }

    /**
     * Log message.
     *
     * @param log Logger.
     * @param clazz class.
     * @param start start time.
     */
    public static long log(@Nullable GridLogger log, String msg, Class<?> clazz, long start) {
        final long end = U.currentTimeMillis();

        log0(log, end, String.format("[%s]: %s, duration: %s", clazz.getSimpleName(), msg, formatDuration(end - start)));

        return end;
    }

    /**
     * @param g Grid to check for debug flag.
     * @return {@code true} if debug enabled.
     * @throws GridException If get operation failed.
     */
    public static boolean debugState(GridEx g) throws GridException {
        Boolean debug = g.localNode().isDaemon()
            ? g.<String, Boolean>nodeLocalMap().get(VISOR_DEBUG_KEY)
            : g.<String, Boolean>cachex(CU.UTILITY_CACHE_NAME).get(VISOR_DEBUG_KEY);

        return debug != null ? debug : false;
    }

    /**
     * Set grid debug state.
     *
     * @param g Grid to set debug flag.
     * @param newState New value for debug state.
     * @throws GridException If get operation failed.
     */
    public static void debugState(GridEx g, Boolean newState) throws GridException {
        if (g.localNode().isDaemon())
            g.<String, Boolean>nodeLocalMap().put(VISOR_DEBUG_KEY, newState);
        else
            g.<String, Boolean>cachex(CU.UTILITY_CACHE_NAME).putx(VISOR_DEBUG_KEY, newState);
    }

    /**
     * Checks if address can be reached using one argument InetAddress.isReachable() version or ping command if failed.
     *
     * @param addr Address to check.
     * @param reachTimeout Timeout for the check.
     * @return {@code True} if address is reachable.
     */
    public static boolean reachableByPing(InetAddress addr, int reachTimeout) {
        try {
            if (addr.isReachable(reachTimeout))
                return true;

            String cmd = String.format("ping -%s 1 %s", U.isWindows() ? "n" : "c", addr.getHostAddress());

            Process myProcess = Runtime.getRuntime().exec(cmd);

            myProcess.waitFor();

            return myProcess.exitValue() == 0;
        }
        catch (IOException ignore) {
            return false;
        }
        catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();

            return false;
        }
    }

    /**
     * Run command in separated console.
     *
     * @param args A string array containing the program and its arguments.
     * @return Started process.
     */
    public static Process openInConsole(String... args) throws IOException {
        return openInConsole(null, args);
    }

    /**
     * Run command in separated console.
     *
     * @param workFolder Work folder for command.
     * @param args A string array containing the program and its arguments.
     * @return Started process.
     * @throws IOException If failed to start process.
     */
    public static Process openInConsole(@Nullable File workFolder, String... args)
        throws IOException {
        String[] commands = args;

        String cmd = F.concat(Arrays.asList(args), " ");

        if (U.isWindows())
            commands = F.asArray("cmd", "/c", String.format("start %s", cmd));

        if (U.isMacOs())
            commands = F.asArray("osascript", "-e",
                String.format("tell application \"Terminal\" to do script \"%s\"", cmd));

        if (U.isUnix())
            commands = F.asArray("xterm", "-sl", "1024", "-geometry", "200x50", "-e", cmd);

        ProcessBuilder pb = new ProcessBuilder(commands);

        if (workFolder != null)
            pb.directory(workFolder);

        return pb.start();
    }
}
