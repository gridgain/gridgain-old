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

package org.gridgain.grid.marshaller.optimized;

import org.gridgain.grid.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jdk8.backport.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.math.*;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

import static org.gridgain.grid.marshaller.optimized.GridOptimizedMarshallerUtils.*;

/**
 * Resolves class names by serialVersionUID.
 */
@SuppressWarnings({"UnnecessaryFullyQualifiedName", "unchecked"})
class GridOptimizedClassResolver {
    /** File name to generate. */
    private static final String FILE_NAME = "optimized-classnames.properties";

    /** */
    private static final Map<String, Integer> ggxName2id = new HashMap<>();

    /** */
    private static final T2<Class<?>, GridOptimizedClassDescriptor>[] ggxId2name;

    /** */
    private static final Map<String, Integer> ggName2id = new HashMap<>();

    /** */
    private static final T3<String, Class<?>, GridOptimizedClassDescriptor>[] ggId2name;

    /** */
    private static Map<String, Integer> usrName2Id;

    /** */
    private static T3<String, Class<?>, GridOptimizedClassDescriptor>[] usrId2Name;

    /** */
    private static final int HEADER_NAME = 255;

    /** */
    private static final int HEADER_GG_NAME = 254;

    /** */
    private static final int HEADER_USER_NAME = 253;

    /** */
    private static final int HEADER_ARRAY = 252;

    /**
     * Initialize predefined classes to optimize.
     */
    static {
        Class[] superOptCls = new Class[] {
            // Array types.
            byte[].class,
            short[].class,
            int[].class,
            long[].class,
            float[].class,
            double[].class,
            boolean[].class,
            char[].class,

            // Boxed types.
            Byte.class,
            Short.class,
            Integer.class,
            Long.class,
            Float.class,
            Double.class,
            Boolean.class,
            Character.class,
            String.class,

            // Atomic.
            AtomicBoolean.class,AtomicInteger.class,
            AtomicLong.class,AtomicReference.class,
            AtomicMarkableReference.class,
            AtomicStampedReference.class,
            AtomicIntegerArray.class,
            AtomicReferenceArray.class,

            // Concurrent types.
            ConcurrentHashMap.class,
            ConcurrentLinkedQueue.class,
            ConcurrentSkipListMap.class,
            ConcurrentSkipListSet.class,
            LinkedBlockingDeque.class,
            LinkedBlockingQueue.class,
            PriorityBlockingQueue.class,
            CopyOnWriteArrayList.class,
            CopyOnWriteArraySet.class,

            // Locks.
            ReentrantLock.class,
            ReentrantReadWriteLock.class,
            ReentrantReadWriteLock.ReadLock.class,
            ReentrantReadWriteLock.WriteLock.class,

            // Util types.
            Date.class,
            UUID.class,
            Calendar.class,
            Random.class,
            Calendar.class,
            Currency.class,
            ArrayList.class,
            LinkedList.class,
            Stack.class,
            Vector.class,
            HashMap.class,
            HashSet.class,
            Hashtable.class,
            TreeMap.class,
            TreeSet.class,
            IdentityHashMap.class,
            LinkedHashMap.class,
            LinkedHashSet.class,
            ArrayDeque.class,
            BitSet.class,
            EnumMap.class,
            EnumSet.class,

            // SQL types.
            java.sql.Date.class,
            Time.class,
            Timestamp.class,

            // Math types.
            BigDecimal.class,
            BigInteger.class,

            // GridGain types.
            GridUuid.class,
            GridBoundedConcurrentOrderedSet.class,
            GridBoundedLinkedHashSet.class,
            GridConcurrentHashSet.class,
            ConcurrentLinkedDeque8.class,
            GridConcurrentPhantomHashSet.class,
            GridConcurrentSkipListSet.class,
            GridConcurrentWeakHashSet.class,
            GridIdentityHashSet.class,
            GridLeanSet.class,
            GridSetWrapper.class
        };

        // Have to leave a range for special purposes.
        assert superOptCls.length < 230;

        ggxId2name = new T2[superOptCls.length];

        for (int i = 0; i < superOptCls.length; i++) {
            Class cls = superOptCls[i];

            ggxName2id.put(cls.getName(), i);
            ggxId2name[i] = new T2<Class<?>, GridOptimizedClassDescriptor>(cls, null);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(
            GridOptimizedClassResolver.class.getResourceAsStream(FILE_NAME),
            GridOptimizedMarshallerUtils.UTF_8));

        List<T3<String, Class<?>, GridOptimizedClassDescriptor>> ggId2name0 =
            new LinkedList<>();

        try {
            for (int i = 0; ; i++) {
                String clsName = reader.readLine();

                if (clsName == null)
                    break;

                ggName2id.put(clsName, i);
                ggId2name0.add(new T3<String, Class<?>, GridOptimizedClassDescriptor>(clsName, null, null));
            }

            ggId2name = ggId2name0.toArray(new T3[ggId2name0.size()]);
        }
        catch (IOException e) {
            throw new AssertionError(e);
        }
        finally {
            U.close(reader, null);
        }
    }

