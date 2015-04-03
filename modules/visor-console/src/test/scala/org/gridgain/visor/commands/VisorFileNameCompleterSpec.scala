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

package org.gridgain.visor.commands

import java.io.File
import java.util

import org.scalatest._

/**
 * Test for visor's file name completer.
 */
class VisorFileNameCompleterSpec extends FlatSpec with ShouldMatchers {
    behavior of "A visor file name completer"

    it should "properly parse empty path" in {
        val c = new VisorFileNameCompleter()

        val res = new util.ArrayList[CharSequence]()

        c.complete("", 0, res)

        assertResult(new File("").getAbsoluteFile.listFiles().length)(res.size)

        res.clear()

        c.complete(null, 0, res)

        assertResult(new File("").getAbsoluteFile.listFiles().length)(res.size)

        res.clear()

        c.complete("    ", 2, res)

        assertResult(new File("").getAbsoluteFile.listFiles().length)(res.size)

        res.clear()

        c.complete("help ", 5, res)

        assertResult(new File("").getAbsoluteFile.listFiles().length)(res.size)
    }
}
