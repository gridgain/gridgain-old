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

package org.gridgain.grid.spi.checkpoint.s3;

import com.amazonaws.auth.*;
import org.gridgain.grid.*;
import org.gridgain.grid.session.*;
import org.gridgain.testframework.config.*;

/**
 * Grid session checkpoint self test using {@link GridS3CheckpointSpi}.
 */
public class GridS3SessionCheckpointSelfTest extends GridSessionCheckpointAbstractSelfTest {
    /**
     * @throws Exception If failed.
     */
    public void testS3Checkpoint() throws Exception {
        GridConfiguration cfg = getConfiguration();

        GridS3CheckpointSpi spi = new GridS3CheckpointSpi();

        AWSCredentials cred = new BasicAWSCredentials(GridTestProperties.getProperty("amazon.access.key"),
            GridTestProperties.getProperty("amazon.secret.key"));

        spi.setAwsCredentials(cred);

        spi.setBucketNameSuffix("test");

        cfg.setCheckpointSpi(spi);

        GridSessionCheckpointSelfTest.spi = spi;

        checkCheckpoints(cfg);
    }
}
