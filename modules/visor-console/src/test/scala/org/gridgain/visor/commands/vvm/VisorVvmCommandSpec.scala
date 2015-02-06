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

package org.gridgain.visor.commands.vvm

import org.gridgain.visor._
import org.gridgain.visor.commands.vvm.VisorVvmCommand._
import org.scalatest._

/**
 * Unit test for 'vvm' command.
 */
class VisorVvmCommandSpec extends FlatSpec with Matchers {
    behavior of "A 'vvm' visor command"

    it should "print error message when not connected" in {
        visor.vvm()
    }

    it should "open VisualVM connected to all nodes skipping ones with disabled JMX" in {
        visor.open("-d")
        visor.vvm()
        visor.close()
    }

    it should "open VisualVM connected to first node if it has JMX enabled" in {
        visor.open("-d")
        visor.vvm("-id8=@n1")
        visor.close()
    }
}
