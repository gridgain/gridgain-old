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

import java.lang.{Boolean => JavaBoolean}
import java.util.UUID

import org.gridgain.grid._
import org.gridgain.grid.kernal.visor.cmd.dto.{VisorGridConfig, VisorCacheAggregatedMetrics, VisorCacheMetrics}
import org.gridgain.grid.kernal.visor.cmd.dto.cache.VisorCacheConfig
import org.gridgain.grid.kernal.visor.cmd.tasks.{VisorConfigCollectorTask, VisorCacheCollectMetricsTask}
import org.gridgain.grid.lang.GridBiTuple
import org.gridgain.grid.util.typedef._
import org.gridgain.visor._
import org.gridgain.visor.commands.cache.VisorCacheCommand._
import org.gridgain.visor.commands.{VisorConsoleCommand, VisorTextTable}
import org.gridgain.visor.visor._
import org.jetbrains.annotations._

import scala.collection.JavaConversions._
import scala.language.{implicitConversions, reflectiveCalls}
import scala.util.control.Breaks._

/**
 * ==Overview==
 * Visor 'cache' command implementation.
 *
 * ==Help==
 * {{{
 * +-----------------------------------------------------------------------------------------+
 * | cache          | Prints statistics about caches from specified node on the entire grid. |
 * |                | Output sorting can be specified in arguments.                          |
 * |                |                                                                        |
 * |                | Output abbreviations:                                                  |
 * |                |     #   Number of nodes.                                               |
 * |                |     H/h Number of cache hits.                                          |
 * |                |     M/m Number of cache misses.                                        |
 * |                |     R/r Number of cache reads.                                         |
 * |                |     W/w Number of cache writes.                                        |
 * +-----------------------------------------------------------------------------------------+
 * | cache -compact | Compacts all entries in cache on all nodes.                            |
 * +-----------------------------------------------------------------------------------------+
 * | cache -clear   | Clears all entries from cache on all nodes.                            |
 * +-----------------------------------------------------------------------------------------+
 * | cache -scan    | List all entries in cache with specified name.                         |
 * +-----------------------------------------------------------------------------------------+
 * }}}
 *
 * ====Specification====
 * {{{
 *     cache
 *     cache -i
 *     cache {-c=<cache-name>} {-id=<node-id>|id8=<node-id8>} {-s=lr|lw|hi|mi|re|wr} {-a} {-r}
 *     cache -clear {-c=<cache-name>}
 *     cache -compact {-c=<cache-name>}
 *     cache -scan -c=<cache-name> {-id=<node-id>|id8=<node-id8>} {-p=<page size>}
 *     cache -swap {-c=<cache-name>} {-id=<node-id>|id8=<node-id8>}
 * }}}
 *
 * ====Arguments====
 * {{{
 *     -id=<node-id>
 *         Full ID of the node to get cache statistics from.
 *         Either '-id8' or '-id' can be specified.
 *         If neither is specified statistics will be gathered from all nodes.
 *     -id8=<node-id>
 *         ID8 of the node to get cache statistics from.
 *         Either '-id8' or '-id' can be specified.
 *         If neither is specified statistics will be gathered from all nodes.
 *     -c=<cache-name>
 *         Name of the cache.
 *     -s=lr|lw|hi|mi|re|wr|cn
 *         Defines sorting type. Sorted by:
 *            lr Last read.
 *            lw Last write.
 *            hi Hits.
 *            mi Misses.
 *            rd Reads.
 *            wr Writes.
 *         If not specified - default sorting is 'lr'.
 *     -i
 *         Interactive mode.
 *         User can interactively select node for cache statistics.
 *     -r
 *         Defines if sorting should be reversed.
 *         Can be specified only with '-s' argument.
 *     -a
 *         Prints details statistics about each cache.
 *         By default only aggregated summary is printed.
 *     -compact
 *          Compacts entries in cache.
 *     -clear
 *          Clears cache.
 *     -scan
 *          Prints list of all entries from cache.
 *     -swap
 *          Swaps backup entries in cache.
 *     -p=<page size>
 *         Number of object to fetch from cache at once.
 *         Valid range from 1 to 100.
 *         By default page size is 25.
 * }}}
 *
 * ====Examples====
 * {{{
 *     cache
 *         Prints summary statistics about all caches.
 *     cache -id8=12345678 -s=hi -r
 *         Prints summary statistics about caches from node with specified id8
 *         sorted by number of hits in reverse order.
 *     cache -i
 *         Prints cache statistics for interactively selected node.
 *     cache -s=hi -r -a
 *         Prints detailed statistics about all caches sorted by number of hits in reverse order.
 *     cache -compact
 *         Compacts entries in interactively selected cache.
 *     cache -compact -c=cache
 *         Compacts entries in cache with name 'cache'.
 *     cache -clear
 *         Clears interactively selected cache.
 *     cache -clear -c=cache
 *         Clears cache with name 'cache'.
 *     cache -scan
 *         Prints list entries from interactively selected cache.
 *     cache -scan -c=cache
 *         Prints list entries from cache with name 'cache' from all nodes with this cache.
 *     cache -scan -c=@c0 -p=50
 *         Prints list entries from cache with name taken from 'c0' memory variable
 *         with page of 50 items from all nodes with this cache.
 *     cache -scan -c=cache -id8=12345678
 *         Prints list entries from cache with name 'cache' and node '12345678' ID8.
 *     cache -swap
 *         Swaps entries in interactively selected cache.
 *     cache -swap -c=cache
 *         Swaps entries in cache with name 'cache'.
 *     cache -swap -c=@c0
 *         Swaps entries in cache with name taken from 'c0' memory variable.
 * }}}
 */
