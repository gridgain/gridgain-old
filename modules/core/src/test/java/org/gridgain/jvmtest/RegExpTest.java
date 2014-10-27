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

package org.gridgain.jvmtest;

import junit.framework.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.util.regex.*;

/**
 * Java reg exp test.
 */
public class RegExpTest extends TestCase {
    /**
     * @throws Exception If failed.
     */
    public void testRegExp() throws Exception {
        String normal =
            "swap-spaces/space1/b53b3a3d6ab90ce0268229151c9bde11|b53b3a3d6ab90ce0268229151c9bde11|1315392441288";

        byte[] b1 = new byte[200];
        byte[] b2 = normal.getBytes();

        U.arrayCopy(b2, 0, b1, 30, b2.length);

        CharSequence corrupt = new String(b1);

        String ptrn = "[a-z0-9/\\-]+\\|[a-f0-9]+\\|[0-9]+";

        Pattern p = Pattern.compile(ptrn);

        Matcher matcher = p.matcher(corrupt);

        assert matcher.find();

        X.println(String.valueOf(matcher.start()));

        assert normal.matches(ptrn);
    }
}
