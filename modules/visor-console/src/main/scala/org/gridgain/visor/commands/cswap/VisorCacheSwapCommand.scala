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
package org.gridgain.visor.commands.cswap

import org.gridgain.scalar._
import scalar._
import org.gridgain.visor._
import org.gridgain.visor.commands.{VisorConsoleCommand, VisorTextTable}
import visor._
import org.gridgain.grid._
import org.gridgain.grid.kernal.GridEx
import resources._
import collection.JavaConversions._
import java.util.UUID
import scala.util.control.Breaks._
import org.jetbrains.annotations.Nullable
import org.gridgain.grid.util.typedef._
import util.scala.impl
import org.gridgain.grid.kernal.processors.task.GridInternal
import org.gridgain.grid.lang.GridCallable

/**
 * ==Overview==
 * Visor 'cswap' command implementation.
 *
 * ==Help==
 * {{{
 * +-----------------------------------------------------+
 * | cswap | Swaps backup entries in cache on all nodes. |
 * +-----------------------------------------------------+
 * }}}
 *
 * ====Specification====
 * {{{
 *     cswap
 *     cswap -c=<cache-name>
 * }}}
 *
 * ====Arguments====
 * {{{
 *     <cache-name>
 *         Name of the cache.
 *         If not specified, entries in default cache will be swapped.
 * }}}
 *
 * ====Examples====
 * {{{
 *     cswap
 *         Swaps entries in default cache.
 *     cswap -c=cache
 *         Swaps entries in cache with name 'cache'.
 * }}}
 */
class VisorCacheSwapCommand {
    /**
     * Prints error message and advise.
     *
     * @param errMsgs Error messages.
     */
    private def scold(errMsgs: Any*) {
        assert(errMsgs != null)

        warn(errMsgs: _*)
        warn("Type 'help cswap' to see how to use this command.")
    }

    /**
     * ===Command===
     * Swaps entries in cache.
     *
     * ===Examples===
     * <ex>cswap -c=cache</ex>
     * Swaps entries in cache with name 'cache'.
     *
     * @param args Command arguments.
     */
    def cswap(@Nullable args: String) = breakable {
        if (!isConnected)
            adviseToConnect()
        else {
            val argLst = parseArgs(args)

            val cacheArg = argValue("c", argLst)

            if (cacheArg.isEmpty)
                scold("Cache name is empty.").^^

            val caches = getVariable(cacheArg.get)

            val prj = grid.forCache(caches)

            if (prj.isEmpty) {
                val msg =
                    if (caches == null)
                        "Can't find nodes with default cache."
                    else
                        "Can't find nodes with specified cache: " + caches

                scold(msg).^^
            }

            val res = prj.compute()
                .withName("visor-cswap-task")
                .withNoFailover()
                .broadcast(new SwapCommand(caches)).get

            val t = VisorTextTable()

            t #= ("Node ID8(@)", "Entries Swapped", "Cache Size Before", "Cache Size After")

            res.foreach(r => t += (nodeId8(r._1), r._2, r._3, r._4))

            t.render()
        }
    }

    /**
     * ===Command===
     * Swaps entries in default cache.
     *
     * ===Examples===
     * <ex>cswap</ex>
     * Swaps entries in default cache.
     */
    def cswap() {
        cswap(null)
    }
}

/**
 *
 */
@GridInternal
class SwapCommand(val cacheName: String) extends GridCallable[(UUID, Int, Int, Int)] {
    @GridInstanceResource
    private val g: Grid = null

    @impl def call(): (UUID, Int, Int, Int) = {
        val c = g.asInstanceOf[GridEx].cachex[AnyRef, AnyRef](cacheName)

        val oldSize = c.size

        val cnt = (c.entrySet :\ 0)((e, cnt) => if (e.backup && e.evict()) cnt + 1 else cnt)

        (g.localNode.id, cnt, oldSize, c.size)
    }
}

/**
 * Companion object that does initialization of the command.
 */
object VisorCacheSwapCommand {
    addHelp(
        name = "cswap",
        shortInfo = "Swaps backup entries in cache on all nodes.",
        spec = List(
            "cswap",
            "cswap -c=<cache-name>"
        ),
        args = List(
            "<cache-name>" -> List(
                "Name of the cache.",
                "If not specified, entries in default cache will be swapped.",
                "Note you can also use '@c0' ... '@cn' variables as shortcut to <cache-name>."
            )
        ),
        examples = List(
            "cswap" -> "Swaps entries in default cache.",
            "cswap -c=cache" -> "Swaps entries in cache with name 'cache'.",
            "cswap -c=@c0" -> "Swaps entries in cache with name taken from 'c0' memory variable."
        ),
        ref = VisorConsoleCommand(cmd.cswap, cmd.cswap)
    )

    /** Singleton command. */
    private val cmd = new VisorCacheSwapCommand

    /**
     * Singleton.
     */
    def apply() = cmd

    /**
     * Implicit converter from visor to commands "pimp".
     *
     * @param vs Visor tagging trait.
     */
    implicit def fromCSwap2Visor(vs: VisorTag) = cmd
}
