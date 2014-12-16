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

package org.gridgain.visor.commands.kill

import org.gridgain.visor._
import org.gridgain.visor.commands.kill.VisorKillCommand._
import org.scalatest._

/**
 * Unit test for 'kill' command.
 */
class VisorKillCommandSpec extends FlatSpec with Matchers {
    behavior of "A 'kill' visor command"

    it should "print error message with null argument" in {
        visor.open("-d")
        visor.kill(null)
        visor.close()
    }

    it should "print error message if both kill and restart specified" in {
        visor.open("-d")
        visor.kill("-k -r")
        visor.close()
    }

    it should "print error message if not connected" in {
        visor.kill("-k")
    }

    it should "restart node" in {
        visor.open("-d")
        visor.kill("-r -id8=@n1")
        visor.close()
    }

    it should "print error message" in {
        visor.open("-d")
        visor.kill("-r -id=xxx")
        visor.close()
    }
}
