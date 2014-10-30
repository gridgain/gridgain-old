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
import java.util.*;

/**
 * AOP test.
 */
public class GridTestAopTarget implements GridTestAopTargetInterface {
    /**
     * @param arg Argument.
     * @return Result.
     */
    @Gridify(gridName="GridTestAopTarget")
    @Override public int gridifyDefault(String arg) {
        return Integer.parseInt(arg);
    }

    /**
     * @param arg Argument.
     * @return Result.
     */
    @Gridify(gridName="GridTestAopTarget", taskClass = GridTestGridifyTask.class)
    @Override public int gridifyNonDefaultClass(String arg) {
        return Integer.parseInt(arg);
    }


    /**
     * @param arg Argument.
     * @return Result.
     */
    @Gridify(gridName="GridTestAopTarget", taskName = GridTestGridifyTask.TASK_NAME)
    @Override public int gridifyNonDefaultName(String arg) {
        return Integer.parseInt(arg);
    }

    /**
     * @param arg Argument.
     * @return Result.
     */
    @Gridify(gridName="GridTestAopTarget", taskName = "")
    @Override public int gridifyNoName(String arg) {
        return 0;
    }

    /**
     * @param arg Argument.
     * @return Result.
     * @throws GridTestGridifyException If failed.
     */
    @Gridify(gridName="GridTestAopTarget")
    @Override public int gridifyDefaultException(String arg) throws GridTestGridifyException {
        throw new GridTestGridifyException(arg);
    }

    /**
     * @param arg Argument.
     * @return Result.
     * @throws GridTestGridifyException If failed.
     */
    @Gridify(gridName="GridTestAopTarget")
    @Override public int gridifyDefaultResource(String arg) throws GridTestGridifyException {
        int res = Integer.parseInt(arg);

        Integer rsrcVal = getResource();

        assert rsrcVal != null;
        assert rsrcVal == res : "Invalid result [res=" + res + ", rsrc=" + rsrcVal + ']';

        return res;
    }

    /**
     * @param arg Argument.
     * @return Result.
     * @throws GridTestGridifyException If failed.
     */
    @Gridify(gridName="GridTestAopTarget", taskClass = GridTestGridifyTask.class)
    @Override public int gridifyNonDefaultClassResource(String arg) throws GridTestGridifyException {
        assert getResource() != null;

        return Integer.parseInt(arg);
    }


    /**
     * @param arg Argument.
     * @return Result.
     * @throws GridTestGridifyException If failed.
     */
    @Gridify(gridName="GridTestAopTarget", taskName = GridTestGridifyTask.TASK_NAME)
    @Override public int gridifyNonDefaultNameResource(String arg) throws GridTestGridifyException {
        assert getResource() != null;

        return Integer.parseInt(arg);
    }

    /**
     * @return Result.
     * @throws GridTestGridifyException If failed.
     */
    private Integer getResource() throws GridTestGridifyException {
        try (InputStream in = getClass().getResourceAsStream("test_resource.properties")) {
            assert in != null;

            Properties prop = new Properties();

            prop.load(in);

            String val = prop.getProperty("param1");

            return Integer.parseInt(val);
        }
        catch (IOException e) {
            throw new GridTestGridifyException("Failed to test load properties file.", e);
        }
    }
}
