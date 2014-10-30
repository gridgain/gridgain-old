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

package org.gridgain.grid.kernal.processors.rest;

import net.spy.memcached.*;
import org.gridgain.grid.*;
import org.gridgain.grid.util.typedef.*;
import org.junit.*;

import java.io.*;
import java.net.*;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests for TCP binary protocol.
 */
public class GridClientMemcachedProtocolSelfTest extends GridAbstractRestProcessorSelfTest {
    /** Grid count. */
    private static final int GRID_CNT = 1;

    /** Custom port. */
    private Integer customPort;

    /** Memcache client. */
    private MemcachedClientIF client;

    /** {@inheritDoc} */
    @Override protected int gridCount() {
        return GRID_CNT;
    }

    /**
     * @return Memcache client.
     * @throws Exception If start failed.
     */
    private MemcachedClientIF startClient() throws Exception {
        int port = customPort != null ? customPort : GridConfiguration.DFLT_TCP_PORT;

        return new MemcachedClient(new BinaryConnectionFactory(),
            F.asList(new InetSocketAddress(LOC_HOST, port)));
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        super.beforeTest();

        client = startClient();
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        super.afterTest();

        client.shutdown();

        customPort = null;
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        assert cfg.getClientConnectionConfiguration() != null;

        if (customPort != null)
            cfg.getClientConnectionConfiguration().setRestTcpPort(customPort);

        return cfg;
    }

    /**
     * @throws Exception If failed.
     */
    public void testGet() throws Exception {
        Assert.assertTrue(cache().putx("getKey1", "getVal1"));
        Assert.assertTrue(cache().putx("getKey2", "getVal2"));

        Assert.assertEquals("getVal1", client.get("getKey1"));
        Assert.assertEquals("getVal2", client.get("getKey2"));
        Assert.assertNull(client.get("wrongKey"));
    }

    /**
     * @throws Exception If failed.
     */
    public void testGetBulk() throws Exception {
        Assert.assertTrue(cache().putx("getKey1", "getVal1"));
        Assert.assertTrue(cache().putx("getKey2", "getVal2"));
        Assert.assertTrue(cache().putx("getKey3", "getVal3"));

        Map<String, Object> map = client.getBulk("getKey1", "getKey2");

        Assert.assertEquals(2, map.size());

        Assert.assertEquals("getVal1", map.get("getKey1"));
        Assert.assertEquals("getVal2", map.get("getKey2"));
    }

    /**
     * @throws Exception If failed.
     */
    public void testSet() throws Exception {
        Assert.assertTrue(client.set("setKey", 0, "setVal").get());

        assertEquals("setVal", cache().get("setKey"));
    }

    /**
     * @throws Exception If failed.
     */
    public void testSetWithExpiration() throws Exception {
        Assert.assertTrue(client.set("setKey", 2000, "setVal").get());

        assertEquals("setVal", cache().get("setKey"));

        Thread.sleep(2100);

        Assert.assertNull(cache().get("setKey"));
    }

    /**
     * @throws Exception If failed.
     */
    public void testAdd() throws Exception {
        Assert.assertTrue(cache().putx("addKey1", "addVal1"));

        Assert.assertFalse(client.add("addKey1", 0, "addVal1New").get());
        Assert.assertTrue(client.add("addKey2", 0, "addVal2").get());

        assertEquals("addVal1", cache().get("addKey1"));
        assertEquals("addVal2", cache().get("addKey2"));
    }

    /**
     * @throws Exception If failed.
     */
    public void testAddWithExpiration() throws Exception {
        Assert.assertTrue(client.add("addKey", 2000, "addVal").get());

        assertEquals("addVal", cache().get("addKey"));

        Thread.sleep(2100);

        Assert.assertNull(cache().get("addKey"));
    }

