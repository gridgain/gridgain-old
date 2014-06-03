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

package org.gridgain.grid.kernal.processors.resource;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.managers.deployment.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;

/**
 *
 */
public class GridResourceLoggerInjector extends GridResourceBasicInjector<GridLogger> {
    /**
     * @param rsrc Root logger.
     */
    public GridResourceLoggerInjector(GridLogger rsrc) {
        super(rsrc);
    }

    /** {@inheritDoc} */
    @Override public void inject(GridResourceField field, Object target, Class<?> depCls, GridDeployment dep)
        throws GridException {
        GridResourceUtils.inject(field.getField(), target, resource((GridLoggerResource)field.getAnnotation(), target));
    }

    /** {@inheritDoc} */
    @Override public void inject(GridResourceMethod mtd, Object target, Class<?> depCls, GridDeployment dep)
        throws GridException {
        GridResourceUtils.inject(mtd.getMethod(), target, resource((GridLoggerResource)mtd.getAnnotation(), target));
    }

    /**
     * @param ann Annotation.
     * @param target Target.
     * @return Logger.
     */
    @SuppressWarnings("IfMayBeConditional")
    private GridLogger resource(GridLoggerResource ann, Object target) {
        Class<?> cls = ann.categoryClass();
        String cat = ann.categoryName();

        GridLogger rsrc = getResource();

        if (cls != null && cls != Void.class)
            rsrc = rsrc.getLogger(cls);
        else if (cat != null && !cat.isEmpty())
            rsrc = rsrc.getLogger(cat);
        else
            rsrc = rsrc.getLogger(target.getClass());

        return rsrc;
    }
}
