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

package org.gridgain.grid.kernal.visor.cmd.dto.node;

import org.gridgain.grid.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.io.*;

import static org.gridgain.grid.GridSystemProperties.*;
import static org.gridgain.grid.kernal.visor.cmd.VisorTaskUtils.*;

/**
 * Data transfer object for node lifecycle configuration properties.
 */
public class VisorLifecycleConfig implements Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Lifecycle beans. */
    private String beans;

    /** Whether or not email notifications should be used on node start and stop. */
    private boolean ntf;

    /**
     * @param c Grid configuration.
     * @return Data transfer object for node lifecycle configuration properties.
     */
    public static VisorLifecycleConfig from(GridConfiguration c) {
        VisorLifecycleConfig cfg = new VisorLifecycleConfig();

        cfg.beans(compactArray(c.getLifecycleBeans()));
        cfg.emailNotification(boolValue(GG_LIFECYCLE_EMAIL_NOTIFY, c.isLifeCycleEmailNotification()));

        return cfg;
    }

    /**
     * @return Lifecycle beans.
     */
    @Nullable public String beans() {
        return beans;
    }

    /**
     * @param beans New lifecycle beans.
     */
    public void beans(@Nullable String beans) {
        this.beans = beans;
    }

    /**
     * @return Whether or not email notifications should be used on node start and stop.
     */
    public boolean emailNotification() {
        return ntf;
    }

    /**
     * @param ntf New whether or not email notifications should be used on node start and stop.
     */
    public void emailNotification(boolean ntf) {
        this.ntf = ntf;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(VisorLifecycleConfig.class, this);
    }
}
