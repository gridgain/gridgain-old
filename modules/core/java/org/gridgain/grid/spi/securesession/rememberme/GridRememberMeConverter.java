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

package org.gridgain.grid.spi.securesession.rememberme;

import org.gridgain.grid.spi.*;
import org.jetbrains.annotations.*;

/**
 * Converter from one byte format to another.
 */
public interface GridRememberMeConverter {
    /**
     * Convert binary message from one format to another.
     *
     * @param data Binary data to process with this message digest.
     * @return processed data or {@code null} if data cannot be processed
     * @throws GridSpiException Thrown on any system exception. Note: this method should return {@code
     * null} value, if passed in data cannot be processed.
     */
    @Nullable byte[] convert(byte[] data) throws GridSpiException;
}
