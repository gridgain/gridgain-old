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

package org.gridgain.visor

import org.gridgain.grid._
import org.gridgain.scalar._
import org.scalatest._

/**
 * Test for interaction between visor.and scalar.
 */
class VisorScalarSpec extends FlatSpec with ShouldMatchers {
    behavior of "A visor object"

    it should "properly open and close w/o Scalar" in {
        visor.open("-d")
        visor.status()
        visor.close()
    }

    it should "properly open and close with Scalar" in {
        scalar start()

        try {
            visor.open("-d")
            visor.status()
            visor.close()
        }
        finally {
            scalar stop()
        }
    }

    it should "properly open and close with named Scalar" in {
        val cfg = new GridConfiguration

        cfg.setGridName("grid-scalar")

        scalar.start(cfg)

        try {
            cfg.setGridName("grid-visor")

            visor.open(cfg, "n/a")
            visor.status()
            visor.close()
        }
        finally {
            visor.close()

            scalar.stop("grid-scalar", true)
        }
    }

    it should "properly handle when local node stopped by Scalar" in {
        scalar start()

        try {
            visor.open("-d")
            scalar.stop()
            visor.status()
        }
        finally {
            scalar.stop()
        }
    }

    it should "properly open and close with Scalar & visor.mixes" in {
        scalar start()

        try {
            visor.open("-d")
            visor.status()
            visor.close()

            visor.open("-d")
            visor.status()
            visor.close()
        }
        finally {
            scalar.stop()
        }
    }
}
