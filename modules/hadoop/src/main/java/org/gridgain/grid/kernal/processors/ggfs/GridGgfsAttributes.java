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

package org.gridgain.grid.kernal.processors.ggfs;

import org.gridgain.grid.ggfs.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.io.*;
import java.util.*;

/**
 * GGFS attributes.
 * <p>
 * This class contains information on a single GGFS configured on some node.
 */
public class GridGgfsAttributes implements Externalizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** GGFS name. */
    private String ggfsName;

    /** File's data block size (bytes). */
    private int blockSize;

    /** Size of the group figured in {@link GridGgfsGroupDataBlocksKeyMapper}. */
    private int grpSize;

    /** Meta cache name. */
    private String metaCacheName;

    /** Data cache name. */
    private String dataCacheName;

    /** Default mode. */
    private GridGgfsMode dfltMode;

    /** Fragmentizer enabled flag. */
    private boolean fragmentizerEnabled;

    /** Path modes. */
    private Map<String, GridGgfsMode> pathModes;

    /**
     * @param ggfsName GGFS name.
     * @param blockSize File's data block size (bytes).
     * @param grpSize Size of the group figured in {@link GridGgfsGroupDataBlocksKeyMapper}.
     * @param metaCacheName Meta cache name.
     * @param dataCacheName Data cache name.
     * @param dfltMode Default mode.
     * @param pathModes Path modes.
     */
    public GridGgfsAttributes(String ggfsName, int blockSize, int grpSize, String metaCacheName, String dataCacheName,
        GridGgfsMode dfltMode, Map<String, GridGgfsMode> pathModes, boolean fragmentizerEnabled) {
        this.blockSize = blockSize;
        this.ggfsName = ggfsName;
        this.grpSize = grpSize;
        this.metaCacheName = metaCacheName;
        this.dataCacheName = dataCacheName;
        this.dfltMode = dfltMode;
        this.pathModes = pathModes;
        this.fragmentizerEnabled = fragmentizerEnabled;
    }

    /**
     * Public no-arg constructor for {@link Externalizable}.
     */
    public GridGgfsAttributes() {
        // No-op.
    }

    /**
     * @return GGFS name.
     */
    public String ggfsName() {
        return ggfsName;
    }

    /**
     * @return File's data block size (bytes).
     */
    public int blockSize() {
        return blockSize;
    }

    /**
     * @return Size of the group figured in {@link GridGgfsGroupDataBlocksKeyMapper}.
     */
    public int groupSize() {
        return grpSize;
    }

    /**
     * @return Metadata cache name.
     */
    public String metaCacheName() {
        return metaCacheName;
    }

    /**
     * @return Data cache name.
     */
    public String dataCacheName() {
        return dataCacheName;
    }

    /**
     * @return Default mode.
     */
    public GridGgfsMode defaultMode() {
        return dfltMode;
    }

    /**
     * @return Path modes.
     */
    public Map<String, GridGgfsMode> pathModes() {
        return pathModes != null ? Collections.unmodifiableMap(pathModes) : null;
    }

    /**
     * @return {@code True} if fragmentizer is enabled.
     */
    public boolean fragmentizerEnabled() {
        return fragmentizerEnabled;
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        U.writeString(out, ggfsName);
        out.writeInt(blockSize);
        out.writeInt(grpSize);
        U.writeString(out, metaCacheName);
        U.writeString(out, dataCacheName);
        U.writeEnum0(out, dfltMode);
        out.writeBoolean(fragmentizerEnabled);

        if (pathModes != null) {
            out.writeBoolean(true);

            out.writeInt(pathModes.size());

            for (Map.Entry<String, GridGgfsMode> pathMode : pathModes.entrySet()) {
                U.writeString(out, pathMode.getKey());
                U.writeEnum0(out, pathMode.getValue());
            }
        }
        else
            out.writeBoolean(false);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        ggfsName = U.readString(in);
        blockSize = in.readInt();
        grpSize = in.readInt();
        metaCacheName = U.readString(in);
        dataCacheName = U.readString(in);
        dfltMode = GridGgfsMode.fromOrdinal(U.readEnumOrdinal0(in));
        fragmentizerEnabled = in.readBoolean();

        if (in.readBoolean()) {
            int size = in.readInt();

            pathModes = new HashMap<>(size, 1.0f);

            for (int i = 0; i < size; i++)
                pathModes.put(U.readString(in), GridGgfsMode.fromOrdinal(U.readEnumOrdinal0(in)));
        }
    }
}
