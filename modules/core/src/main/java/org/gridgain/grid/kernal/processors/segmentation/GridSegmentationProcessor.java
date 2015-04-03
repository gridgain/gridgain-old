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

package org.gridgain.grid.kernal.processors.segmentation;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.processors.*;

/**
 * Kernal processor responsible for checking network segmentation issues.
 * <p>
 * Segment checks are performed by segmentation resolvers
 * Each segmentation resolver checks segment for validity, using its inner logic.
 * Typically, resolver should run light-weight single check (i.e. one IP address or
 * one shared folder). Compound segment checks may be performed using several
 * resolvers.
 * @see GridConfiguration#getSegmentationResolvers()
 * @see GridConfiguration#getSegmentationPolicy()
 * @see GridConfiguration#getSegmentCheckFrequency()
 * @see GridConfiguration#isAllSegmentationResolversPassRequired()
 * @see GridConfiguration#isWaitForSegmentOnStart()
 */
public interface GridSegmentationProcessor extends GridProcessor {
    /**
     * Performs network segment check.
     * <p>
     * This method is called by discovery manager in the following cases:
     * <ol>
     *     <li>Before discovery SPI start.</li>
     *     <li>When other node leaves topology.</li>
     *     <li>When other node in topology fails.</li>
     *     <li>Periodically (see {@link GridConfiguration#getSegmentCheckFrequency()}).</li>
     * </ol>
     *
     * @return {@code True} if segment is correct.
     */
    public boolean isValidSegment();
}
