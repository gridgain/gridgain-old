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

package org.gridgain.visor.commands.gc

import org.gridgain.visor._
import org.gridgain.visor.commands.gc.VisorGcCommand._
import org.gridgain.visor.commands.top.VisorTopologyCommand._
import org.scalatest._

/**
 * Unit test for 'gc' command.
 */
class VisorGcCommandSpec extends FlatSpec with Matchers with BeforeAndAfterAll {
    behavior of "A 'gc' visor command"

    override def beforeAll() {
        visor.open("-d")

        visor.top()
    }

    override def afterAll() {
        visor.close()
    }

    it should "run GC on all nodes" in {
        visor.gc()
    }

    it should "run GC on first node" in {
        visor.gc("-id8=@n0")
    }

    it should "run GC and DGC on all nodes" in {
        visor.gc("-c")
    }

    it should "run GC and DGC on first node" in {
        visor.gc("-id8=@n0 -c")
    }
}