    /**
     * Ensure singleton.
     */
    private GridOptimizedClassResolver() {
        // No-op.
    }

    /**
     * @param usrName2id0 From name to ID.
     * @param usrId2Name0 From ID to name.
     */
    static void userClasses(@Nullable Map<String, Integer> usrName2id0,
        @Nullable T3<String, Class<?>, GridOptimizedClassDescriptor>[] usrId2Name0) {
        usrName2Id = usrName2id0;
        usrId2Name = usrId2Name0;
    }

    /**
     * @param in DataInput to read from.
     * @param clsLdr ClassLoader.
     * @return Class descriptor.
     * @throws IOException If serial version UID failed.
     * @throws ClassNotFoundException If the class cannot be located by the specified class loader.
     */
    static GridOptimizedClassDescriptor readClass(DataInput in, ClassLoader clsLdr)
        throws IOException, ClassNotFoundException {
        assert in != null;
        assert clsLdr != null;

        int hdr = in.readByte() & 0xff;

        if (hdr < ggxId2name.length) {
            T2<Class<?>, GridOptimizedClassDescriptor> ggxT = ggxId2name[hdr];

            GridOptimizedClassDescriptor desc = ggxT.get2();

            if (desc == null) {
                desc = classDescriptor(ggxT.get1(), null);

                ggxT.set2(desc);
            }

            return desc;
        }

        String name;
        Class<?> cls;
        GridOptimizedClassDescriptor desc;

        switch (hdr) {
            case HEADER_GG_NAME:
                int ggId = in.readInt();

                T3<String, Class<?>, GridOptimizedClassDescriptor> ggT;

                try {
                    ggT = ggId2name[ggId];
                }
                catch (ArrayIndexOutOfBoundsException e) {
                    throw new ClassNotFoundException("Failed to find optimized class ID " +
                        "(is same GridGain version running on all nodes?): " + ggId, e);
                }

                name = ggT.get1();
                cls = ggT.get2();
                desc = ggT.get3();

                if (desc == null) {
                    if (clsLdr == U.gridClassLoader()) {
                        if (cls == null) {
                            cls = forName(name, clsLdr);

                            ggT.set2(cls);
                        }

                        desc = classDescriptor(cls, null);

                        ggT.set3(desc);
                    }
                    else {
                        cls = forName(name, clsLdr);

                        desc = classDescriptor(cls, null);
                    }
                }

                break;

            case HEADER_USER_NAME:
                int usrId = in.readInt();

                T3<String, Class<?>, GridOptimizedClassDescriptor> usrT;

                try {
                    if (usrId2Name != null)
                        usrT = usrId2Name[usrId];
                    else
                        throw new ClassNotFoundException("Failed to find user defined class ID " +
                            "(make sure to register identical classes on all nodes for optimization): " + usrId);
                }
                catch (ArrayIndexOutOfBoundsException e) {
                    throw new ClassNotFoundException("Failed to find user defined class ID " +
                        "(make sure to register identical classes on all nodes for optimization): " + usrId, e);
                }

                name = usrT.get1();
                cls = usrT.get2();
                desc = usrT.get3();

                if (desc == null) {
                    if (cls == null) {
                        cls = forName(name, clsLdr);

                        usrT.set2(cls);
                    }

                    desc = classDescriptor(cls, null);

                    usrT.set3(desc);
                }

                break;

            case HEADER_ARRAY:
                name = readClass(in, clsLdr).name();

                name = name.charAt(0) == '[' ? "[" + name : "[L" + name + ';';

                cls = forName(name, clsLdr);

                return classDescriptor(cls, null);

            case HEADER_NAME:
                name = in.readUTF();

                cls = forName(name, clsLdr);

                desc = classDescriptor(cls, null);

                break;

            default:
                throw new IOException("Unexpected optimized stream header: " + hdr);
        }

        short actual = desc.shortId();

        short exp = in.readShort();

        if (actual != exp)
            throw new ClassNotFoundException("Optimized stream class checksum mismatch " +
                "(is same version of marshalled class present on all nodes?) " +
                "[expected=" + exp + ", actual=" + actual + ", cls=" + cls + ']');

        return desc;
    }

