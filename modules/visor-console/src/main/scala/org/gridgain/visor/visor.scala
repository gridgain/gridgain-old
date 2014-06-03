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

import java.net._
import java.text._
import java.io._
import java.util._
import java.util.concurrent._
import org.jetbrains.annotations.Nullable
import scala.collection.immutable
import collection.JavaConversions._
import org.gridgain.grid._
import org.gridgain.grid.{GridGain => G, GridException => GE}
import org.gridgain.grid.util.lang.{GridFunc => F}
import org.gridgain.grid.events._
import org.gridgain.grid.events.GridEventType._
import org.gridgain.grid.events.GridDiscoveryEvent
import org.gridgain.grid.kernal.{GridProductImpl, GridEx}
import org.gridgain.grid.kernal.GridNodeAttributes._
import org.gridgain.grid.lang.{GridCallable, GridPredicate, GridBiTuple}
import org.gridgain.grid.spi.communication.tcp.GridTcpCommunicationSpi
import org.gridgain.grid.thread._
import org.gridgain.grid.util.typedef._
import org.gridgain.grid.util.{GridUtils => U, GridConfigurationFinder}
import org.gridgain.scalar._
import org.gridgain.scalar.scalar._
import org.gridgain.visor.commands.{VisorTextTable, VisorConsoleCommand}
import org.gridgain.grid.resources.GridInstanceResource
import org.gridgain.grid.kernal.processors.task.GridInternal
import org.gridgain.grid.util.scala.impl
import org.gridgain.grid.kernal.processors.spring.GridSpringProcessor
import org.gridgain.grid.kernal.GridComponentType._

/**
 * Holder for command help information.
 */
sealed case class VisorConsoleCommandHolder(
    name: String,
    shortInfo: String,
    longInfo: Seq[String],
    aliases: Seq[String],
    spec: Seq[String],
    args: Seq[(String, AnyRef)],
    examples: Seq[(String, AnyRef)],
    impl: VisorConsoleCommand
    ) {
    /** Command host with optional aliases. */
    lazy val nameWithAliases: String =
        if (aliases != null && !aliases.isEmpty)
            name + " (" + ("" /: aliases)((b, a) => if (b.length() == 0) a else b + ", " + a) + ")"
        else
            name
}

/**
 * ==Overview==
 * This is the '''tagging''' trait existing solely to have type associated with
 * with `visor` object so that implicit conversions can be done
 * on `visor` object itself. Implicit conversions are essential to extensibility
 * of the Visor.
 *
 * ==Example==
 * This is an example on how [[org.gridgain.visor.VisorTag]] trait is used to
 * extend `visor` natively with custom commands:
 *
 * <ex>
 * class VisorCustomCommand {
 *     def foo(@Nullable args: String) = {
 *         if (visor.hasValue("bar", visor.parse(args)))
 *             println("foobar")
 *         else
 *             println("foo")
 *     }
 *     def foo(@Nullable args: Symbol*): Unit = foo(visor.flatSymbols(args: _*))
 * }
 * object VisorCustomCommand {
 *     implicit def fromVisor(vs: VisorTag) = new VisorCustomCommand
 * }
 * </ex>
 */
trait VisorTag

/**
 * {{{
 * ___    _________________________ ________
 * __ |  / /____  _/__  ___/__  __ \___  __ \
 * __ | / /  __  /  _____ \ _  / / /__  /_/ /
 * __ |/ /  __/ /   ____/ / / /_/ / _  _, _/
 * _____/   /___/   /____/  \____/  /_/ |_|
 *
 * }}}
 *
 * ==Overview==
 * Visor console provides monitoring capabilities for GridGain.
 *
 * ==Usage==
 * GridGain ships with `GRIDGAIN_HOME/bin/ggvisorcmd.{sh|bat}` script that starts Visor console.
 *
 * Just type:<ex>help</ex> in Visor console to get help and get started.
 */
@GridNotPeerDeployable
object visor extends VisorTag {
    /** Argument type. */
    type Arg = (String, String)

    /** Type alias for command argument list. */
    type ArgList = Seq[Arg]

    /** Type alias for general node filter. */
    type NodeFilter = GridNode => Boolean

    /** Type alias for general event filter. */
    type EventFilter = GridEvent => Boolean

    /** `Nil` is for empty list, `Til` is for empty tuple. */
    val Til: Arg = (null, null)

    /** Node filter that includes any node. */
    final val ALL_NODES_FILTER = (_: GridNode) => true

    /** System line separator. */
    final val NL = System getProperty "line.separator"

    /** */
    private var cmdLst: Seq[VisorConsoleCommandHolder] = Nil

    /** Node left listener. */
    private var nodeLeftLsnr: GridPredicate[GridEvent] = null

    /** Node join listener. */
    private var nodeJoinLsnr: GridPredicate[GridEvent] = null

    /** Node segmentation listener. */
    private var nodeSegLsnr: GridPredicate[GridEvent] = null

    /** Node stop listener. */
    private var nodeStopLsnr: GridGainListener = null

    /** Visor copyright blurb. */
    private final val COPYRIGHT = GridProductImpl.COPYRIGHT

    /** */
    @volatile private var isCon: Boolean = false

    /**
     * Whether or not Visor is the owner of connection - or it
     * reused one already opened.
     */
    @volatile private var conOwner: Boolean = false

    /** */
    @volatile private var conTs: Long = 0

    /** Date time format. */
    private final val dtFmt = new SimpleDateFormat("MM/dd/yy, HH:mm:ss", Locale.US)

    /** Date format. */
    private final val dFmt = new SimpleDateFormat("MM/dd/yy", Locale.US)

    /** KB format. */
    private final val kbFmt = new DecimalFormat("###,###,###,###,###")

    /** */
    private val mem = new ConcurrentHashMap[String, String]()

    /** List of close callbacks*/
    @volatile private var cbs = Seq.empty[() => Unit]

    /** List of shutdown callbacks*/
    @volatile private var shutdownCbs = Seq.empty[() => Unit]

    /** Default log file path. */
    /**
     * Default log file path. Note that this path is relative to `GRIDGAIN_HOME/work` folder
     * if `GRIDGAIN_HOME` system or environment variable specified, otherwise it is relative to
     * `work` folder under system `java.io.tmpdir` folder.
     */
    private final val DFLT_LOG_PATH = "visor/visor-log"

    /** Default configuration path relative to GridGain home. */
    private final val DFLT_CFG = "config/default-config.xml"

    /** Log file. */
    private var logFile: File = null

    /** Log timer. */
    private var logTimer: Timer = null

    /** Topology log timer. */
    private var topTimer: Timer = null

    /** Log started flag. */
    @volatile private var logStarted = false

    /** Remote log disabled flag. */
    @volatile private var rmtLogDisabled = false

    /** Internal thread pool. */
    @volatile var pool: ExecutorService = null

    /** Configuration file path, if any. */
    @volatile var cfgPath: String = null

    /** */
    @volatile var grid: GridEx = null

    /**
     * Get grid node for specified ID.
     *
     * @param nid Node ID.
     * @return GridNode instance.
     * @throws GridException if Visor is disconnected or node not found.
     */
    def node(nid: UUID): GridNode = {
        val g = grid

        if (g == null)
            throw new GridException("Visor disconnected")
        else {
            val node = g.node(nid)

            if (node == null)
                throw new GridException("Node is gone: " + nid)

            node
        }
    }

    // Asserts to make sure visor doesn't get peer deployed.
    // Property '-DVISOR' is only set in ggvisor.{sh|bat} scripts.
    assert(System.getProperty("VISOR") != null, "Visor is instantiating on non-visor node.")

    Runtime.getRuntime.addShutdownHook(new Thread() {
        override def run() {
            try
                if (grid != null && isConnected) {
                    // Call all shutdown callbacks.
                    shutdownCbs foreach(_.apply())

                    close() // This will stop the grid too if Visor is connection owner.
                }
            catch {
                case ignore: Throwable => // ignore
            }
        }
    })

    addHelp(
        name = "mlist",
        shortInfo = "Prints visor memory variables.",
        spec = Seq(
            "mlist {arg}"
        ),
        args = Seq(
            "arg" ->
                "String that contains start characters of variable names."
        ),
        examples = Seq(
            "mlist" ->
                "Prints out all visor memory variables.",
            "mlist ac" ->
                "Lists variables that start with 'a' or 'c' from visor memory."
        ),
        ref = VisorConsoleCommand(mlist, mlist)
    )