class VisorCacheCommand {
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

    /**
     * ===Command===
     * Prints statistics about caches from nodes that pass mnemonic predicate.
     * Sorting can be specified in arguments.
     *
     * ===Examples===
     * <ex>cache -id8=12345678 -s=no -r</ex>
     *     Prints statistics about caches from node with specified id8 sorted by number of nodes in reverse order.
     * <br>
     * <ex>cache -s=no -r</ex>
     *     Prints statistics about all caches sorted by number of nodes in reverse order.
     * <br>
     * <ex>cache -compact</ex>
     *      Compacts entries in interactively selected cache.
     * <br>
     * <ex>cache -compact -c=cache</ex>
     *      Compacts entries in cache with name 'cache'.
     * <br>
     * <ex>cache -clear</ex>
     *      Clears interactively selected cache.
     * <br>
     * <ex>cache -clear -c=cache</ex>
     *      Clears cache with name 'cache'.
     * <br>
     * <ex>cache -scan</ex>
     *     Prints list entries from interactively selected cache.
     * <br>
     * <ex>cache -scan -c=cache</ex>
     *     Prints list entries from cache with name 'cache' from all nodes with this cache.
     * <br>
     * <ex>cache -scan -c=@c0 -p=50</ex>
     *     Prints list entries from cache with name taken from 'c0' memory variable with page of 50 items
     *     from all nodes with this cache.
     * <br>
     * <ex>cache -scan -c=cache -id8=12345678</ex>
     *     Prints list entries from cache with name 'cache' and node '12345678' ID8.
     * <br>
     * <ex>cache -swap</ex>
     *     Swaps entries in interactively selected cache.
     * <br>
     * <ex>cache -swap -c=cache</ex>
     *     Swaps entries in cache with name 'cache'.
     * <br>
     * <ex>cache -swap -c=@c0</ex>
     *     Swaps entries in cache with name taken from 'c0' memory variable.
     *
     * @param args Command arguments.
     */
    def cache(args: String) {
        breakable {
            if (!isConnected)
                adviseToConnect()
            else {
                var argLst = parseArgs(args)

                if (hasArgFlag("i", argLst)) {
                    askForNode("Select node from:") match {
                        case Some(nid) => ask("Detailed statistics (y/n) [n]: ", "n") match {
                            case "n" | "N" => nl(); cache("-id=" + nid).^^
                            case "y" | "Y" => nl(); cache("-a -id=" + nid).^^
                            case x => nl(); warn("Invalid answer: " + x).^^
                        }
                        case None => break()
                    }

                    break()
                }

                val node = parseNode(argLst) match {
                    case Left(msg) =>
                        scold(msg)

                        break()

                    case Right(n) => n
                }

                val cacheName = argValue("c", argLst) match {
                    case Some("<default>") | Some(CACHE_DFLT) =>
                        argLst = argLst.filter(_._1 != "c") ++ Seq("c" -> null)

                        Some(null)
                    case cn => cn
                }

                if (Seq("clear", "compact", "swap", "scan").exists(hasArgFlag(_, argLst))) {
                    if (cacheName.isEmpty)
                        askForCache("Select cache from:", node) match {
                            case Some(name) => argLst = argLst ++ Seq("c" -> name)
                            case None => break()
                        }

                    if (hasArgFlag("clear", argLst))
                        VisorCacheClearCommand().clear(argLst, node)
                    else if (hasArgFlag("compact", argLst))
                        VisorCacheCompactCommand().compact(argLst, node)
                    else if (hasArgFlag("swap", argLst))
                        VisorCacheSwapCommand().swap(argLst, node)
                    else if (hasArgFlag("scan", argLst))
                        VisorCacheScanCommand().scan(argLst, node)

                    break()
                }

                val all = hasArgFlag("a", argLst)

                val sortType = argValue("s", argLst)
                val reversed = hasArgName("r", argLst)

                if (sortType.isDefined && !isValidSortType(sortType.get))
                    scold("Invalid '-s' argument in: " + args).^^

                // Get cache stats data from all nodes.
                val aggrData = cacheData(node, cacheName)

                if (aggrData.isEmpty)
                    scold("No caches found.").^^

                println("Time of the snapshot: " + formatDateTime(System.currentTimeMillis))

                val sumT = VisorTextTable()

                sumT #= (("Name(@),", "Last Read/Write"), "Nodes", "Entries", "Hits", "Misses", "Reads", "Writes")

                sortAggregatedData(aggrData, sortType getOrElse "lr", reversed).foreach(
                    ad => {
                        // Add cache host as visor variable.
                        registerCacheName(ad.cacheName)

                        sumT += (
                            (
                                mkCacheName(ad.cacheName),
                                " ",
                                formatDateTime(ad.lastRead),
                                formatDateTime(ad.lastWrite)
                                ),
                            ad.nodes,
                            (
                                "min: " + ad.minSize,
                                "avg: " + formatDouble(ad.avgSize),
                                "max: " + ad.maxSize
                                ),
                            (
                                "min: " + ad.minHits,
                                "avg: " + formatDouble(ad.avgHits),
                                "max: " + ad.maxHits
                                ),
                            (
                                "min: " + ad.minMisses,
                                "avg: " + formatDouble(ad.avgMisses),
                                "max: " + ad.maxMisses
                                ),
                            (
                                "min: " + ad.minReads,
                                "avg: " + formatDouble(ad.avgReads),
                                "max: " + ad.maxReads
                                ),
                            (
                                "min: " + ad.minWrites,
                                "avg: " + formatDouble(ad.avgWrites),
                                "max: " + ad.maxWrites
                                )
                            )
                    }
                )

                sumT.render()

                if (all) {
                    val sorted = aggrData.sortWith((k1, k2) => {
                        if (k1.cacheName == null)
                            true
                        else if (k2.cacheName == null)
                            false
                        else k1.cacheName.compareTo(k2.cacheName) < 0
                    })

                    val gCfg = node.map(config).collect {
                        case cfg if cfg != null => cfg
                    }

                    sorted.foreach(ad => {
                        val cacheNameVar = mkCacheName(ad.cacheName)

                        println("\nCache '" + cacheNameVar + "':")

                        val csT = VisorTextTable()

                        csT += ("Name(@)", cacheNameVar)
                        csT += ("Nodes", ad.nodes.size)
                        csT += ("Size Min/Avg/Max", ad.minSize + " / " + formatDouble(ad.avgSize) + " / " + ad.maxSize)

                        val ciT = VisorTextTable()

                        ciT #= ("Node ID8(@), IP", "CPUs", "Heap Used", "CPU Load", "Up Time", "Size",
                            "Last Read/Write", "Hi/Mi/Rd/Wr")

                        sortData(ad.metrics(), sortType getOrElse "lr", reversed).
                            foreach(cd => {
                                ciT += (
                                    nodeId8Addr(cd.nodeId),
                                    cd.cpus,
                                    formatDouble(cd.heapUsed) + " %",
                                    formatDouble(cd.cpuLoad) + " %",
                                    X.timeSpan2HMSM(cd.upTime),
                                    cd.size,
                                    (
                                        formatDateTime(cd.lastRead),
                                        formatDateTime(cd.lastWrite)
                                        ),
                                    (
                                        "Hi: " + cd.hits,
                                        "Mi: " + cd.misses,
                                        "Rd: " + cd.reads,
                                        "Wr: " + cd.writes
                                        )
                                    )
                            })

                        csT.render()

                        nl()
                        println("Nodes for: " + cacheNameVar)

                        ciT.render()

                        // Print footnote.
                        println("'Hi' - Number of cache hits.")
                        println("'Mi' - Number of cache misses.")
                        println("'Rd' - number of cache reads.")
                        println("'Wr' - Number of cache writes.")

                        // Print metrics.
                        val qm = ad.queryMetrics()

                        nl()
                        println("Aggregated queries metrics:")
                        println("  Minimum execution time: " + X.timeSpan2HMSM(qm.minTime))
                        println("  Maximum execution time: " + X.timeSpan2HMSM(qm.maxTime))
                        println("  Average execution time: " + X.timeSpan2HMSM(qm.avgTime.toLong))
                        println("  Total number of executions: " + qm.execs)
                        println("  Total number of failures:   " + qm.fails)
                        
                        gCfg.foreach(_.caches().find(_.name() == ad.cacheName()).foreach(cfg => {
                            nl()

                            showCacheConfiguration("Cache configuration:", cfg)
                        }))
                    })

                }
                else
                    println("\nUse \"-a\" flag to see detailed statistics.")
            }
        }
    }

    /**
     * Makes extended cache host attaching optional visor variable host
     * associated with it.
     *
     * @param s Cache host.
     */
    private def mkCacheName(@Nullable s: String): String = {
        if (s == null) {
            val v = mfind(CACHE_DFLT)

            "<default>" + (if (v.isDefined) "(@" + v.get._1 + ')' else "")
        }
        else {
            val v = mfind(s)

            s + (if (v.isDefined) "(@" + v.get._1 + ')' else "")
        }
    }

    /**
     * Registers cache host as a visor variable if one wasn't already registered.
     *
     * @param s Cache host.
     */
    private def registerCacheName(@Nullable s: String) = setVarIfAbsent(if (s != null) s else CACHE_DFLT, "c")

    /**
     * ===Command===
     * Prints unsorted statistics about all caches.
     *
     * ===Examples===
     * <ex>cache</ex>
     * Prints unsorted statistics about all caches.
     */
    def cache() {
        this.cache("")
    }

    /**
     * Get metrics data for all caches from all node or from specified node.
     *
     * @return Caches metrics data.
     */
    private def cacheData(node: Option[GridNode], name: Option[String]): List[VisorCacheAggregatedMetrics] = {
        assert(node != null)

        try {
            val prj = node.fold(grid.forRemotes())(grid.forNode(_))

            val nids = prj.nodes().map(_.id())

            prj.compute().execute(classOf[VisorCacheCollectMetricsTask], toTaskArgument(nids,
                new GridBiTuple(new JavaBoolean(name.isEmpty), name.orNull))).get.toList
        }
        catch {
            case e: GridException => Nil
        }
    }

    /**
     * Gets configuration of grid from specified node for callecting of node cache's configuration.
     *
     * @param node Specified node.
     * @return Grid configuration for specified node.
     */
    private def config(node: GridNode): VisorGridConfig = {
        try
            grid.forNode(node).compute().withNoFailover()
                .execute(classOf[VisorConfigCollectorTask], emptyTaskArgument(node.id())).get
        catch {
            case e: GridException =>
                scold(e.getMessage)

                null
        }
    }

    /**
     * Tests whether passed in parameter is a valid sorting type.
     *
     * @param arg Sorting type to test.
     */
    private def isValidSortType(arg: String): Boolean = {
        assert(arg != null)

        Set("lr", "lw", "hi", "mi", "rd", "wr", "cn").contains(arg.trim)
    }

    /**
     * Sort metrics data.
     *
     * @param data Unsorted list.
     * @param arg Sorting command argument.
     * @param reverse Whether to reverse sorting or not.
     * @return Sorted data.
     */
    private def sortData(data: Iterable[VisorCacheMetrics], arg: String, reverse: Boolean): List[VisorCacheMetrics] = {
        assert(data != null)
        assert(arg != null)

        val sorted = arg.trim match {
            case "lr" => data.toList.sortBy(_.lastRead)
            case "lw" => data.toList.sortBy(_.lastWrite)
            case "hi" => data.toList.sortBy(_.hits)
            case "mi" => data.toList.sortBy(_.misses)
            case "rd" => data.toList.sortBy(_.reads)
            case "wr" => data.toList.sortBy(_.writes)
            case "cn" => data.toList.sortWith((x, y) => x.cacheName == null ||
                x.cacheName.toLowerCase < y.cacheName.toLowerCase)

            case _ =>
                assert(false, "Unknown sorting type: " + arg)

                Nil
        }

        if (reverse) sorted.reverse else sorted
    }

    /**
     * Sort aggregated metrics data.
     *
     * @param data Unsorted list.
     * @param arg Command argument.
     * @param reverse Whether to reverse sorting or not.
     * @return Sorted data.
     */
    private def sortAggregatedData(data: Iterable[VisorCacheAggregatedMetrics], arg: String, reverse: Boolean):
        List[VisorCacheAggregatedMetrics] = {

        val sorted = arg.trim match {
            case "lr" => data.toList.sortBy(_.lastRead)
            case "lw" => data.toList.sortBy(_.lastWrite)
            case "hi" => data.toList.sortBy(_.avgHits)
            case "mi" => data.toList.sortBy(_.avgMisses)
            case "rd" => data.toList.sortBy(_.avgReads)
            case "wr" => data.toList.sortBy(_.avgWrites)
            case "cn" => data.toList.sortWith((x, y) =>
                x.cacheName == null || (y.cacheName != null && x.cacheName.toLowerCase < y.cacheName.toLowerCase))

            case _ =>
                assert(false, "Unknown sorting type: " + arg)

                Nil
        }

        if (reverse) sorted.reverse else sorted
    }

    /**
     * Asks user to select a cache from the list.
     *
     * @param title Title displayed before the list of caches.
     * @return `Option` for ID of selected cache.
     */
    def askForCache(title: String, node: Option[GridNode]): Option[String] = {
        assert(title != null)
        assert(visor.isConnected)

        // Get cache stats data from all nodes.
        val aggrData = cacheData(node, None)

        if (aggrData.isEmpty)
            scold("No caches found.").^^

        val sortedAggrData = sortAggregatedData(aggrData, "cn", false)

        println("Time of the snapshot: " + formatDateTime(System.currentTimeMillis))

        val sumT = VisorTextTable()

        sumT #= ("#", ("Name(@),", "Last Read/Write"), "Nodes", "Size")

        (0 until sortedAggrData.size) foreach (i => {
            val ad = sortedAggrData(i)

            // Add cache host as visor variable.
            registerCacheName(ad.cacheName)

            sumT += (
                i,
                (
                    mkCacheName(ad.cacheName),
                    " ",
                    formatDateTime(ad.lastRead),
                    formatDateTime(ad.lastWrite)
                    ),
                ad.nodes,
                (
                    "min: " + ad.minSize,
                    "avg: " + formatDouble(ad.avgSize),
                    "max: " + ad.maxSize
                ))
        })

        sumT.render()

        val a = ask("\nChoose cache number ('c' to cancel) [c]: ", "c")

        if (a.toLowerCase == "c")
            None
        else {
            try
                Some(sortedAggrData(a.toInt).cacheName)
            catch {
                case e: Throwable =>
                    warn("Invalid selection: " + a)

                    None
            }
        }
    }
}