    /**
     * @param out Output.
     * @param desc Class descriptor.
     * @throws IOException In case of error.
     */
    static void writeClass(DataOutput out, GridOptimizedClassDescriptor desc) throws IOException {
        assert out != null;
        assert desc != null;

        int hdr = desc.header();

        out.writeByte(hdr);

        switch (hdr) {
            case HEADER_GG_NAME:
            case HEADER_USER_NAME:
                out.writeInt(desc.id());
                out.writeShort(desc.shortId());

                return;

            case HEADER_ARRAY:
                writeClass(out, classDescriptor(desc.componentType(), null));

                return;

            case HEADER_NAME:
                out.writeUTF(desc.name());
                out.writeShort(desc.shortId());
        }
    }

    /**
     * @param cls Class to write.
     * @return Data for {@code writeClass} method.
     */
    static T2<Integer, Integer> writeClassData(Class<?> cls) {
        assert cls != null;

        String name = cls.getName();

        Integer superHdr = ggxName2id.get(name);

        if (superHdr != null)
            return new T2<>(superHdr, null);

        Integer id;

        if ((id = ggName2id.get(name)) != null)
            return new T2<>(HEADER_GG_NAME, id);

        if (usrName2Id != null && (id = usrName2Id.get(name)) != null)
            return new T2<>(HEADER_USER_NAME, id);

        if (cls.isArray())
            return new T2<>(HEADER_ARRAY, null);

        return new T2<>(HEADER_NAME, null);
    }

    /**
     * @param name Class name.
     * @param ldr Class loader.
     * @return Class.
     * @throws ClassNotFoundException If class not found.
     */
    private static Class<?> forName(String name, ClassLoader ldr) throws ClassNotFoundException {
        Class<?> cls = primitive(name);

        if (cls == null)
            cls = GridOptimizedMarshallerUtils.forName(name, ldr);

        return cls;
    }

    /**
     * @param name Name of primitive class.
     * @return Primitive type class or null.
     */
    @SuppressWarnings("TypeMayBeWeakened")
    @Nullable private static Class<?> primitive(String name) {
        if (name.length() > 7)
            return null;

        switch (name.charAt(0)) {
            case 'b':
                if ("boolean".equals(name))
                    return boolean.class;

                return "byte".equals(name) ? byte.class : null;
            case 's':
                return "short".equals(name) ? short.class : null;
            case 'i':
                return "int".equals(name) ? int.class : null;
            case 'l':
                return "long".equals(name) ? long.class : null;
            case 'c':
                return "char".equals(name) ? char.class : null;
            case 'f':
                return "float".equals(name) ? float.class : null;
            case 'd':
                return "double".equals(name) ? double.class : null;
            case 'v':
                return "void".equals(name) ? void.class : null;
        }

        return null;
    }
}