    addHelp(
        name = "mclear",
        shortInfo = "Clears visor memory variables.",
        spec = Seq(
            "mclear",
            "mclear <name>|-ev|-al|-ca|-no|-tn|-ex"
        ),
        args = Seq(
            "<name>" -> Seq(
                "Variable name to clear.",
                "Note that name doesn't include '@' symbol used to reference variable."
            ),
            "-ev" ->
                "Clears all 'event' variables.",
            "-al" ->
                "Clears all 'alert' variables.",
            "-ca" ->
                "Clears all 'cache' variables.",
            "-no" ->
                "Clears all 'node' variables.",
            "-tn" ->
                "Clears all 'task name' variables.",
            "-ex" ->
                "Clears all 'task execution' variables."
        ),
        examples = Seq(
            "mclear" ->
                "Clears all visor variables.",
            "mclear -ca" ->
                "Clears all visor cache variables.",
            "mclear n2" ->
                "Clears 'n2' visor variable."
        ),
        ref = VisorConsoleCommand(mclear, mclear)
    )

    addHelp(
        name = "mget",
        shortInfo = "Gets visor memory variable.",
        longInfo = Seq(
            "Gets visor memory variable. Variable can be referenced with '@' prefix."
        ),
        spec = Seq(
            "mget <@v>"
        ),
        args = Seq(
            "@v" ->
                "Variable name."
        ),
        examples = Seq(
            "mget <@v>" ->
                "Gets visor variable whose name is referenced by variable 'v'."
        ),
        ref = VisorConsoleCommand(mget, mget)
    )

    addHelp(
        name = "help",
        shortInfo = "Prints visor help.",
        aliases = Seq("?"),
        spec = Seq(
            "help {c1 c2 ... ck}"
        ),
        args = Seq(
            "ck" ->
                "Command to get help for."
        ),
        examples = Seq(
            "help status" ->
                "Prints help for 'status' command.",
            "help" ->
                "Prints help for all command."
        ),
        ref = VisorConsoleCommand(help, help)
    )

    addHelp(
        name = "status",
        shortInfo = "Prints visor status.",
        aliases = Seq("!"),
        spec = Seq(
            "status {-q}"
        ),
        args = Seq(
            "-q" ->
                "Quite output without ASCII logo."
        ),
        examples = Seq(
            "status" ->
                "Prints visor status.",
            "status -q" ->
                "Prints visor status in quiet mode."
        ),
        ref = VisorConsoleCommand(status, status)
    )

    addHelp(
        name = "open",
        shortInfo = "Connects visor to the grid.",
        longInfo = Seq(
            "Connects visor to the grid. Note that P2P class loading",
            "should be enabled on all nodes.",
            " ",
            "If neither '-cpath' or '-d' are provided, command will ask",
            "user to select XML configuration file in interactive mode."
        ),
        spec = Seq(
            "open {-cpath=<path>} {-g=<gridName>} {-dl}",
            "open {-d} {-g=<gridName>} {-dl}",
            "open {-e} {-g=<gridName>} {-dl}",
            "open"
        ),
        args = Seq(
            "-cpath=<path>" -> Seq(
                "Spring configuration path.",
                "Can be absolute, relative to GRIDGAIN_HOME or any well formed URL."
            ),
            "-g=<gridName>" -> Seq(
                "Optional grid name.",
                "Can be used with '-cpath', '-d' and '-e'."
            ),
            "-d" -> Seq(
                "Flag forces the command to connect to the default grid",
                "without interactive mode."
            ),
            "-e" -> Seq(
                "Flag forces the command to connect to the existing grid",
                "without interactive mode. If there is no existing grid",
                "command will fail."
            ),
            "-dl" -> Seq(
                "Flag disables remote log collection."
            )
        ),
        examples = Seq(
            "open" ->
                "Prompts user to select XML Spring configuration file in interactive mode.",
            "open -d" ->
                "Connects visor using default XML configuration.",
            "open -g=mygrid" ->
                "Connects visor to 'mygrid' grid using default configuration.",
            "open -cpath=/gg/config/mycfg.xml -g=mygrid" ->
                "Connects visor to 'mygrid' grid using configuration from provided Spring file."
        ),
        ref = VisorConsoleCommand(open, open(_))
    )

    addHelp(
        name = "close",
        shortInfo = "Disconnects visor from the grid.",
        spec = Seq("close"),
        examples = Seq(
            "close" ->
                "Disconnects visor from the grid."
        ),
        ref = VisorConsoleCommand(close)
    )

    addHelp(
        name = "quit",
        shortInfo = "Quit from visor console.",
        spec = Seq("quit"),
        examples = Seq(
            "quit" ->
                "Quit from visor console."
        ),
        aliases = Seq("exit"),
        ref = VisorConsoleCommand(quit)
    )

    addHelp(
        name = "log",
        shortInfo = "Starts or stops grid-wide events logging.",
        longInfo = Seq(
            "Logging of discovery and failure grid-wide events.",
            "Logging starts by default when Visor starts.",
            " ",
            "Events are logged to a file. If path is not provided,",
            "it will log into 'GRIDGAIN_HOME/work/visor/visor-log'.",
            " ",
            "File is always opened in append mode.",
            "If file doesn't exist, it will be created.",
            " ",
            "It is often convenient to 'tail -f' the log file",
            "in a separate console window.",
            " ",
            "Log command prints periodic topology snapshots in the following format:",
            "H/N/C |1   |1   |4   |=^========..........|",
            "where:",
            "   H - Hosts",
            "   N - Nodes",
            "   C - CPUs",
            "   = - 5%-based marker of average CPU load across the topology",
            "   ^ - 5%-based marker of average heap memory used across the topology"
        ),
        spec = Seq(
            "log",
            "log -l {-f=<path>} {-p=<num>} {-t=<num>}",
            "log -s"
        ),
        args = Seq(
            "-l" -> Seq(
                "Starts logging.",
                "If logging is already started - it's no-op."
            ),
            "-f=<path>" -> Seq(
                "Provides path to the file.",
                "Path can be absolute or relative to GRIDGAIN_HOME."
            ),
            "-p=<num>" -> Seq(
                "Provides period of querying events (in seconds).",
                "Default is 10."
            ),
            "-t=<num>" -> Seq(
                "Provides period of logging topology snapshot (in seconds).",
                "Default is 20."
            ),
            "-s" -> Seq(
                "Stops logging.",
                "If logging is already stopped - it's no-op."
            )
        ),
        examples = Seq(
            "log" ->
                "Prints log status.",
            "log -l -f=/home/user/visor-log" ->
                "Starts logging to file located at '/home/user/visor-log'.",
            "log -l -f=log/visor-log" ->
                "Starts logging to file located at 'GRIDGAIN_HOME/log/visor-log'.",
            "log -l -p=20" ->
                "Starts logging with querying events period of 20 seconds.",
            "log -l -t=30" ->
                "Starts logging with topology snapshot logging period of 30 seconds.",
            "log -s" ->
                "Stops logging."
        ),
        ref = VisorConsoleCommand(log, log)
    )

    logText("Visor started.")

    // Print out log explanation at the beginning.
    logText("<log>: H - Hosts")
    logText("<log>: N - Nodes")
    logText("<log>: C - CPUs")
    logText("<log>: = - 5%-based marker of average CPU load across the topology")
    logText("<log>: ^ - 5%-based marker of average heap memory used across the topology")

    /**
     * ==Command==
     * Lists visor memory variables.
     *
     * ==Examples==
     * <ex>mlist ac</ex>
     * Lists variables that start with `a` or `c` from visor memory.
     *
     * <ex>mlist</ex>
     * Lists all variables from visor memory.
     *
     * @param arg String that contains start characters of listed variables.
     *      If empty - all variables will be listed.
     */
    def mlist(arg: String) {
        assert(arg != null)

        if (mem.isEmpty)
            println("Memory is empty.")
        else {
            val t = new VisorTextTable()

            t.maxCellWidth = 70

            t #= ("Name", "Value")

            for ((k, v) <- mem.iterator.toList.sortBy(_._1) if arg == "" || arg.contains(k.charAt(0)))
                t += (k, v)

            t.render()

            nl()
            println(
                "Variable can be referenced in other commands with '@' prefix." + NL +
                "Reference can be either a flag or a parameter value." + NL +
                "\nEXAMPLE: " + NL +
                "    'help @cmd' - where 'cmd' variable contains command name." + NL +
                "    'node -id8=@n11' - where 'n11' variable contains node ID8."
            )
        }
    }

    /**
     * Shortcut for `println()`.
     */
    def nl() {
        println()
    }

