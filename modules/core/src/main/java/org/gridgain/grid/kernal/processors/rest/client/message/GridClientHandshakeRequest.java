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

package org.gridgain.grid.kernal.processors.rest.client.message;

import org.gridgain.grid.util.typedef.internal.*;

import java.util.*;

/**
 * A client handshake request, containing version info and
 * a marshaller ID.
 *
 * A handshake request structure is as follows:
 * <ol>
 *     <li>Protocol version (2 bytes)</li>
 *     <li>Marshaller ID (2 bits)</li>
 *     <li>Reserved space (6 bits + 1 byte)</li>
 *     <li>Marshaller ID for backward compatibility (1 byte)</li>
 * </ol>
 */
public class GridClientHandshakeRequest extends GridClientAbstractMessage {
    /** */
    private static final long serialVersionUID = 0L;

    /** Packet size. */
    static final int PACKET_SIZE = 5;

    /** Protocol version. */
    private static final short PROTO_VER = 1;

    /** Handshake byte array. */
    private byte[] arr;

    /** Marshaller ID. */
    private byte marshId;

    /**
     * @return Protocol version.
     */
    public short version() {
        return U.bytesToShort(arr, 0);
    }

    /**
     * @return Marshaller ID.
     */
    public byte marshallerId() {
        return (byte)((arr[2] & 0xff) >> 6);
    }

    /**
     * @param marshId Marshaller ID.
     */
    public void marshallerId(byte marshId) {
        assert marshId >= 0 && marshId <= 2;

        this.marshId = marshId;
    }

    /**
     * Sets bytes from specified buffer to a given value.
     *
     * @param buf Buffer.
     * @param off Offset.
     * @param len Length.
     */
    public void putBytes(byte[] buf, int off, int len) {
        if (arr == null)
            arr = new byte[PACKET_SIZE];

        U.arrayCopy(buf, 0, arr, off, len);
    }

    /**
     * @return Raw representation of this packet.
     */
    public byte[] rawBytes() {
        byte[] ret = new byte[PACKET_SIZE];

        U.shortToBytes(PROTO_VER, ret, 0);

        ret[2] = (byte)(marshId << 6);

        ret[4] = marshId; // Marshaller ID for backward compatibility.

        return ret;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return getClass().getSimpleName() + " [arr=" + Arrays.toString(arr) + ']';
    }
}
