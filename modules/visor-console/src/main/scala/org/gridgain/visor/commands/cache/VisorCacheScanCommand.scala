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

package org.gridgain.visor.commands.cache

import org.gridgain.grid.GridNode
import org.gridgain.grid.kernal.visor.cmd.dto.VisorQueryResult
import org.gridgain.grid.kernal.visor.cmd.tasks.VisorQueryTask.VisorQueryArg
import org.gridgain.grid.kernal.visor.cmd.tasks.{VisorQueryNextPageTask, VisorQueryTask}
import org.gridgain.grid.lang.GridBiTuple
import org.gridgain.visor.commands._
import org.gridgain.visor.visor._

import scala.collection.JavaConversions._

/**
 * ==Overview==
 * Visor 'scan' command implementation.
 *
 * ====Specification====
 * {{{
 *     cache {-id=<node-id>|-id8=<node-id8>} {-p=<page size>} -c=<cache name> -scan
 * }}}
 *
 * ====Arguments====
 * {{{
 *     <node-id>
 *         Full node ID.
 *     <node-id8>
 *         Node ID8.
 *     <page size>
 *         Number of object to fetch from cache at once.
 *     <cache-name>
 *         Name of the cache.
 * }}}
 *
 * ====Examples====
 * {{{
 *    cache -c=cache
 *        List entries from cache with name 'cache' from all nodes with this cache.
 *    cache -c=@c0 -scan -p=50
 *        List entries from cache with name taken from 'c0' memory variable with page of 50 items
 *        from all nodes with this cache.
 *    cache -c=cache -scan -id8=12345678
 *        List entries from cache with name 'cache' and node '12345678' ID8.
 * }}}
 */
class VisorCacheScanCommand {
    /**
     * Prints error message and advise.
     *
     * @param errMsgs Error messages.
     */
    private def scold(errMsgs: Any*) {
        assert(errMsgs != null)

        warn(errMsgs: _*)
        warn("Type 'help cache' to see how to use this command.")
    }

    private def error(e: Exception) {
        var cause: Throwable = e

        while (cause.getCause != null)
            cause = cause.getCause

        scold(cause.getMessage)
    }

    /**
     * ===Command===
     * List all entries in cache with specified name.
     *
     * ===Examples===
     * <ex>cache -c=cache -scan</ex>
     *     List entries from cache with name 'cache' from all nodes with this cache.
     * <br>
     * <ex>cache -c=@c0 -scan -p=50</ex>
     *     List entries from cache with name taken from 'c0' memory variable with page of 50 items
     *     from all nodes with this cache.
     * <br>
     * <ex>cache -c=cache -scan -id8=12345678</ex>
     *     List entries from cache with name 'cache' and node '12345678' ID8.
     *
     * @param argLst Command arguments.
     */
    def scan(argLst: ArgList, node: Option[GridNode]) {
        val pageArg = argValue("p", argLst)
        val cacheArg = argValue("c", argLst)

        var pageSize = 25

        if (pageArg.isDefined) {
            val page = pageArg.get

            try
             pageSize = page.toInt
            catch {
                case nfe: NumberFormatException =>
                    scold("Invalid value for 'page size': " + page)

                    return
            }

            if (pageSize < 1 || pageSize > 100) {
                scold("'Page size' should be in range [1..100] but found: " + page)

                return
            }
        }

        val cacheName = cacheArg match {
            case None => null // default cache.

            case Some(s) if s.startsWith("@") =>
                warn("Can't find cache variable with specified name: " + s,
                    "Type 'cache' to see available cache variables."
                )

                return

            case Some(name) => name
        }

        val cachePrj = node match {
            case Some(n) => grid.forNode(n).forCache(cacheName)
            case _ => grid.forCache(cacheName)
        }

        if (cachePrj.nodes().isEmpty) {
            warn("Can't find nodes with specified cache: " + cacheName,
                "Type 'cache' to see available cache names."
            )

            return
        }

        val qryPrj = cachePrj.forRandom()
        val proj = new java.util.HashSet(cachePrj.nodes().map(_.id()))

        val nid = qryPrj.node().id()

        val fullRes =
            try
                qryPrj
                    .compute()
                    .withName("visor-cscan-task")
                    .withNoFailover()
                    .execute(classOf[VisorQueryTask],
                        toTaskArgument(nid, new VisorQueryArg(proj, cacheName, "SCAN", pageSize)))
                    .get match {
                    case x if x.get1() != null =>
                        error(x.get1())

                        return
                    case x => x.get2()
                }
            catch {
                case e: Exception =>
                    error(e)

                    return
            }

        def escapeCacheName(name: String) = if (name == null) "<default>" else name

        var res: VisorQueryResult = fullRes

        if (res.rows.isEmpty) {
            println("Cache: " + escapeCacheName(cacheName) + " is empty")

            return
        }

        def render() {
            println("Entries in cache: " + escapeCacheName(cacheName))

            val t = VisorTextTable()

            t #= ("Key Class", "Key", "Value Class", "Value")

            res.rows.foreach(r => t += (r(0), r(1), r(2), r(3)))

            t.render()
        }

        render()

        while (res.hasMore) {
            ask("\nFetch more objects (y/n) [y]:", "y") match {
                case "y" | "Y" =>
                    try {
                        res = qryPrj.compute()
                            .withName("visor-cscan-fetch-task")
                            .withNoFailover()
                            .execute(classOf[VisorQueryNextPageTask],
                                toTaskArgument(nid, new GridBiTuple[String, Integer](fullRes.queryId(), pageSize)))
                            .get

                        render()
                    }
                    catch {
                        case e: Exception => error(e)
                    }
                case _ => return
            }

        }
    }
}

/**
 * Companion object that does initialization of the command.
 */
object VisorCacheScanCommand {
    /** Singleton command. */
    private val cmd = new VisorCacheScanCommand

    /**
     * Singleton.
     */
    def apply() = cmd
}
