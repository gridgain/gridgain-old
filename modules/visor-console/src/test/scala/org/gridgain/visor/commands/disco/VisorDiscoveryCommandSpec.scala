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

/*
 * ___    _________________________ ________
 * __ |  / /____  _/__  ___/__  __ \___  __ \
 * __ | / /  __  /  _____ \ _  / / /__  /_/ /
 * __ |/ /  __/ /   ____/ / / /_/ / _  _, _/
 * _____/   /___/   /____/  \____/  /_/ |_|
 *
 */

package org.gridgain.visor.commands.disco

import org.gridgain.grid.{GridGain => G, _}
import org.gridgain.visor._
import org.gridgain.visor.commands.disco.VisorDiscoveryCommand._

/**
 * Unit test for 'disco' command.
 */
class VisorDiscoveryCommandSpec extends VisorRuntimeBaseSpec(4) {
    /**
     * Open visor and execute several tasks before all tests.
     */
    override protected def beforeAll() {
        super.beforeAll()

        G.stop("node-1", false)
        G.stop("node-2", false)
    }

    /**
     * Creates grid configuration for provided grid host.
     *
     * @param name Grid name.
     * @return Grid configuration.
     */
    override def config(name: String): GridConfiguration = {
        val cfg = new GridConfiguration

        cfg.setGridName(name)
        cfg.setLifeCycleEmailNotification(false)

        cfg
    }

    behavior of  "A 'disco' visor command"

    it should "advise to connect" in  {
        closeVisorQuiet()

        visor.disco()
    }

    it should "show all discovery events" in  {
        visor.disco()
    }

    it should "show all discovery events in reversed order" in  {
        visor.disco("-r")
    }

    it should "show discovery events from last two minutes" in {
        visor.disco("-t=2m")
    }

    it should "show discovery events from last two minutes in reversed order " in {
        visor.disco("-t=2m -r")
    }

    it should "show top 3 discovery events" in  {
        visor.disco("-c=3")
    }

    it should "print error message with invalid count" in {
        visor.disco("-c=x")
    }
}
