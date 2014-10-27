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
 * Contains Visor command `gc` implementation.
 *
 * ==Help==
 * {{{
 * +---------------------------------------------------------------------------+
 * | gc | Runs garbage collector on remote nodes.                              |
 * |    | If specific node is provided, garbage collector is run on that node. |
 * |    | Otherwise, it will be run on all nodes in topology.                  |
 * +---------------------------------------------------------------------------+
 * }}}
 *
 * ====Specification====
 * {{{
 *     gc
 *     gc "{-id8=<node-id8>|-id=<node-id>} {-c}"
 * }}}
 *
 * ====Arguments====
 * {{{
 *     -id8=<node-id8>
 *         ID8 of the node.
 *         Note that either '-id8' or '-id' can be specified.
 *     -id=<node-id>
 *         ID of the node.
 *         Note that either '-id8' or '-id' can be specified.
 *     -c
 *         Run DGC procedure on all caches.
 * }}}
 *
 * ====Examples====
 * {{{
 *     gc "-id8=12345678"
 *         Runs garbage collector on specified node.
 *     gc
 *         Runs garbage collector on all nodes in topology.
 *     gc "-id8=12345678 -c"
 *         Runs garbage collector and DGC procedure on all caches.
 * }}}
 */
package object gc
