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

/**
 * ==Overview==
 * Visor 'ack' command implementation.
 *
 * ==Importing==
 * When using this command from Scala code (not from REPL) you need to make sure to properly
 * import all necessary typed and implicit conversions:
 * <ex>
 * import org.gridgain.visor._
 * import commands.ack.VisorAckCommand._
 * </ex>
 * Note that `VisorAckCommand` object contains necessary implicit conversions so that
 * this command would be available via `visor` keyword.
 *
 * ==Help==
 * {{{
 * +-------------------------------------------+
 * | ack | Acks arguments on all remote nodes. |
 * +-------------------------------------------+
 * }}}
 *
 * ====Specification====
 * {{{
 *     visor ack {"s"}
 *     visor ack ("s", f)
 * }}}
 *
 * ====Arguments====
 * {{{
 *     s
 *         Optional string to print on each remote node.
 *     f
 *         Optional Scala predicate on 'ScalarRichNodePimp' filtering nodes in the topology.
 * }}}
 *
 * ====Examples====
 * {{{
 *     visor ack "Howdy!"
 *         Prints 'Howdy!' on all nodes in the topology.
 *     visor ack("Howdy!", _.id8.startsWith("123"))
 *         Prints 'Howdy!' on all nodes satisfying this predicate.
 *     visor ack
 *         Prints local node ID on all nodes in the topology.
 * }}}
 *
 * @author @java.author
 * @version @java.version
 */
package object ack