/**
 * Companion object that does initialization of the command.
 */
object VisorCacheCommand {
    addHelp(
        name = "cache",
        shortInfo = "Prints cache statistics, clears cache, compacts entries in cache, prints list of all entries from cache.",
        longInfo = Seq(
            "Prints statistics about caches from specified node on the entire grid.",
            "Output sorting can be specified in arguments.",
            " ",
            "Output abbreviations:",
            "    #   Number of nodes.",
            "    H/h Number of cache hits.",
            "    M/m Number of cache misses.",
            "    R/r Number of cache reads.",
            "    W/w Number of cache writes.",
            " ",
            "Clears cache.",
            " ",
            "Compacts entries in cache.",
            " ",
            "Prints list of all entries from cache.",
            " ",
            "Swaps backup entries in cache."
        ),
        spec = Seq(
            "cache",
            "cache -i",
            "cache {-c=<cache-name>} {-id=<node-id>|id8=<node-id8>} {-s=lr|lw|hi|mi|re|wr} {-a} {-r}",
            "cache -compact {-c=<cache-name>} {-id=<node-id>|id8=<node-id8>}",
            "cache -clear {-c=<cache-name>} {-id=<node-id>|id8=<node-id8>}",
            "cache -scan -c=<cache-name> {-id=<node-id>|id8=<node-id8>} {-p=<page size>}",
            "cache -swap {-c=<cache-name>} {-id=<node-id>|id8=<node-id8>}"
    ),
        args = Seq(
            "-id=<node-id>" -> Seq(
                "Full ID of the node to get cache statistics from.",
                "Either '-id8' or '-id' can be specified.",
                "If neither is specified statistics will be gathered from all nodes."
            ),
            "-id8=<node-id>" -> Seq(
                "ID8 of the node to get cache statistics from.",
                "Either '-id8' or '-id' can be specified.",
                "If neither is specified statistics will be gathered from all nodes.",
                "Note you can also use '@n0' ... '@nn' variables as shortcut to <node-id>."
            ),
            "-c=<cache-name>" -> Seq(
                "Name of the cache.",
                "Note you can also use '@c0' ... '@cn' variables as shortcut to <cache-name>."
            ),
            "-compact" -> Seq(
                "Compacts entries in cache."
            ),
            "-clear" -> Seq(
                "Clears cache."
            ),
            "-scan" -> Seq(
                "Prints list of all entries from cache."
            ),
            "-swap" -> Seq(
                "Swaps backup entries in cache."
            ),
            "-s=lr|lw|hi|mi|re|wr|cn" -> Seq(
                "Defines sorting type. Sorted by:",
                "   lr Last read.",
                "   lw Last write.",
                "   hi Hits.",
                "   mi Misses.",
                "   rd Reads.",
                "   wr Writes.",
                "   cn Cache name.",
                "If not specified - default sorting is 'lr'."
            ),
            "-i" -> Seq(
                "Interactive mode.",
                "User can interactively select node for cache statistics."
            ),
            "-r" -> Seq(
                "Defines if sorting should be reversed.",
                "Can be specified only with '-s' argument."
            ),
            "-a" -> Seq(
                "Prints details statistics about each cache.",
                "By default only aggregated summary is printed."
            ),
            "-p=<page size>" -> Seq(
                "Number of object to fetch from cache at once.",
                "Valid range from 1 to 100.",
                "By default page size is 25."
            )
        ),
        examples = Seq(
            "cache" ->
                "Prints summary statistics about all caches.",
            "cache -i" ->
                "Prints cache statistics for interactively selected node.",
            "cache -id8=12345678 -s=hi -r"  -> Seq(
                "Prints summary statistics about caches from node with specified id8",
                "sorted by number of hits in reverse order."
            ),
            "cache -id8=@n0 -s=hi -r"  -> Seq(
                "Prints summary statistics about caches from node with id8 taken from 'n0' memory variable.",
                "sorted by number of hits in reverse order."
            ),
            "cache -c=@c0 -a"  -> Seq(
                "Prints detailed statistics about cache with name taken from 'c0' memory variable."
            ),
            "cache -s=hi -r -a" ->
                "Prints detailed statistics about all caches sorted by number of hits in reverse order.",
            "cache -compact" -> "Compacts entries in interactively selected cache.",
            "cache -compact -c=cache" -> "Compacts entries in cache with name 'cache'.",
            "cache -compact -c=@c0" -> "Compacts cache with name taken from 'c0' memory variable.",
            "cache -clear" -> "Clears interactively selected cache.",
            "cache -clear -c=cache" -> "Clears cache with name 'cache'.",
            "cache -clear -c=@c0" -> "Clears cache with name taken from 'c0' memory variable.",
            "cache -scan" -> "Prints list entries from interactively selected cache.",
            "cache -scan -c=cache" -> "List entries from cache with name 'cache' from all nodes with this cache.",
            "cache -scan -c=@c0 -p=50" -> ("Prints list entries from cache with name taken from 'c0' memory variable" +
                " with page of 50 items from all nodes with this cache."),
            "cache -scan -c=cache -id8=12345678" -> "Prints list entries from cache with name 'cache' and node '12345678' ID8.",
            "cache -swap" -> "Swaps entries in interactively selected cache.",
            "cache -swap -c=cache" -> "Swaps entries in cache with name 'cache'.",
            "cache -swap -c=@c0" -> "Swaps entries in cache with name taken from 'c0' memory variable."
        ),
        ref = VisorConsoleCommand(cmd.cache, cmd.cache)
    )