    /**
     * ==Command==
     * Lists all visor memory.
     *
     * ==Examples==
     * <ex>mlist</ex>
     * Lists all variables in visor memory.
     */
    def mlist() {
        mlist("")
    }

    /**
     * Clears given visor variable or the whole namespace.
     *
     * @param arg Variable host or namespace mnemonic.
     */
    def mclear(arg: String) {
        assert(arg != null)

        arg match {
            case "-ev" => clearNamespace("e")
            case "-al" => clearNamespace("a")
            case "-ca" => clearNamespace("c")
            case "-no" => clearNamespace("n")
            case "-tn" => clearNamespace("t")
            case "-ex" => clearNamespace("s")
            case _ => mem.remove(arg)
        }
    }

    /**
     * Clears given variable namespace.
     *
     * @param namespace Namespace.
     */
    private def clearNamespace(namespace: String) {
        assert(namespace != null)

        mem.keySet.foreach(k => {
            if (k.startsWith(namespace))
                try {
                    k.substring(1).toInt

                    mem.remove(k)
                }
                catch {
                    case ignored: Throwable => // no-op
                }
        })
    }

    /**
     * Clears all visor memory.
     */
    def mclear() {
        mem.clear()
    }

    /**
     * Finds variable by its value.
     *
     * @param v Value to find by.
     */
    def mfind(@Nullable v: String): Option[(String, String)] =
        mem find(t => t._2 == v)

    /**
     * Sets visor memory variable. Note that this method '''does not'''
     * perform variable substitution on its parameters.
     *
     * @param n Name of the variable. Can't be `null`.
     * @param v Value of the variable. Can't be `null`.
     * @return Previous value.
     */
    def mset(n: String, v: String): String = {
        msetOpt(n, v).getOrElse(null)
    }

    /**
     * Sets visor memory variable. Note that this method '''does not'''
     * perform variable substitution on its parameters.
     *
     * @param n Name of the variable. Can't be `null`.
     * @param v Value of the variable. Can't be `null`.
     * @return Previous value as an option.
     */
    def msetOpt(n: String, v: String): Option[String] = {
        assert(n != null)
        assert(v != null)

        val prev = mem.get(n)

        mem.put(n, v)

        Option(prev)
    }

    /**
     * ==Command==
     * Gets visor memory variable. Note that this method '''does not'''
     * perform variable substitution on its parameters.
     *
     * ==Examples==
     * <ex>mget @a</ex>
     * Gets the value for visor variable '@a'.
     *
     * @param n Name of the variable.
     * @return Variable value or `null` if such variable doesn't exist or its value was set as `null`.
     */
    def mget(n: String) {
        val key = if (n.startsWith("@")) n.substring(1) else n

        if (mem.containsKey(key)) {
            val t = new VisorTextTable()

            t.maxCellWidth = 70

            t #= ("Name", "Value")

            t += (n, mem.get(key))

            t.render()

            nl()
        }
        else {
            warn("Missing variable with name: \'" + n + "\'.")
        }
    }

    /**
     * Trap for missing arguments.
     */
    def mget() {
        warn("Missing argument.")
        warn("Type 'help mget' to see how to use this command.")
    }

    /**
     * ==Command==
     * Gets visor memory variable. Note that this method '''does not'''
     * perform variable substitution on its parameters.
     *
     * ==Examples==
     * <ex>mgetOpt a</ex>
     * Gets the value as an option for visor variable 'a'.
     *
     * @param n Name of the variable.
     * @return Variable host as an option.
     */
    def mgetOpt(n: String): Option[String] = {
        assert(n != null)

        Option(mem.get(n))
    }

    /**
     * If variable with given value and prefix doesn't exist - creates
     * a new variable with given value and returns its host. Otherwise,
     * returns an existing variable host.
     *
     * @param v Value.
     * @param prefix Variable host prefix.
     * @return Existing variable host or the new variable host.
     */
    def setVarIfAbsent(v: AnyRef, prefix: String): String = {
        assert(v != null)
        assert(prefix != null && prefix.length > 0)

        val s = v.toString

        val t = mem.find((t: (String, String)) => t._1.startsWith(prefix) && t._2 == s)

        if (t.isDefined)
            t.get._1
        else {
            for (i <- 0 until Int.MaxValue if mem.putIfAbsent(prefix + i, s) == null)
                return prefix + i

            throw new GridRuntimeException("No more memory.")
        }
    }

    /**
     * Try get variable value with given name.
     *
     * @param v variable name.
     * @return variable value or `v` if variable with name `v` not exist.
     */
    def getVariable(v: String): String = {
        v match {
            case name if name.startsWith("@") => mgetOpt(name.substring(1)).getOrElse(v)
            case _ => v
        }
    }

    /**
     * Creates a new variable with given value and returns its host.
     *
     * @param v Value.
     * @param prefix Variable host prefix.
     * @return New variable host.
     */
    def setVar(v: AnyRef, prefix: String): String = {
        assert(v != null)
        assert(prefix != null && prefix.length > 0)

        val s = v.toString

        for (i <- 0 until Int.MaxValue if mem.putIfAbsent(prefix + i, s) == null)
            return prefix + i

        throw new GridRuntimeException("No more memory.")
    }

    /**
     * Adds command help to the visor. This will be printed as part of `help` command.
     *
     * @param name Command name.
     * @param shortInfo Short command description.
     * @param longInfo Optional multi-line long command description. If not provided - short description
     *      will be used instead.
     * @param aliases List of aliases. Optional.
     * @param spec Command specification.
     * @param args List of `(host, description)` tuples for command arguments. Optional.
     * @param examples List of `(example, description)` tuples for command examples.
     * @param ref - command implementation.
     */
    def addHelp(
        name: String,
        shortInfo: String,
        @Nullable longInfo: Seq[String] = null,
        @Nullable aliases: Seq[String] = Seq.empty,
        spec: Seq[String],
        @Nullable args: Seq[(String, AnyRef)] = null,
        examples: Seq[(String, AnyRef)],
        ref: VisorConsoleCommand) {
        assert(name != null)
        assert(shortInfo != null)
        assert(spec != null && !spec.isEmpty)
        assert(examples != null && !examples.isEmpty)
        assert(ref != null)

        // Add and re-sort
        cmdLst = (cmdLst ++ Seq(VisorConsoleCommandHolder(name, shortInfo, longInfo, aliases, spec, args, examples, ref))).
            sortWith((a, b) => a.name.compareTo(b.name) < 0)
    }

    /**
     * Extract node from command arguments.
     *
     * @param argLst Command arguments.
     * @return error message or node ref.
     */
    def parseNode(argLst: ArgList) = {
        val id8 = argValue("id8", argLst)
        val id = argValue("id", argLst)

        if (id8.isDefined && id.isDefined)
            Left("Only one of '-id8' or '-id' is allowed.")
        else if (id8.isDefined) {
            nodeById8(id8.get) match {
                case Nil => Left("Unknown 'id8' value: " + id8.get)
                case node :: Nil => Right(Option(node))
                case _ => Left("'id8' resolves to more than one node (use full 'id' instead): " + id8.get)
            }
        }
        else if (id.isDefined)
            try {
                val node = Option(grid.node(java.util.UUID.fromString(id.get)))

                if (node.isDefined)
                    Right(node)
                else
                    Left("'id' does not match any node: " + id.get)
            }
            catch {
                case e: IllegalArgumentException => Left("Invalid node 'id': " + id.get)
            }
        else
            Right(None)
    }

    /**
     * Utility method that parses command arguments. Arguments represented as a string
     * into argument list represented as list of tuples (host, value) performing
     * variable substitution:
     *
     * `-p=@n` - A named parameter where `@n` will be considered as a reference to variable named `n`.
     * `@ n` - An unnamed parameter where `@n` will be considered as a reference to variable named `n`.
     * `-p` - A flag doesn't support variable substitution.
     *
     * Note that recursive substitution isn't supported. If specified variable isn't set - the value
     * starting with `@` will be used as-is.
     *
     * @param args Command arguments to parse.
     */
    def parseArgs(@Nullable args: String): ArgList = {
        var lst: ArgList = Nil

        if (args != null)
            for (s <- args.split(" ") if s.trim.length > 0)
                if (s(0) == '-' || s(0) == '/') {
                    val eq = s.indexOf('=')

                    if (eq == -1)
                        lst = lst ++ Seq(s.substring(1) -> null)
                    else {
                        val n = s.substring(1, eq).trim
                        var v = s.substring(eq + 1).trim

                        if (v.startsWith("@"))
                            v = mgetOpt(v.substring(1)).getOrElse(v)

                        lst = lst ++ Seq(n -> v)
                    }
                }
                else {
                    var v = s

                    if (v.startsWith("@"))
                        v = mgetOpt(v.substring(1)).getOrElse(v)

                    lst = lst ++ Seq((null, v))
                }

        lst
    }

