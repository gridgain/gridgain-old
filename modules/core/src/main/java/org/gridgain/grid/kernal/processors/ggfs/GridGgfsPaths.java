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
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;

/**
 * Description of path modes.
 */
public class GridGgfsPaths implements Externalizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Additional secondary file system properties. */
    private Map<String, String> props;

    /** Default GGFS mode. */
    private GridGgfsMode dfltMode;

    /** Path modes. */
    private List<T2<GridGgfsPath, GridGgfsMode>> pathModes;

    /**
     * Empty constructor required by {@link Externalizable}.
     */
    public GridGgfsPaths() {
        // No-op.
    }

    /**
     * Constructor.
     *
     * @param props Additional secondary file system properties.
     * @param dfltMode Default GGFS mode.
     * @param pathModes Path modes.
     */
    public GridGgfsPaths(Map<String, String> props, GridGgfsMode dfltMode, @Nullable List<T2<GridGgfsPath,
        GridGgfsMode>> pathModes) {
        this.props = props;
        this.dfltMode = dfltMode;
        this.pathModes = pathModes;
    }

    /**
     * @return Secondary file system properties.
     */
    public Map<String, String> properties() {
        return props;
    }

    /**
     * @return Default GGFS mode.
     */
    public GridGgfsMode defaultMode() {
        return dfltMode;
    }

    /**
     * @return Path modes.
     */
    @Nullable public List<T2<GridGgfsPath, GridGgfsMode>> pathModes() {
        return pathModes;
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        U.writeStringMap(out, props);
        U.writeEnum0(out, dfltMode);

        if (pathModes != null) {
            out.writeBoolean(true);
            out.writeInt(pathModes.size());

            for (T2<GridGgfsPath, GridGgfsMode> pathMode : pathModes) {
                pathMode.getKey().writeExternal(out);
                U.writeEnum0(out, pathMode.getValue());
            }
        }
        else
            out.writeBoolean(false);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        props = U.readStringMap(in);
        dfltMode = GridGgfsMode.fromOrdinal(U.readEnumOrdinal0(in));

        if (in.readBoolean()) {
            int size = in.readInt();

            pathModes = new ArrayList<>(size);

            for (int i = 0; i < size; i++) {
                GridGgfsPath path = new GridGgfsPath();
                path.readExternal(in);

                T2<GridGgfsPath, GridGgfsMode> entry = new T2<>(path, GridGgfsMode.fromOrdinal(U.readEnumOrdinal0(in)));

                pathModes.add(entry);
            }
        }
    }
}
