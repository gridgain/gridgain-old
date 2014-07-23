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

package org.gridgain.grid.spi.deployment.uri.tasks;

import org.gridgain.grid.util.typedef.internal.*;
import java.io.*;
import java.util.*;

/**
 * This class used by {@link GridUriDeploymentTestTask2} which loaded from GAR file.
 * GridDependency loaded from {@code /lib/*.jar} in GAR file.
 * GridDependency load resource {@code test.properties} from the same jar in {@code /lib/*.jar}
 */
public class GridUriDeploymentDependency1 {
    /** */
    public static final String RESOURCE = "org/gridgain/grid/spi/deployment/uri/tasks/test1.properties";

    /**
     * @return Value of the property {@code test1.txt} loaded from the {@code test1.properties} file.
     */
    public String getMessage() {
        InputStream in = null;

        try {
            in = getClass().getClassLoader().getResourceAsStream(RESOURCE);

            Properties props = new Properties();

            props.load(in);

            return props.getProperty("test1.txt");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally{
            U.close(in, null);
        }

        return null;
    }
}