    /**
     * Shortcut method that checks if passed in argument list has an argument with given value.
     *
     * @param v Argument value to check for existence in this list.
     * @param args Command argument list.
     */
    def hasArgValue(@Nullable v: String, args: ArgList): Boolean = {
        assert(args != null)

        !args.find(_._2 == v).isEmpty
    }

    /**
     * Shortcut method that checks if passed in argument list has an argument with given host.
     *
     * @param n Argument host to check for existence in this list.
     * @param args Command argument list.
     */
    def hasArgName(@Nullable n: String, args: ArgList): Boolean = {
        assert(args != null)

        !args.find(_._1 == n).isEmpty
    }

    /**
     * Shortcut method that checks if flag (non-`null` host and `null` value) is set
     * in the argument list.
     *
     * @param n Name of the flag.
     * @param args Command argument list.
     */
    def hasArgFlag(n: String, args: ArgList): Boolean = {
        assert(n != null && args != null)

        !args.find((a) => a._1 == n && a._2 == null).isEmpty
    }

    /**
     * Gets the value for a given argument host.
     *
     * @param n Argument host.
     * @param args Argument list.
     * @return Argument value.
     */
    @Nullable def argValue(n: String, args: ArgList): Option[String] = {
        assert(n != null && args != null)

        Option((args find(_._1 == n) getOrElse Til)._2)
    }

    /**
     * Gets a non-`null` value for given parameter.
     *
     * @param a Parameter.
     * @param dflt Value to return if `a` is `null`.
     */
    def safe(@Nullable a: Any, dflt: Any = ""): String = {
        assert(dflt != null)

        if (a != null) a.toString else dflt.toString
    }

    /**
     * Reconstructs string presentation for given argument.
     *
     * @param arg Argument to reconstruct.
     */
    def makeArg(arg: Arg): String = {
        assert(arg != null)
        assert(arg.isSome)

        var s = ""

        if (arg._1 != null) {
            s = "-" + arg._1

            if (arg._2 != null)
                s = s + '=' + arg._2
        }
        else
            s = arg._2

        s
    }

    /**
     * Reconstructs string presentation for given argument list.
     *
     * @param args Argument list to reconstruct.
     */
    def makeArgs(args: ArgList): String = {
        assert(args != null)

        ("" /: args)((b, a) => if (b.length == 0) makeArg(a) else b + ' ' + makeArg(a))
    }

    /**
     * Parses string containing mnemonic predicate and returns Scala predicate.
     *
     * @param s Mnemonic predicate.
     * @return Long to Boolean predicate or null if predicate cannot be created.
     */
    def makeExpression(s: String): Option[Long => Boolean] = {
        assert(s != null)

        def value(v: String): Long =
            // Support for seconds, minutes and hours.
            // NOTE: all memory sizes are assumed to be in MB.
            v.last match {
                case 's' => v.substring(0, v.length - 1).toLong * 1000
                case 'm' => v.substring(0, v.length - 1).toLong * 1000 * 60
                case 'h' => v.substring(0, v.length - 1).toLong * 1000 * 60 * 60
                case _ => v.toLong
            }

        try
            if (s.startsWith("lte")) // <=
                Some(_ <= value(s.substring(3)))
            else if (s.startsWith("lt")) // <
                Some(_ < value(s.substring(2)))
            else if (s.startsWith("gte")) // >=
                Some(_ >= value(s.substring(3)))
            else if (s.startsWith("gt")) // >
                Some(_ > value(s.substring(2)))
            else if (s.startsWith("eq")) // ==
                Some(_ == value(s.substring(2)))
            else if (s.startsWith("neq")) // !=
                Some(_ != value(s.substring(3)))
            else
                None
        catch {
            case e: Throwable => None
        }
    }

    // Formatters.
    private val dblFmt = new DecimalFormat("#0.00")
    private val intFmt = new DecimalFormat("#0")

    /**
     * Formats double value with `#0.00` formatter.
     *
     * @param d Double value to format.
     */
    def formatDouble(d: Double): String = {
        dblFmt.format(d)
    }

    /**
     * Formats double value with `#0` formatter.
     *
     * @param d Double value to format.
     */
    def formatInt(d: Double): String = {
        intFmt.format(d.round)
    }

    /**
     * Returns string representation of the timestamp provided. Result formatted
     * using pattern `MM/dd/yy, HH:mm:ss`.
     *
     * @param ts Timestamp.
     */
    def formatDateTime(ts: Long): String =
        dtFmt.format(ts)

    /**
     * Returns string representation of the date provided. Result formatted using
     * pattern `MM/dd/yy, HH:mm:ss`.
     *
     * @param date Date.
     */
    def formatDateTime(date: Date): String =
        dtFmt.format(date)

    /**
     * Returns string representation of the timestamp provided. Result formatted
     * using pattern `MM/dd/yy`.
     *
     * @param ts Timestamp.
     */
    def formatDate(ts: Long): String =
        dFmt.format(ts)

    /**
     * Returns string representation of the date provided. Result formatted using
     * pattern `MM/dd/yy`.
     *
     * @param date Date.
     */
    def formatDate(date: Date): String =
        dFmt.format(date)

    /**
     * Tests whether or not visor is connected.
     *
     * @return `True` if visor is connected.
     */
    def isConnected =
        isCon

    /**
     * Gets timestamp of visor connection. Returns `0` if visor is not
     * connected.
     *
     * @return Timestamp of visor connection.
     */
    def connectTimestamp =
        conTs

    /**
     * Prints properly formatted error message like:
     * {{{
     * <visor>: err: error message
     * }}}
     *
     * @param errMsgs Error messages to print. If `null` - this function is no-op.
     */
    def warn(errMsgs: Any*) {
        assert(errMsgs != null)

        errMsgs foreach (msg => scala.Console.out.println("(wrn) <visor>: " + msg))
    }

    /**
     * Prints standard 'not connected' error message.
     */
    def adviseToConnect() {
        warn(
            "Visor is disconnected.",
            "Type 'open' to connect visor or 'help open' to get help."
        )
    }

    /**
     * Gets global projection as an option.
     */
    def gridOpt =
        Option(grid)

    def noop() {}

    /**
     * ==Command==
     * Prints visor status.
     *
     * ==Example==
     * <ex>status -q</ex>
     * Prints visor status without ASCII logo.
     *
     * @param args Optional "-q" flag to disable ASCII logo printout.
     */
    def status(args: String) {
        val argLst = parseArgs(args)

        if (!hasArgFlag("q", argLst))
            println(
                " ___    _________________________ ________" + NL +
                " __ |  / /____  _/__  ___/__  __ \\___  __ \\" + NL +
                " __ | / /  __  /  _____ \\ _  / / /__  /_/ /" + NL +
                " __ |/ /  __/ /   ____/ / / /_/ / _  _, _/" + NL +
                " _____/   /___/   /____/  \\____/  /_/ |_|" + NL + NL +
                " ADMIN CONSOLE" + NL +
                " " + COPYRIGHT + NL
            )

        val t = VisorTextTable()

        t += ("Status", if (isCon) "Connected" else "Disconnected")
        t += ("Grid name",
            if (grid == null)
                "<n/a>"
            else {
                val n = grid.name

                if (n == null) "<default>" else n
            }
        )
        t += ("Config path", safe(cfgPath, "<n/a>"))
        t += ("Uptime", if (isCon) X.timeSpan2HMS(uptime) else "<n/a>")

        t.render()
    }

    /**
     * ==Alias==
     * Prints visor status. This is an alias for `status` command.
     *
     * ==Example==
     * <ex>! -q</ex>
     * Prints visor status without ASCII logo.
     *
     * @param args Optional "-q" flag to disable ASCII logo printout.
     */
    def !(args: String) {
        status(args)
    }

    /**
     * ==Command==
     * Prints visor status (with ASCII logo).
     *
     * ==Example==
     * <ex>status</ex>
     * Prints visor status.
     */
    def status() {
        status("")
    }

    /**
     * ==Alias==
     * Prints visor status. This is an alias for `status` command.
     *
     * ==Example==
     * <ex>status</ex>
     * Prints visor status.
     */
    def `!`() {
        status(null)
    }

