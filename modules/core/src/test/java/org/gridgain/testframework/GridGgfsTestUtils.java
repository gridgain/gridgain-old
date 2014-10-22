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

package org.gridgain.testframework;

import net.sf.json.*;
import org.gridgain.grid.*;

import java.util.*;

/**
 * Utility class for tests.
 */
public class GridGgfsTestUtils {
    /**
     * Converts json string to Map<String,String>.
     *
     * @param jsonStr String to convert.
     * @return Map.
     * @throws GridException If fails.
     */
    public static Map<String,String> jsonToMap(String jsonStr) throws GridException {
        Map<String,String> res = new HashMap<>();

        try {
            JSONObject jsonObj = JSONObject.fromObject(jsonStr);

            for (Object o : jsonObj.entrySet()) {
                Map.Entry e = (Map.Entry) o;

                res.put(e.getKey().toString(), e.getValue().toString());
            }

        }
        catch (JSONException e) {
            throw new GridException(e);
        }

        return res;
    }
}