    /**
     * @throws Exception If failed.
     */
    public void testReplace() throws Exception {
        Assert.assertFalse(client.replace("replaceKey", 0, "replaceVal").get());

        Assert.assertNull(cache().get("replaceKey"));
        Assert.assertTrue(cache().putx("replaceKey", "replaceVal"));

        Assert.assertTrue(client.replace("replaceKey", 0, "replaceValNew").get());

        assertEquals("replaceValNew", cache().get("replaceKey"));
    }

    /**
     * @throws Exception If failed.
     */
    public void testReplaceWithExpiration() throws Exception {
        Assert.assertTrue(cache().putx("replaceKey", "replaceVal"));

        Assert.assertTrue(client.set("replaceKey", 2000, "replaceValNew").get());

        assertEquals("replaceValNew", cache().get("replaceKey"));

        Thread.sleep(2100);

        Assert.assertNull(cache().get("replaceKey"));
    }

    /**
     * @throws Exception If failed.
     */
    public void testDelete() throws Exception {
        Assert.assertFalse(client.delete("deleteKey").get());

        Assert.assertTrue(cache().putx("deleteKey", "deleteVal"));

        Assert.assertTrue(client.delete("deleteKey").get());

        Assert.assertNull(cache().get("deleteKey"));
    }

    /**
     * @throws Exception If failed.
     */
    public void testIncrement() throws Exception {
        Assert.assertEquals(5, client.incr("incrKey", 3, 2));

        assertEquals(5, cache().dataStructures().atomicLong("incrKey", 0, true).get());

        Assert.assertEquals(15, client.incr("incrKey", 10, 0));

        assertEquals(15, cache().dataStructures().atomicLong("incrKey", 0, true).get());
    }

    /**
     * @throws Exception If failed.
     */
    public void testDecrement() throws Exception {
        Assert.assertEquals(5, client.decr("decrKey", 10, 15));

        assertEquals(5, cache().dataStructures().atomicLong("decrKey", 0, true).get());

        Assert.assertEquals(2, client.decr("decrKey", 3, 0));

        assertEquals(2, cache().dataStructures().atomicLong("decrKey", 0, true).get());
    }

    /**
     * @throws Exception If failed.
     */
    public void testFlush() throws Exception {
        Assert.assertTrue(cache().putx("flushKey1", "flushVal1"));
        Assert.assertTrue(cache().putx("flushKey2", "flushVal2"));

        Assert.assertTrue(client.flush().get());

        Assert.assertNull(cache().get("flushKey1"));
        Assert.assertNull(cache().get("flushKey2"));
        Assert.assertTrue(cache().isEmpty());
    }

    /**
     * @throws Exception If failed.
     */
    public void testStat() throws Exception {
        Assert.assertTrue(cache().putx("statKey1", "statVal1"));
        assertEquals("statVal1", cache().get("statKey1"));

        Map<SocketAddress, Map<String, String>> map = client.getStats();

        Assert.assertEquals(1, map.size());

        Map<String, String> stats = F.first(map.values());

        Assert.assertEquals(7, stats.size());
        Assert.assertTrue(Integer.valueOf(stats.get("writes")) >= 1);
        Assert.assertTrue(Integer.valueOf(stats.get("reads")) >= 1);

        Assert.assertTrue(cache().putx("statKey2", "statVal2"));
        assertEquals("statVal2", cache().get("statKey2"));

        map = client.getStats();

        Assert.assertEquals(1, map.size());

        stats = F.first(map.values());

        Assert.assertEquals(7, stats.size());
        Assert.assertTrue(Integer.valueOf(stats.get("writes")) >= 2);
        Assert.assertTrue(Integer.valueOf(stats.get("reads")) >= 2);
    }

    /**
     * @throws Exception If failed.
     */
    public void testAppend() throws Exception {
        Assert.assertFalse(client.append(0, "appendKey", "_suffix").get());

        Assert.assertTrue(cache().putx("appendKey", "appendVal"));

        Assert.assertTrue(client.append(0, "appendKey", "_suffix").get());

        Assert.assertEquals("appendVal_suffix", client.get("appendKey"));

        assertEquals("appendVal_suffix", cache().get("appendKey"));
    }