    /**
     * ==Command==
     * Prints help for specific command(s) or for all commands.
     *
     * ==Example==
     * <ex>help</ex>
     * Prints general help.
     *
     * <ex>help open</ex>
     * Prints help for 'open' command.
     *
     * @param args List of commands to print help for. If empty - prints generic help.
     */
    def help(args: String = null) {
        val argLst = parseArgs(args)

        if (!has(argLst)) {
            val t = VisorTextTable()

            t #= ("Command", "Description")

            cmdLst foreach (hlp => t += (hlp.nameWithAliases, hlp.shortInfo))

            t.render()

            println("\nType 'help \"command name\"' to see how to use this command.")
        }
        else
            for (c <- argLst)
                if (c._1 != null)
                    warn("Invalid command name: " + argName(c))
                else if (c._2 == null)
                    warn("Invalid command name: " + argName(c))
                else {
                    val n = c._2

                    val opt = cmdLst.find(_.name == n)

                    if (opt.isEmpty)
                        warn("Invalid command name: " + n)
                    else {
                        val hlp: VisorConsoleCommandHolder = opt.get

                        val t = VisorTextTable()

                        t += (hlp.nameWithAliases, if (hlp.longInfo == null) hlp.shortInfo else hlp.longInfo)

                        t.render()

                        println("\nSPECIFICATION:")

                        hlp.spec foreach(s => println(blank(4) + s))

                        if (has(hlp.args)) {
                            println("\nARGUMENTS:")

                            hlp.args foreach (a => {
                                val (arg, desc) = a

                                println(blank(4) + arg)

                                desc match {
                                    case (lines: Iterable[_]) => lines foreach (line => println(blank(8) + line))
                                    case s: AnyRef => println(blank(8) + s.toString)
                                }
                            })
                        }

                        if (has(hlp.examples)) {
                            println("\nEXAMPLES:")

                            hlp.examples foreach (a => {
                                val (ex, desc) = a

                                println(blank(4) + ex)

                                desc match {
                                    case (lines: Iterable[_]) => lines foreach (line => println(blank(8) + line))
                                    case s: AnyRef => println(blank(8) + s.toString)
                                }
                            })
                        }

                        nl()
                    }
                }
    }

    /**
     * Tests whether passed in sequence is not `null` and not empty.
     */
    private def has[T](@Nullable s: Seq[T]): Boolean = {
        s != null && !s.isEmpty
    }

    /**
     * ==Command==
     * Prints generic help.
     *
     * ==Example==
     * <ex>help</ex>
     * Prints help.
     */
    def help() {
        help("")
    }

    /**
     * ==Alias==
     * Prints help. This is an alias for `help` command.
     *
     * ==Example==
     * <ex>help open</ex>
     * Prints help for 'open' command.
     *
     * @param args List of commands to print help for. If `null` or empty - prints generic help.
     */
    def ?(args: String = "") {
        help(args)
    }

    /**
     * ==Alias==
     * Prints help. This is an alias for `help` command.
     *
     * ==Example==
     * <ex>help</ex>
     * Prints help.
     */
    def `?`() {
        help()
    }

    /**
     * Helper function that makes up the full argument host from tuple.
     *
     * @param t Command argument tuple.
     */
    def argName(t: (String, String)): String =
        if (F.isEmpty(t._1) && F.isEmpty(t._2))
            "<empty>"
        else if (F.isEmpty(t._1))
            t._2
        else
            t._1

    /**
     * Helper method that produces blank string of given length.
     *
     * @param len Length of the blank string.
     */
    private def blank(len: Int) = new String().padTo(len, ' ')

    /**
     * ==Command==
     * Connects visor to default or named grid.
     *
     * ==Examples==
     * <ex>open -g=mygrid</ex>
     * Connects to 'mygrid' grid.
     *
     * @param args Command arguments.
     * @param repl Whether or not Visor is running inside of the Scala REPL.
     */
    def open(args: String, repl: Boolean = true) {
        def scold(errMsgs: String*) {
            assert(errMsgs != null)

            warn(errMsgs: _*)
            warn("Type 'help open' to see how to use this command.")
        }

        try {
            open0(args, repl)
        }
        catch {
            case e: GridException => scold(e.getMessage)
        }
    }

    /**
     * Internal implementation of 'open' command that throws 'GridException' in
     * case of any error.
     *
     * @param args Command arguments.
     * @param repl Whether or not Visor is running inside of the Scala REPL.
     */
    def open0(args: String, repl: Boolean) {
        assert(args != null)

        def configuration(path: String): GridConfiguration = {
            assert(path != null)

            val url =
                try
                    new URL(path)
                catch {
                    case e: Exception =>
                        val url = U.resolveGridGainUrl(path)

                        if (url == null)
                            throw new GE("Spring XML configuration path is invalid: " + path, e)

                        url
                }

            val isLog4jUsed = classOf[G].getClassLoader.getResource("org/apache/log4j/Appender.class") != null

            var log4jTup: GridBiTuple[AnyRef, AnyRef] = null

            val spring: GridSpringProcessor = SPRING.create(false)

            val cfgs =
                try {
                    spring.loadConfigurations(url).get1()
                }
                finally {
                    if (isLog4jUsed && log4jTup != null)
                        U.removeLog4jNoOpLogger(log4jTup)
                }

            if (cfgs == null || cfgs.isEmpty)
                throw new GE("Can't find grid configuration in: " + url)

            if (cfgs.size > 1)
                throw new GE("More than one grid configuration found in: " + url)

            val cfg = cfgs.iterator().next()

            // Setting up 'Config URL' for properly print in console.
            System.setProperty(GridSystemProperties.GG_CONFIG_URL, url.getPath)

            var cpuCnt = Runtime.getRuntime.availableProcessors

            if (cpuCnt < 4)
                cpuCnt = 4

            cfg.setRestEnabled(false)

            // All thread pools are overridden to have size equal to number of CPUs.
            cfg.setExecutorService(new GridThreadPoolExecutor(cpuCnt, cpuCnt,
                Long.MaxValue, new LinkedBlockingQueue[Runnable]))
            cfg.setSystemExecutorService(new GridThreadPoolExecutor(cpuCnt, cpuCnt,
                Long.MaxValue, new LinkedBlockingQueue[Runnable]))
            cfg.setPeerClassLoadingExecutorService(new GridThreadPoolExecutor(cpuCnt, cpuCnt,
                Long.MaxValue, new LinkedBlockingQueue[Runnable]))

            var ioSpi = cfg.getCommunicationSpi

            if (ioSpi == null)
                ioSpi = new GridTcpCommunicationSpi()

            cfg
        }

        if (isConnected)
            throw new GE("Visor is already connected. Disconnect first.")
        else {
            val argLst = parseArgs(args)

            val name = argValue("g", argLst).getOrElse(null)
            val path = argValue("cpath", argLst)
            val existing = hasArgFlag("e", argLst)
            val dflt = hasArgFlag("d", argLst)

            rmtLogDisabled = hasArgFlag("dl", argLst)

            if (existing && dflt)
                throw new GE("Can't have both '-e' and '-d' together.")

            if (existing)
                grid$(name) match {
                    case Some(g) =>
                        // Successfully "connected" to already joined grid.
                        grid = g.asInstanceOf[GridEx]
                        isCon = true
                        conOwner = false
                        cfgPath = "<n/a>"
                        conTs = System.currentTimeMillis

                    case None => throw new GE("Failed to connect to existing grid.")
                }
            else {
                var cfg: GridConfiguration = null
                var startedGridName: String = null

                val cfgPath =
                    if (path.isDefined) {
                        cfg = configuration(path.get)

                        path.get
                    }
                    else if (dflt) {
                        cfg = configuration(DFLT_CFG)

                        "<default>"
                    }
                    else {
                        // If configuration file is not defined in arguments,
                        // ask to choose from the list
                        askConfigFile() match {
                            case Some(p) =>
                                nl()

                                (VisorTextTable() += ("Using configuration", p)) render()

                                nl()

                                cfg = configuration(p)

                                p
                            case None =>
                                return
                        }
                    }

                val daemon = scalar.isDaemon

                // Make sure visor starts as daemon node.
                scalar.daemon(true)

                try {
                    startedGridName = scalar.start(cfg).name
                }
                finally {
                    scalar.daemon(daemon)
                }

                this.cfgPath = cfgPath

                val nameToCheck = if (name == null) startedGridName else name

                grid$(nameToCheck) match {
                    case Some(g) => grid = g.asInstanceOf[GridEx]
                    case None =>
                        this.cfgPath = null

                        throw new GE("Named grid unavailable: " + nameToCheck)
                }

                assert(cfgPath != null)

                isCon = true
                conOwner = true
                conTs = System.currentTimeMillis
            }

            if (!grid.configuration().isPeerClassLoadingEnabled)
                warn("Peer class loading is disabled (custom closures in shell mode will not work).")

            pool = new GridThreadPoolExecutor()

            grid.nodes().foreach(n => {
                setVarIfAbsent(nid8(n), "n")

                val ip = n.addresses().headOption

                if (ip.isDefined)
                    setVarIfAbsent(ip.get, "h")
            })

            nodeJoinLsnr = new GridPredicate[GridEvent]() {
                override def apply(e: GridEvent): Boolean = {
                    e match {
                        case de: GridDiscoveryEvent =>
                            setVarIfAbsent(nid8(de.eventNode()), "n")

                            val node = grid.node(de.eventNode().id())

                            if (node != null) {
                                val ip = node.addresses().headOption

                                if (ip.isDefined)
                                    setVarIfAbsent(ip.get, "h")
                            }
                            else {
                                if (repl)
                                    warn(
                                        "New node not found: " + de.eventNode().id(),
                                        "Visor must have discovery configuration and local " +
                                            "host bindings identical with grid nodes."
                                    )
                            }
                    }

                    true
                }
            }

            grid.events().localListen(nodeJoinLsnr, EVT_NODE_JOINED)

            nodeLeftLsnr = new GridPredicate[GridEvent]() {
                override def apply(e: GridEvent): Boolean = {
                    e match {
                        case (de: GridDiscoveryEvent) =>
                            val nv = mfind(nid8(de.eventNode()))

                            if (nv.isDefined)
                                mem.remove(nv.get._1)

                            val ip = de.eventNode().addresses.headOption

                            if (ip.isDefined) {
                                val last = !grid.nodes().exists(n =>
                                    n.addresses.size > 0 && n.addresses.head == ip.get
                                )

                                if (last) {
                                    val hv = mfind(ip.get)

                                    if (hv.isDefined)
                                        mem.remove(hv.get._1)
                                }
                            }
                    }

                    true
                }
            }

            grid.events().localListen(nodeLeftLsnr, EVT_NODE_LEFT, EVT_NODE_FAILED)

            nodeSegLsnr = new GridPredicate[GridEvent] {
                override def apply(e: GridEvent): Boolean = {
                    e match {
                        case de: GridDiscoveryEvent =>
                            if (de.eventNode().id() == grid.localNode.id) {
                                if (repl) {
                                    warn("Closing visor due to topology segmentation.")
                                    warn("Contact your system administrator.")

                                    nl()
                                }

                                close()
                            }
                    }

                    true
                }
            }

            grid.events().localListen(nodeSegLsnr, EVT_NODE_SEGMENTED)

            nodeStopLsnr = new GridGainListener {
                def onStateChange(name: String, state: GridGainState) {
                    if (name == grid.name && state == GridGainState.STOPPED) {
                        if (repl) {
                            warn("Closing visor due to stopping of host grid instance.")

                            nl()
                        }

                        close()
                    }
                }
            }

            G.addListener(nodeStopLsnr)

            if (repl) {
                logText("Visor joined topology: " + cfgPath)
                logText("All live nodes, if any, will re-join.")

                nl()

                val t = VisorTextTable()

                // Print advise.
                println("Some useful commands:")

                t += ("Type 'top'", "to see full topology.")
                t += ("Type 'node'", "to see node statistics.")
                t += ("Type 'cache'", "to see cache statistics.")
                t += ("Type 'tasks'", "to see tasks statistics.")
                t += ("Type 'config'", "to see node configuration.")

                t.render()

                println("\nType 'help' to get help.\n")
            }

            status()
        }
    }

