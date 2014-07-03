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

package org.gridgain.testframework.junits.spi;

import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.discovery.*;
import org.gridgain.grid.spi.discovery.tcp.*;

import java.lang.annotation.*;

/**
 * Annotates all tests in SPI test framework. Provides implementation class of the SPI and
 * optional dependencies.
 */
@SuppressWarnings({"JavaDoc"})
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface GridSpiTest {
    /**
     * Mandatory implementation class for SPI.
     */
    public Class<? extends GridSpi> spi();

    /**
     * Flag indicating whether SPI should be automatically started.
     */
    public boolean trigger() default true;

    /**
     * Flag indicating whether discovery SPI should be automatically started.
     */
    public boolean triggerDiscovery() default false;

    /**
     * Optional discovery SPI property to specify which SPI to use for discovering other nodes.
     * This property is ignored if the spi being tested is an implementation of {@link GridDiscoverySpi} or
     * {@link #triggerDiscovery()} is set to {@code false}.
     */
    public Class<? extends GridDiscoverySpi> discoverySpi() default GridTcpDiscoverySpi.class;

    /**
     * Optional group this test belongs to.
     */
    public String group() default "";
}
