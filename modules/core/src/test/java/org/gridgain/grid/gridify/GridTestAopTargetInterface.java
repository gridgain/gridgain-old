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

package org.gridgain.grid.gridify;

import org.gridgain.grid.compute.gridify.*;

import java.io.*;

/**
 * Test AOP target interface.
 */
public interface GridTestAopTargetInterface extends Serializable {
    /**
     * @param arg Argument.
     * @return Result.
     */
    @Gridify(gridName="GridTestAopTargetInterface")
    public int gridifyDefault(String arg);

    /**
     * @param arg Argument.
     * @return Result.
     */
    @Gridify(gridName="GridTestAopTargetInterface", taskName = GridTestGridifyTask.TASK_NAME)
    public int gridifyNonDefaultName(String arg);

    /**
     * @param arg Argument.
     * @return Result.
     */
    @Gridify(gridName="GridTestAopTargetInterface", taskClass = GridTestGridifyTask.class)
    public int gridifyNonDefaultClass(String arg);

    /**
     * @param arg Argument.
     * @return Result.
     */
    @Gridify(gridName="GridTestAopTargetInterface", taskName = "")
    public int gridifyNoName(String arg);

    /**
     * @param arg Argument.
     * @return Result.
     * @throws GridTestGridifyException If failed.
     */
    @Gridify(gridName="GridTestAopTargetInterface")
    public int gridifyDefaultException(String arg) throws GridTestGridifyException;

    /**
     * @param arg Argument.
     * @return Result.
     * @throws GridTestGridifyException If failed.
     */
    @Gridify(gridName="GridTestAopTargetInterface")
    public int gridifyDefaultResource(String arg) throws GridTestGridifyException;

    /**
     * @param arg Argument.
     * @return Result.
     * @throws GridTestGridifyException If failed.
     */
    @Gridify(gridName="GridTestAopTargetInterface", taskClass = GridTestGridifyTask.class)
    public int gridifyNonDefaultClassResource(String arg) throws GridTestGridifyException;

    /**
     * @param arg Argument.
     * @return Result.
     * @throws GridTestGridifyException If failed.
     */
    @Gridify(gridName="GridTestAopTargetInterface", taskName = GridTestGridifyTask.TASK_NAME)
    public int gridifyNonDefaultNameResource(String arg) throws GridTestGridifyException;
}