    /**
     * ==Command==
     * Connects visor to the default grid.
     *
     * ==Example==
     * <ex>open</ex>
     * Connects to the default grid.
     */
    def open() {
        open("")
    }

    /**
     * Returns string with node id8, its memory variable, if available, and its
     * IP address (first internal address), if node is alive.
     *
     * @param id Node ID.
     * @return String.
     */
    def nodeId8Addr(id: UUID): String = {
        assert(id != null)
        assert(isCon)

        val g = grid

        if (g != null && g.localNode.id == id)
            "<visor>"
        else {
            val n = grid.node(id)

            val id8 = nid8(id)
            val v = mfind(id8)

            id8 +
                (if (v.isDefined) "(@" + v.get._1 + ")" else "") +
                ", " +
                (if (n == null) "<n/a>" else n.addresses().headOption.getOrElse("<n/a>"))
        }
    }

    /**
     * Returns string with node id8 and its memory variable, if available.
     *
     * @param id Node ID.
     * @return String.
     */
    def nodeId8(id: UUID): String = {
        assert(id != null)
        assert(isCon)

        val id8 = nid8(id)
        val v = mfind(id8)

        id8 + (if (v.isDefined) "(@" + v.get._1 + ")" else "")
    }

    /**
     * Guards against invalid percent readings.
     *
     * @param v Value in '%' to guard. Any value below `0` and greater than `100`
     *      will return `n/a` string.
     */
    def safePercent(v: Double): String = {
        if (v < 0 || v > 100)
            "n/a"
        else
            formatDouble(v) + " %"
    }

    /**
     * Asks user to select a node from the list.
     *
     * @param title Title displayed before the list of nodes.
     * @return `Option` for ID of selected node.
     */
    def askForNode(title: String): Option[UUID] = {
        assert(title != null)
        assert(isCon)

        val t = VisorTextTable()

        t #= (">", "Node ID8(@), IP", "Up Time", "CPUs", "CPU Load", "Free Heap")

        val nodes = grid.nodes().toList

        if (nodes.isEmpty) {
            warn("Topology is empty.")

            None
        }
        else {
            (0 until nodes.size) foreach (i => {
                val n = nodes(i)

                val m = n.metrics

                val usdMem = m.getHeapMemoryUsed
                val maxMem = m.getHeapMemoryMaximum
                val freeHeapPct = (maxMem - usdMem) * 100 / maxMem

                val cpuLoadPct = m.getCurrentCpuLoad * 100

                t += (
                    i,
                    nodeId8Addr(n.id),
                    X.timeSpan2HMS(m.getUpTime),
                    n.metrics.getTotalCpus,
                    safePercent(cpuLoadPct),
                    formatDouble(freeHeapPct) + " %"
                )
            })

            println(title)

            t.render()

            val a = ask("\nChoose node ('c' to cancel) [c]: ", "c")

            if (a.toLowerCase == "c")
                None
            else {
                try
                    Some(nodes(a.toInt).id)
                catch {
                    case e: Throwable =>
                        warn("Invalid selection: " + a)

                        None
                }
            }
        }
    }

    /**
     * Asks user to select a host from the list.
     *
     * @param title Title displayed before the list of hosts.
     * @return `Option` for projection of nodes located on selected host.
     */
    def askForHost(title: String): Option[GridProjection] = {
        assert(title != null)
        assert(isCon)

        val t = VisorTextTable()

        t #= (">", "Int./Ext. IPs", "Node ID8(@)", "OS", "CPUs", "MACs", "CPU Load")

        val neighborhood = U.neighborhood(grid.nodes()).values().toIndexedSeq

        if (neighborhood.isEmpty) {
            warn("Topology is empty.")

            None
        }
        else {
            (0 until neighborhood.size) foreach (i => {
                val neighbors = neighborhood(i)

                var ips = immutable.Set.empty[String]
                var id8s = Seq.empty[String]
                var macs = immutable.Set.empty[String]
                var cpuLoadSum = 0.0

                val n1 = neighbors.head

                assert(n1 != null)

                val cpus = n1.metrics.getTotalCpus

                val os = "" +
                    n1.attribute("os.name") + " " +
                    n1.attribute("os.arch") + " " +
                    n1.attribute("os.version")

                neighbors.foreach(n => {
                    id8s = id8s :+ nodeId8(n.id)

                    ips = ips ++ n.addresses()

                    cpuLoadSum += n.metrics().getCurrentCpuLoad

                    macs = macs ++ n.attribute[String](ATTR_MACS).split(", ").map(_.grouped(2).mkString(":"))
                })

                t += (
                    i,
                    ips.toSeq,
                    id8s,
                    os,
                    cpus,
                    macs.toSeq,
                    safePercent(cpuLoadSum / neighbors.size() * 100)
                )
            })

            println(title)

            t.render()

            val a = ask("\nChoose host ('c' to cancel) [c]: ", "c")

            if (a.toLowerCase == "c")
                None
            else {
                try
                    Some(grid.forNodes(neighborhood(a.toInt)))
                catch {
                    case e: Throwable =>
                        warn("Invalid selection: " + a)

                        None
                }
            }
        }
    }

