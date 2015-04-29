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

package org.gridgain.visor.commands.node

import org.gridgain.visor._
import org.gridgain.visor.commands.node.VisorNodeCommand._

/**
 * Unit test for 'node' command.
 */
class VisorNodeCommandSpec extends VisorRuntimeBaseSpec(1) {
    behavior of "A 'node' visor command"

    it should "properly execute with valid node ID" in {
        visor.node("-id8=@n1")
    }

    it should "print the error message for invalid node ID" in {
        visor.node("-id8=zeee")
    }

    it should "print error message when not connected" in {
        closeVisorQuiet()

        visor.node("") // Arguments are ignored.
    }
}
