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

package org.gridgain.visor.commands.top

import org.gridgain.visor._
import VisorTopologyCommand._

/**
 * Unit test for topology commands.
 */
class VisorTopologyCommandSpec extends VisorRuntimeBaseSpec(2) {
    behavior of "A 'top' visor command"

    it should "advise to connect" in {
        visor.top()
    }

    it should "print error message" in {
        visor.open("-d", false)
        visor.top("-cc=eq1x")
        visor.close()
    }

    it should "print full topology" in {
        visor.open("-d", false)
        visor.top()
        visor.close()
    }

    it should "print nodes with idle time greater than 12000ms" in {
        visor.open("-d", false)
        visor.top("-it=gt12000")
        visor.close()
    }

    it should "print nodes with idle time greater than 12sec" in {
        visor.open("-d", false)
        visor.top("-it=gt12s")
        visor.close()
    }

    it should "print full information about all nodes" in {
        visor.open("-d", false)
        visor.top("-a")
        visor.close()
    }

    it should "print information about nodes on localhost" in {
        visor.open("-d", false)
        visor.top("-h=192.168.1.100")
        visor.close()
    }

    it should "print full information about nodes on localhost" in {
        visor.open("-d", false)
        visor.top("-h=localhost")
        visor.close()
    }
}
