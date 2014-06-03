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

import org.gridgain.grid.util.scala.impl
import org.gridgain.visor.visor

/**
 * Command implementation.
 */
trait VisorConsoleCommand {
    /**
     * Command without arguments.
     */
    def invoke()

    /**
     * Command with arguments.
     *
     * @param args - arguments as string.
     */
    def invoke(args: String)
}

/**
 * Singleton companion object.
 */
object VisorConsoleCommand {
    /**
     * Create `VisorConsoleCommand`.
     *
     * @param emptyArgs Function to execute in case of no args passed to command.
     * @param withArgs Function to execute in case of some args passed to command.
     * @return New instance of `VisorConsoleCommand`.
     */
    def apply(emptyArgs: () => Unit, withArgs: (String) => Unit) = {
        new VisorConsoleCommand {
            @impl def invoke(): Unit = emptyArgs.apply()

            @impl def invoke(args: String): Unit = withArgs.apply(args)
        }
    }

    /**
     * Create `VisorConsoleCommand`.
     *
     * @param emptyArgs Function to execute in case of no args passed to command.
     * @return New instance of `VisorConsoleCommand`.
     */
    def apply(emptyArgs: () => Unit) = {
        new VisorConsoleCommand {
            @impl def invoke(): Unit = emptyArgs.apply()

            @impl def invoke(args: String): Unit = {
                visor.warn(
                    "Invalid arguments for command without arguments.",
                    "Type 'help' to print commands list."
                )
            }
        }
    }
}
