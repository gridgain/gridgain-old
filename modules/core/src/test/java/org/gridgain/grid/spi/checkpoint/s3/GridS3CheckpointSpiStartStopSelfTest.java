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
import org.gridgain.grid.spi.*;
import org.gridgain.testframework.config.*;
import org.gridgain.testframework.junits.spi.*;

/**
 * Grid S3 checkpoint SPI start stop self test.
 */
@GridSpiTest(spi = GridS3CheckpointSpi.class, group = "Checkpoint SPI")
public class GridS3CheckpointSpiStartStopSelfTest extends GridSpiStartStopAbstractTest<GridS3CheckpointSpi> {
    /** {@inheritDoc} */
    @Override protected void spiConfigure(GridS3CheckpointSpi spi) throws Exception {
        AWSCredentials cred = new BasicAWSCredentials(GridTestProperties.getProperty("amazon.access.key"),
            GridTestProperties.getProperty("amazon.secret.key"));

        spi.setAwsCredentials(cred);

        spi.setBucketNameSuffix("test");

        super.spiConfigure(spi);
    }
}