    /** Default cache key. */
    protected val CACHE_DFLT = "<default>-" + UUID.randomUUID().toString

    /** Singleton command */
    private val cmd = new VisorCacheCommand

    /**
     * Singleton.
     */
    def apply() = cmd

    /**
     * Implicit converter from visor to commands "pimp".
     *
     * @param vs Visor tagging trait.
     */
    implicit def fromCinfo2Visor(vs: VisorTag) = cmd

    /**
     * Show table of cache configuration information.
     *
     * @param title Specified title for table.
     * @param cfg Config to show information.
     */
    private[commands] def showCacheConfiguration(title: String, cfg: VisorCacheConfig) {
        val cacheT = VisorTextTable()

        cacheT #= ("Name", "Value")

        cacheT += ("Mode", cfg.mode)
        cacheT += ("Atomicity Mode", cfg.atomicityMode)
        cacheT += ("Atomic Sequence Reserve Size", cfg.atomicSequenceReserveSize)
        cacheT += ("Atomic Write Ordering Mode", cfg.atomicWriteOrderMode)
        cacheT += ("Time To Live", cfg.defaultConfig().timeToLive())
        cacheT += ("Time To Live Eager Flag", cfg.eagerTtl)
        cacheT += ("Refresh Ahead Ratio", cfg.refreshAheadRatio)
        cacheT += ("Write Synchronization Mode", cfg.writeSynchronizationMode)
        cacheT += ("Swap Enabled", cfg.swapEnabled())
        cacheT += ("Invalidate", cfg.invalidate())
        cacheT += ("Start Size", cfg.startSize())
        cacheT += ("Cloner", cfg.cloner())
        cacheT += ("Batch Update", cfg.batchUpdateOnCommit())
        cacheT += ("Transaction Manager Lookup", cfg.transactionManagerLookupClassName())
        cacheT += ("Transaction Serializable", cfg.txSerializableEnabled)
        cacheT += ("Affinity Function", cfg.affinityConfig().function())
        cacheT += ("Affinity Backups", cfg.affinityConfig().partitionedBackups())
        cacheT += ("Affinity Partitions", cfg.affinityConfig().partitions())
        cacheT += ("Affinity Default Replicas", cfg.affinityConfig().defaultReplicas())
        cacheT += ("Affinity Exclude Neighbors", cfg.affinityConfig().excludeNeighbors())
        cacheT += ("Affinity Mapper", cfg.affinityConfig().mapper())
        cacheT += ("Preload Mode", cfg.preloadConfig().mode())
        cacheT += ("Preload Batch Size", cfg.preloadConfig().batchSize())
        cacheT += ("Preload Thread Pool size", cfg.preloadConfig().threadPoolSize())
        cacheT += ("Preload Timeout", cfg.preloadConfig().timeout())
        cacheT += ("Preloading Delay", cfg.preloadConfig().partitionedDelay())
        cacheT += ("Time Between Preload Messages", cfg.preloadConfig.throttle())
        cacheT += ("Eviction Policy Enabled", cfg.evictConfig().policy() != null)
        cacheT += ("Eviction Policy", cfg.evictConfig().policy())
        cacheT += ("Eviction Policy Max Size", cfg.evictConfig.policyMaxSize())
        cacheT += ("Eviction Filter", cfg.evictConfig().filter())
        cacheT += ("Eviction Key Buffer Size", cfg.evictConfig().synchronizedKeyBufferSize())
        cacheT += ("Eviction Synchronized", cfg.evictConfig().evictSynchronized())
        cacheT += ("Eviction Overflow Ratio", cfg.evictConfig().maxOverflowRatio())
        cacheT += ("Synchronous Eviction Timeout", cfg.evictConfig().synchronizedTimeout())
        cacheT += ("Synchronous Eviction Concurrency Level", cfg.evictConfig().synchronizedConcurrencyLevel())
        cacheT += ("Distribution Mode", cfg.distributionMode())
        cacheT += ("Near Start Size", cfg.nearConfig().nearStartSize())
        cacheT += ("Near Eviction Policy", cfg.nearConfig().nearEvictPolicy())
        cacheT += ("Near Eviction Enabled", cfg.nearConfig().nearEnabled())
        cacheT += ("Near Eviction Synchronized", cfg.evictConfig().nearSynchronized())
        cacheT += ("Default Isolation", cfg.defaultConfig().txIsolation())
        cacheT += ("Default Concurrency", cfg.defaultConfig().txConcurrency())
        cacheT += ("Default Transaction Timeout", cfg.defaultConfig().txTimeout())
        cacheT += ("Default Lock Timeout", cfg.defaultConfig().txLockTimeout())
        cacheT += ("Default Query Timeout", cfg.defaultConfig().queryTimeout())
        cacheT += ("Query Indexing Enabled", cfg.queryIndexEnabled())
        cacheT += ("Query Iterators Number", cfg.maxQueryIteratorCount())
        cacheT += ("Indexing SPI Name", cfg.indexingSpiName())
        cacheT += ("Cache Interceptor", cfg.interceptor())
        cacheT += ("DGC Frequency", cfg.dgcConfig().frequency())
        cacheT += ("DGC Remove Locks Flag", cfg.dgcConfig().removedLocks())
        cacheT += ("DGC Suspect Lock Timeout", cfg.dgcConfig().suspectLockTimeout())
        cacheT += ("Store Enabled", cfg.storeConfig().enabled())
        cacheT += ("Store", cfg.storeConfig().store())
        cacheT += ("Store Values In Bytes", cfg.storeConfig().valueBytes())
        cacheT += ("Off-Heap Size", cfg.offsetHeapMaxMemory())
        cacheT += ("Write-Behind Enabled", cfg.writeBehind().enabled())
        cacheT += ("Write-Behind Flush Size", cfg.writeBehind().flushSize())
        cacheT += ("Write-Behind Frequency", cfg.writeBehind().flushFrequency())
        cacheT += ("Write-Behind Flush Threads Count", cfg.writeBehind().flushThreadCount())
        cacheT += ("Write-Behind Batch Size", cfg.writeBehind().batchSize())
        cacheT += ("Pessimistic Tx Log Size", cfg.pessimisticTxLoggerSize())
        cacheT += ("Pessimistic Tx Log Linger", cfg.pessimisticTxLoggerLinger())
        cacheT += ("Concurrent Asynchronous Operations Number", cfg.maxConcurrentAsyncOperations())
        cacheT += ("Memory Mode", cfg.memoryMode())

        println(title)

        cacheT.render()
    }
}