    /**
     * Asks user to choose configuration file.
     *
     * @return `Option` for file path.
     */
    def askConfigFile(): Option[String] = {
        val files = GridConfigurationFinder.getConfigFiles

        if (files.isEmpty) {
            warn("No configuration files found.")

            None
        }
        else {
            val t = VisorTextTable()

            t #= (">", "Configuration File")

            (0 until files.size).foreach(i => t += (i, files(i)._1))

            println("Local configuration files:")

            t.render()

            val a = ask("\nChoose configuration file ('c' to cancel) [0]: ", "0")

            if (a.toLowerCase == "c")
                None
            else {
                try
                    Some(files(a.toInt)._1)
                catch {
                    case e: Throwable =>
                        nl()

                        warn("Invalid selection: " + a)

                        None
                }
            }
        }
    }

    /**
     * Asks user input.
     *
     * @param prompt Prompt string.
     * @param dflt Default value for user input.
     * @param passwd If `true`, input will be masked with '*' character. `false` by default.
     */
    def ask(prompt: String, dflt: String, passwd: Boolean = false): String = {
        assert(prompt != null)
        assert(dflt != null)

        readLineOpt(prompt, if (passwd) Some('*') else None) match {
            case None => dflt
            case Some(s) if s.length == 0 => dflt
            case Some(s) => s
        }
    }

    /**
     * Safe `readLine` version.
     *
     * @param prompt User prompt.
     * @param mask Mask character (if `None`, no masking will be applied).
     */
    private def readLineOpt(prompt: String, mask: Option[Char]): Option[String] =
        try {
            val s = if (System.getProperty("VISOR_REPL") == null)
                readLine(prompt)
            else
                // Current jline (Scala 2.8) has a known bug that makes
                // default `readLine()` non-operational.
                // More details: http://lampsvn.epfl.ch/trac/scala/ticket/3442
                if (mask.isDefined)
                    new scala.tools.jline.console.ConsoleReader().readLine(prompt, mask.get)
                else
                    new scala.tools.jline.console.ConsoleReader().readLine(prompt)

            Option(s)
        }
        catch {
            case _: Throwable => None
        }

    /**
     * Asks user to choose node id8.
     *
     * @return `Option` for node id8.
     */
    def askNodeId(): Option[String] = {
        assert(isConnected)

        val ids = grid.forRemotes().nodes().map(nid8).toList

        (0 until ids.size).foreach(i => println((i + 1) + ": " + ids(i)))

        println("\nC: Cancel")

        readLine("\nChoose node: ") match {
            case "c" | "C" => None
            case idx =>
                try
                    Some(ids(idx.toInt - 1))
                catch {
                    case e: Throwable =>
                        if (idx.isEmpty)
                            warn("Index can't be empty.")
                        else
                            warn("Invalid index: " + idx + ".")

                        None
                }
        }
    }

    /**
     * Adds close callback. Added function will be called every time
     * command `close` is called.
     *
     * @param f Close callback to add.
     */
    def addShutdownCallback(f: () => Unit) {
        assert(f != null)

        shutdownCbs = shutdownCbs :+ f
    }


    /**
     * Adds close callback. Added function will be called every time
     * command `close` is called.
     *
     * @param f Close callback to add.
     */
    def addCloseCallback(f: () => Unit) {
        assert(f != null)

        cbs = cbs :+ f
    }

    /**
     * Removes close callback.
     *
     * @param f Close callback to remove.
     */
    def removeCloseCallback(f: () => Unit) {
        assert(f != null)

        cbs = cbs.filter(_ != f)
    }

    /**
     * Removes all close callbacks.
     */
    def removeCloseCallbacks() {
        cbs = Seq.empty[() => Unit]
    }

    /**
     * Gets visor uptime.
     */
    def uptime = if (isCon) System.currentTimeMillis() - conTs else -1L

    /**
     * ==Command==
     * Disconnects visor.
     *
     * ==Examples==
     * <ex>close</ex>
     * Disconnects from the grid.
     */
    def close() {
        if (!isConnected)
            adviseToConnect()
        else {
            if (pool != null) {
                pool.shutdown()

                try
                    if (!pool.awaitTermination(5, TimeUnit.SECONDS))
                        pool.shutdownNow
                catch {
                    case e: InterruptedException =>
                        pool.shutdownNow

                        Thread.currentThread.interrupt()
                }

                pool = null
            }

            // Call all close callbacks.
            cbs foreach(_.apply())

            if (grid != null && G.state(grid.name) == GridGainState.STARTED) {
                if (nodeJoinLsnr != null)
                    grid.events().stopLocalListen(nodeJoinLsnr)

                if (nodeLeftLsnr != null)
                    grid.events().stopLocalListen(nodeLeftLsnr)

                if (nodeSegLsnr != null)
                    grid.events().stopLocalListen(nodeSegLsnr)
            }

            if (nodeStopLsnr != null)
                G.removeListener(nodeStopLsnr)

            if (grid != null && conOwner)
                try {
                    scalar.stop(grid.name, true)
                }
                catch {
                    case e: Exception => warn(e.getMessage)
                }

            // Fall through and treat visor as closed
            // even in case when grid didn't stop properly.

            logText("Visor left topology.")

            if (logStarted) {
                stopLog()

                nl()
            }

            rmtLogDisabled = false
            isCon = false
            conOwner = false
            conTs = 0
            grid = null
            nodeJoinLsnr = null
            nodeLeftLsnr = null
            nodeSegLsnr = null
            nodeStopLsnr = null
            cfgPath = null

            // Clear the memory.
            mclear()

            nl()

            status()
        }
    }

    /**
     * ==Command==
     * quit from visor.
     *
     * ==Examples==
     * <ex>quit</ex>
     * Quit from visor.
     */
    def quit() {
        System.exit(0)
    }

    /**
     * ==Command==
     * Prints log status.
     *
     * ==Examples==
     * <ex>log</ex>
     * Prints log status.
     */
    def log() {
        val t = VisorTextTable()

        t += ("Status", if (logStarted) "Started" else "Stopped")

        if (logStarted) {
            t += ("File path", logFile.getAbsolutePath)
            t += ("File size", if (logFile.exists) kbFmt.format(logFile.length()) + "kb" else "0kb")
        }

        t.render()
    }

    /**
     * ==Command==
     * Starts or stops logging.
     *
     * ==Examples==
     * <ex>log -l -f=/home/user/visor-log</ex>
     * Starts logging to file located at '/home/user/visor-log'.
     *
     * <ex>log -l -f=log/visor-log</ex>
     * Starts logging to file located at 'GRIDGAIN_HOME/log/visor-log'.
     *
     * <ex>log -l -p=20</ex>
     * Starts logging with querying events period of 20 seconds.
     *
     * <ex>log -l -t=30</ex>
     * Starts logging with topology snapshot logging period of 30 seconds.
     *
     * <ex>log -s</ex>
     * Stops logging.
     *
     * @param args Command arguments.
     */
    def log(args: String) {
        assert(args != null)

        def scold(errMsgs: Any*) {
            assert(errMsgs != null)

            warn(errMsgs: _*)
            warn("Type 'help log' to see how to use this command.")
        }

        val argLst = parseArgs(args)

        if (hasArgFlag("s", argLst))
            if (!logStarted)
                scold("Logging was not started.")
            else
                stopLog()
        else if (hasArgFlag("l", argLst))
            if (logStarted)
                scold("Logging is already started.")
            else
                try
                    startLog(argValue("f", argLst), argValue("p", argLst), argValue("t", argLst))
                catch {
                    case e: IllegalArgumentException => scold(e.getMessage)
                }
        else
            scold("Invalid arguments.")
    }

    /**
     * Stops logging.
     */
    private def stopLog() {
        assert(logStarted)

        logText("Log stopped.")

        if (logTimer != null) {
            logTimer.cancel()
            logTimer.purge()

            logTimer = null
        }

        if (topTimer != null) {
            topTimer.cancel()
            topTimer.purge()

            topTimer = null
        }

        logStarted = false

        println("<visor>: Log stopped: " + logFile.getAbsolutePath)
    }