    /**
     * @throws Exception If failed.
     */
    public void testPrepend() throws Exception {
        Assert.assertFalse(client.append(0, "prependKey", "_suffix").get());

        Assert.assertTrue(cache().putx("prependKey", "prependVal"));

        Assert.assertTrue(client.append(0, "prependKey", "_suffix").get());

        Assert.assertEquals("prependVal_suffix", client.get("prependKey"));

        assertEquals("prependVal_suffix", cache().get("prependKey"));
    }

    /**
     * @throws Exception If failed.
     */
    public void testSpecialTypes() throws Exception {
        Assert.assertTrue(client.set("boolKey", 0, true).get());

        Assert.assertEquals(true, client.get("boolKey"));

        Assert.assertTrue(client.set("intKey", 0, 10).get());

        Assert.assertEquals(10, client.get("intKey"));

        Assert.assertTrue(client.set("longKey", 0, 100L).get());

        Assert.assertEquals(100L, client.get("longKey"));

        Date now = new Date();

        Assert.assertTrue(client.set("dateKey", 0, now).get());

        Assert.assertEquals(now, client.get("dateKey"));

        Assert.assertTrue(client.set("byteKey", 0, (byte) 1).get());

        Assert.assertEquals((byte) 1, client.get("byteKey"));

        Assert.assertTrue(client.set("floatKey", 0, 1.1).get());

        Assert.assertEquals(1.1, client.get("floatKey"));

        Assert.assertTrue(client.set("doubleKey", 0, 100.001d).get());

        Assert.assertEquals(100.001d, client.get("doubleKey"));

        byte[] arr = new byte[5];

        for (byte i = 0; i < arr.length; i++)
            arr[i] = i;

        Assert.assertTrue(client.set("arrKey", 0, arr).get());

        assertArrayEquals(arr, (byte[])client.get("arrKey"));

        Assert.assertTrue(client.set("shortKey", 0, (short) 1).get());

        Assert.assertEquals((short) 1, client.get("shortKey"));
    }

    /**
     * @throws Exception If failed.
     */
    public void testComplexObject() throws Exception {
        Assert.assertTrue(client.set("objKey", 0, new ValueObject(10, "String")).get());

        Assert.assertEquals(new ValueObject(10, "String"), client.get("objKey"));
    }

    /**
     * @throws Exception If failed.
     */
    public void testCustomPort() throws Exception {
        customPort = 11212;

        Grid g = startGrid();

        assert g != null;
        assert g.nodes().size() == gridCount() + 1;

        MemcachedClientIF c = startClient();

        Assert.assertTrue(c.set("key", 0, 1).get());

        Assert.assertEquals(1, c.get("key"));

        c.shutdown();

        stopGrid();
    }

    /**
     * @throws Exception If failed.
     */
    public void testVersion() throws Exception {
        Map<SocketAddress, String> map = client.getVersions();

        Assert.assertEquals(1, map.size());

        String ver = F.first(map.values());

        Assert.assertFalse(F.isEmpty(ver));
    }

    /**
     * Complex object.
     */
    private static class ValueObject implements Serializable {
        /** */
        private int intVal;

        /** */
        private String strVal;

        /**
         * @param intVal Integer value.
         * @param strVal String value.
         */
        private ValueObject(int intVal, String strVal) {
            this.intVal = intVal;
            this.strVal = strVal;
        }

        /** {@inheritDoc} */
        @Override public boolean equals(Object o) {
            if (this == o)
                return true;

            if (o == null || getClass() != o.getClass())
                return false;

            ValueObject obj = (ValueObject)o;

            return intVal == obj.intVal &&
                !(strVal != null ? !strVal.equals(obj.strVal) : obj.strVal != null);

        }

        /** {@inheritDoc} */
        @Override public int hashCode() {
            int res = intVal;

            res = 31 * res + (strVal != null ? strVal.hashCode() : 0);

            return res;
        }
    }
}
