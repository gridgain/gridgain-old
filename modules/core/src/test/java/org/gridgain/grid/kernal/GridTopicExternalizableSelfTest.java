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

package org.gridgain.grid.kernal;

import org.gridgain.grid.*;
import org.gridgain.grid.marshaller.*;

import java.io.*;
import java.util.*;

/**
 * Grid topic externalization test.
 */
public class GridTopicExternalizableSelfTest extends GridExternalizableAbstractTest {
    /** */
    private static final GridUuid A_GRID_UUID = GridUuid.randomUuid();

    /** */
    private static final UUID AN_UUID = UUID.randomUUID();

    /** */
    private static final long A_LONG = Long.MAX_VALUE;

    /** */
    private static final String A_STRING = "test_test_test_test_test_test_test_test_test_test_test_test_test_test";

    /** */
    private static final int AN_INT = Integer.MAX_VALUE;

    /**
     * @throws Exception If failed.
     */
    public void testSerializationTopicCreatedByGridUuid() throws Exception {
        for (GridMarshaller marsh : getMarshallers()) {
            info("Test GridTopic externalization [marshaller=" + marsh + ']');

            for (GridTopic topic : GridTopic.values()) {
                Externalizable msgOut = (Externalizable)topic.topic(A_GRID_UUID);

                assertEquals(msgOut, GridTestIoUtils.externalize(msgOut, marsh));
            }
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testSerializationTopicCreatedByGridUuidAndUUID() throws Exception {
        for (GridMarshaller marsh : getMarshallers()) {
            info("Test GridTopic externalization [marshaller=" + marsh + ']');

            for (GridTopic topic : GridTopic.values()) {
                Externalizable msgOut = (Externalizable)topic.topic(A_GRID_UUID, AN_UUID);

                assertEquals(msgOut, GridTestIoUtils.externalize(msgOut, marsh));
            }
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testSerializationTopicCreatedByGridUuidAndLong() throws Exception {
        for (GridMarshaller marsh : getMarshallers()) {
            info("Test GridTopic externalization [marshaller=" + marsh + ']');

            for (GridTopic topic : GridTopic.values()) {
                Externalizable msgOut = (Externalizable)topic.topic(A_GRID_UUID, A_LONG);

                assertEquals(msgOut, GridTestIoUtils.externalize(msgOut, marsh));
            }
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testSerializationTopicCreatedByStringAndUUIDAndLong() throws Exception {
        for (GridMarshaller marsh : getMarshallers()) {
            info("Test GridTopic externalization [marshaller=" + marsh + ']');

            for (GridTopic topic : GridTopic.values()) {
                Externalizable msgOut = (Externalizable)topic.topic(A_STRING, AN_UUID, A_LONG);

                assertEquals(msgOut, GridTestIoUtils.externalize(msgOut, marsh));
            }
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testSerializationTopicCreatedByString() throws Exception {
        for (GridMarshaller marsh : getMarshallers()) {
            info("Test GridTopic externalization [marshaller=" + marsh + ']');

            for (GridTopic topic : GridTopic.values()) {
                Externalizable msgOut = (Externalizable)topic.topic(A_STRING);

                assertEquals(msgOut, GridTestIoUtils.externalize(msgOut, marsh));
            }
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testSerializationTopicCreatedByStringAndIntAndLong() throws Exception {
        for (GridMarshaller marsh : getMarshallers()) {
            info("Test GridTopic externalization [marshaller=" + marsh + ']');

            for (GridTopic topic : GridTopic.values()) {
                Externalizable msgOut = (Externalizable)topic.topic(A_STRING, AN_INT, A_LONG);

                assertEquals(msgOut, GridTestIoUtils.externalize(msgOut, marsh));
            }
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testSerializationTopicCreatedByStrinAndLong() throws Exception {
        for (GridMarshaller marsh : getMarshallers()) {
            info("Test GridTopic externalization [marshaller=" + marsh + ']');

            for (GridTopic topic : GridTopic.values()) {
                Externalizable msgOut = (Externalizable)topic.topic(A_STRING, A_LONG);

                assertEquals(msgOut, GridTestIoUtils.externalize(msgOut, marsh));
            }
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testSerializationTopicCreatedByStringAndUUIDAndIntAndLong() throws Exception {
        for (GridMarshaller marsh : getMarshallers()) {
            info("Test GridTopic externalization [marshaller=" + marsh + ']');

            for (GridTopic topic : GridTopic.values()) {
                Externalizable msgOut = (Externalizable)topic.topic(A_STRING, AN_UUID, AN_INT, A_LONG);

                assertEquals(msgOut, GridTestIoUtils.externalize(msgOut, marsh));
            }
        }
    }
}