    /**
     * Starts logging. If logging is already started - no-op.
     *
     * @param pathOpt `Option` for log file path. If `None` - default is used.
     * @param freqOpt `Option` for events fetching frequency If `None` - default is used.
     */
    private def startLog(pathOpt: Option[String], freqOpt: Option[String], topFreqOpt: Option[String]) {
        assert(pathOpt != null)
        assert(freqOpt != null)
        assert(!logStarted)

        val path = pathOpt.getOrElse(DFLT_LOG_PATH)

        logFile = U.resolveWorkDirectory(path, false)

        var freq = 0L

        try
            freq = freqOpt.getOrElse("10").toLong * 1000L
        catch {
            case e: NumberFormatException =>
                throw new IllegalArgumentException("Invalid frequency: " + freqOpt.get)
        }

        if (freq <= 0)
            throw new IllegalArgumentException("Frequency must be positive: " + freq)

        if (freq > 60000)
            warn("Frequency greater than a minute is too low (ignoring).")

        var topFreq = 0L

        try
            topFreq = topFreqOpt.getOrElse("20").toLong * 1000L
        catch {
            case e: NumberFormatException =>
                throw new IllegalArgumentException("Invalid topology frequency: " + topFreqOpt.get)
        }

        if (topFreq <= 0)
            throw new IllegalArgumentException("Topology frequency must be positive: " + topFreq)

        // Unique key for this JVM.
        val key = UUID.randomUUID().toString + System.identityHashCode(classOf[java.lang.Object]).toString

        logTimer = new Timer(true)

        logTimer.schedule(new TimerTask() {
            /** Events to be logged by visor (additionally to discovery events). */
            private final val LOG_EVTS = Seq(
                EVT_JOB_TIMEDOUT,
                EVT_JOB_FAILED,
                EVT_JOB_FAILED_OVER,
                EVT_JOB_REJECTED,
                EVT_JOB_CANCELLED,
                EVT_TASK_TIMEDOUT,
                EVT_TASK_FAILED,
                EVT_CLASS_DEPLOY_FAILED,
                EVT_TASK_DEPLOY_FAILED,
                EVT_TASK_DEPLOYED,
                EVT_TASK_UNDEPLOYED,
                EVT_LIC_CLEARED,
                EVT_LIC_VIOLATION,
                EVT_LIC_GRACE_EXPIRED,
                EVT_CACHE_PRELOAD_STARTED,
                EVT_CACHE_PRELOAD_STOPPED
            )

            override def run() {
                val g = grid

                if (g != null) {
                    // Discovery events collected only locally.
                    var evts = Collector.collect(LOG_EVTS ++ EVTS_DISCOVERY, g, key)

                    if (!rmtLogDisabled)
                        try {
                            evts = evts ++ g.forRemotes()
                                .compute()
                                .withName("visor-log-collector")
                                .withNoFailover()
                                .broadcast(new CollectorClosure(LOG_EVTS, key))
                                .get
                                .flatten
                        }
                        catch {
                            case _: GridEmptyProjectionException => // Ignore.
                            case _: Exception => logText("Failed to collect remote log.")
                        }

                    if (!evts.isEmpty) {
                        var out: FileWriter = null

                        try {
                            out = new FileWriter(logFile, true)

                            evts.toList.sortBy(_.timestamp).foreach((e: GridEvent) => {
                                logImpl(
                                    out,
                                    formatDateTime(e.timestamp),
                                    nodeId8Addr(e.node().id()),
                                    U.compact(e.shortDisplay)
                                )

                                e match {
                                    case _: GridDiscoveryEvent => snapshot()
                                    case _ => ()
                                }
                            })
                        }
                        finally {
                            U.close(out, null)
                        }
                    }
                }
            }
        }, freq, freq)

        topTimer = new Timer(true)

        topTimer.schedule(new TimerTask() {
            override def run() {
                snapshot()
            }
        }, topFreq, topFreq)

        logStarted = true

        logText("Log started.")

        println("<visor>: Log started: " + logFile.getAbsolutePath)
    }

    /**
     * Does topology snapshot.
     */
    private def snapshot() {
        val g = grid

        if (g != null)
            try
                drawBar(g.metrics())
            catch {
                case e: GridEmptyProjectionException => logText("Topology is empty.")
                case e: Exception => ()
            }
    }

    /**
     *
     * @param m Projection metrics.
     */
    private def drawBar(m: GridProjectionMetrics) {
        assert(m != null)

        val pipe = "|"

        def bar(cpuLoad: Double, memUsed: Double): String = {
            val nCpu = if (cpuLoad < 0 || cpuLoad > 1) 0 else (cpuLoad * 20).toInt
            val nMem = if (memUsed < 0 || memUsed > 1) 0 else (memUsed * 20).toInt

            ("" /: (0 until 20))((s: String, i: Int) => {
                s + (i match {
                    case a if a == nMem => "^"
                    case a if a <= nCpu => "="
                    case _ => '.'
                })
            })
        }

        logText("H/N/C" + pipe +
            m.getTotalHosts.toString.padTo(4, ' ') + pipe +
            m.getTotalNodes.toString.padTo(4, ' ') + pipe +
            m.getTotalCpus.toString.padTo(4, ' ') + pipe +
            bar(m.getAverageCpuLoad, m.getAverageHeapMemoryUsed / m.getAverageHeapMemoryMaximum) + pipe
        )
    }

    /**
     * Logs text message.
     *
     * @param msg Message to log.
     */
    def logText(msg: String) {
        assert(msg != null)

        if (logStarted) {
            var out: FileWriter = null

            try {
                out = new FileWriter(logFile, true)

                logImpl(
                    out,
                    formatDateTime(System.currentTimeMillis),
                    null,
                    msg
                )
            }
            catch {
                case e: IOException => ()
            }
            finally {
                U.close(out, null)
            }
        }
    }

    /**
     * @param out Writer.
     * @param tstamp Timestamp of the log.
     * @param node Node associated with the event.
     * @param msg Message associated with the event.
     */
    private def logImpl(
        out: java.io.Writer,
        tstamp: String,
        node: String = null,
        msg: String
    ) {
        assert(out != null)
        assert(tstamp != null)
        assert(msg != null)
        assert(logStarted)

        if (node != null)
            out.write(tstamp.padTo(18, ' ') + " | " + node + " => " + msg + "\n")
        else
            out.write(tstamp.padTo(18, ' ') + " | " + msg + "\n")
    }

    /**
     * Prints out status and help in case someone calls `visor()` from REPL.
     *
     */
    def apply() {
        status()

        nl()

        `?`()
    }

    lazy val commands = cmdLst.map(_.name) ++ cmdLst.map(_.aliases).flatten

    def searchCmd(cmd: String) = cmdLst.find(c => c.name.equals(cmd) || (c.aliases != null && c.aliases.contains(cmd)))

    /**
     * Transform node ID to ID8 string.
     *
     * @param node Node to take ID from.
     * @return Node ID in ID8 format.
     */
    def nid8(node: GridNode): String = {
        nid8(node.id())
    }

    /**
     * Transform node ID to ID8 string.
     *
     * @param nid Node ID.
     * @return Node ID in ID8 format.
     */
    def nid8(nid: UUID): String = {
        nid.toString.take(8).toUpperCase
    }

    /**
     * Get node by ID8 string.
     *
     * @param id8 Node ID in ID8 format.
     * @return Collection of nodes that has specified ID8.
     */
    def nodeById8(id8: String) = {
        grid.nodes().filter(n => id8.equalsIgnoreCase(nid8(n)))
    }
}

/**
 * Event collect utils
 */
object Collector {
    /**
     * Collects local event from given grid instance.
     *
     * @param types Types of events to collect.
     * @param g Grid instance.
     * @param key Node local storage key.
     */
    def collect(types: Seq[Int], g: Grid, key: String): Seq[GridEvent] = {
        assert(types != null)
        assert(g != null)
        assert(key != null)

        val nl = g.nodeLocalMap[String, Long]()

        val last: Long = nl.getOrElse(key, -1L)

        val tenMinAgo = System.currentTimeMillis() - 10 * 60 * 1000

        val evts = g.events().localQuery((evt: GridEvent) =>
            types.contains(evt.`type`) && evt.localOrder > last && evt.timestamp() > tenMinAgo)

        // Update latest order in node local, if not empty.
        if (!evts.isEmpty)
            nl.put(key, evts.maxBy(_.localOrder()).localOrder)

        evts.toList.sortBy(_.timestamp)
    }
}

/**
 * Remote events collector closure.
 */
@GridInternal
class CollectorClosure(types: Seq[Int], key: String) extends GridCallable[Seq[GridEvent]] {
    @GridInstanceResource
    private val g: Grid = null

    @impl def call(): Seq[GridEvent] = {
        Collector.collect(types, g, key)
    }
}
