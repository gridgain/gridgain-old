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

package org.gridgain.visor.commands.license

import java.io._
import java.util.UUID

import org.gridgain.grid._
import org.gridgain.grid.kernal.visor.cmd.tasks.{VisorLicenseCollectTask, VisorLicenseUpdateTask}
import org.gridgain.grid.lang.GridBiTuple
import org.gridgain.scalar.scalar._
import org.gridgain.visor._
import org.gridgain.visor.commands.{VisorConsoleCommand, VisorTextTable}
import org.gridgain.visor.visor._

import scala.collection.JavaConversions._
import scala.io.Source
import scala.language.implicitConversions

/**
 * ==Overview==
 * Contains Visor command `license` implementation.
 *
 * ==Help==
 * {{{
 * +---------------------------------------------------------------------------+
 * | license | Shows information about all licenses that are used on the grid. |
 * |         | Also can be used to update one of the licenses.                  |
 * +---------------------------------------------------------------------------+
 * }}}
 *
 * ====Specification====
 * {{{
 *     license
 *     license "-f=<path> -id=<license-id>"
 * }}}
 *
 * ====Arguments====
 * {{{
 *     -f=<path>
 *         Path to new license XML file.
 *     -id=<license-id>
 *         ID of the license will be updated.
 * }}}
 *
 * ====Examples====
 * {{{
 *     license
 *         Shows all licenses that are used on the grid.
 *     license "-f=/path/to/new/license.xml -id=fbdea781-90e6-4d1b-b8b3-5b8c14aa2df7"
 *         Copies new license file to all nodes that use license with provided ID.
 * }}}
 */
class VisorLicenseCommand {
    /**
     * Prints error message and advise.
     *
     * @param errMsgs Error messages.
     */
    private def scold(errMsgs: Any*) {
        assert(errMsgs != null)

        nl()

        warn(errMsgs: _*)
        warn("Type 'help license' to see how to use this command.")
    }

    /**
     * ===Command===
     * Shows all licenses that are used on the grid.
     *
     * ===Examples===
     * <ex>license</ex>
     * Shows all licenses that are used on the grid.
     */
    def license() {
        if (!isConnected)
            adviseToConnect()
        else {
            if (grid.isEmpty)
                scold("Topology is empty.")
            else {
                 val sumT = new VisorTextTable()

                sumT #= ("Node ID8(@)", "License ID")

                val nodes = grid.nodes()

                val lics = try
                    grid.forNodes(nodes).compute().execute(classOf[VisorLicenseCollectTask],
                        emptyTaskArgument(nodes.map(_.id()))).get
                catch {
                    case _: GridException =>
                        warn("Failed to obtain license from grid.")

                        return
                }

                lics.foreach(t => sumT += (nodeId8Addr(t.get1()), Option(t.get2()).fold("Open source")(_.id().toString)))

                if (lics.nonEmpty) {
                    sumT.render()

                    for (p <- lics if p.get2() != null) {
                        val l = p.get2()

                        nl()

                        println("License '" + l.id + "':")

                        val licT = new VisorTextTable()

                        licT += ("Version", safe(l.version(), "<n/a>"))
                        licT += ("Version regular expression", safe(l.versionRegexp(), "<n/a>"))
                        licT += ("Issue date", Option(l.issueDate()).fold("<n/a>")(d => formatDate(d)))
                        licT += ("Maintenance time",
                            if (l.maintenanceTime() > 0) l.maintenanceTime() + " months" else "No restriction")
                        licT += ("Issue organization", safe(l.issueOrganization(), "<n/a>"))
                        licT += ("User name", safe(l.userName(), "<n/a>"))
                        licT += ("User organization", safe(l.userOrganization(), "<n/a>"))
                        licT += ("User organization URL", safe(l.userWww(), "<n/a>"))
                        licT += ("User organization e-mail", safe(l.userEmail(), "<n/a>"))
                        licT += ("License note", safe(l.note(), "<n/a>"))
                        licT += ("Expire date", Option(l.expireDate()).fold("No restriction")(d => formatDate(d)))
                        licT += ("Maximum number of nodes", if (l.maxNodes() > 0) l.maxNodes() else "No restriction")
                        licT += ("Maximum number of computers", if (l.maxComputers() > 0) l.maxComputers() else "No restriction")
                        licT += ("Maximum number of CPUs", if (l.maxCpus() > 0) l.maxCpus() else "No restriction")
                        licT += ("Maximum up time", if (l.maxUpTime() > 0) l.maxUpTime() + " min." else "No restriction")
                        licT += ("Grace/burst period", if (l.gracePeriod() > 0) l.gracePeriod() + " min." else "No grace/burst period")
                        licT += ("Disabled subsystems", Option(l.disabledSubsystems()).
                            fold("No disabled subsystems")(s => s.split(',').toList.toString()))

                        licT.render()
                    }
                }
            }
        }
    }

    /**
     * ===Command===
     * Updates license with provided ID.
     *
     * ===Examples===
     * <ex>license "-f=/path/to/new/license.xml -id=fbdea781-90e6-4d1b-b8b3-5b8c14aa2df7"</ex>
     * Copies new license file to all nodes that use license with provided ID.
     *
     * @param args Command arguments.
     */
    def license(args: String) {
        assert(args != null)

        val argLst = parseArgs(args)

        val path = argValue("f", argLst)
        val id = argValue("id", argLst)

        if (!path.isDefined)
            scold("Path to new license file is not defined.")
        else if (!id.isDefined)
            scold("Old license ID is not defined.")
        else {
            val licId = id.get
            val licPath = path.get

            try {
                val nodes = grid.nodes()

                nodes.foreach(n => {
                    grid.forNode(n).compute().withNoFailover().
                        execute(classOf[VisorLicenseUpdateTask], toTaskArgument(n.id,
                        new GridBiTuple(UUID.fromString(licId), Source.fromFile(licPath).mkString))).get
                })

                println("All licenses have been updated.")

                nl()

                license()
            }
            catch {
                case _: IllegalArgumentException => scold("Invalid License ID: " + licId)
                case _: FileNotFoundException => scold("File not found: " + licPath)
                case _: IOException => scold("Failed to read the license file: " + licPath)
                case _: GridException => scold(
                    "Failed to update the license due to system error.",
                    "Note: Some licenses may haven been updated."
                )
            }
        }
    }
}

/**
 * Companion object that does initialization of the command.
 */
object VisorLicenseCommand {
    addHelp(
        name = "license",
        shortInfo = "Shows information about licenses and updates them.",
        longInfo = List(
            "Shows information about all licenses that are used on the grid.",
            "Also can be used to update on of the licenses."
        ),
        spec = List(
            "license",
            "license -f=<path> -id=<license-id>"
        ),
        args = List(
            "-f=<path>" -> "Path to new license XML file.",
            "-id=<license-id>" -> "ID of the license will be updated."
        ),
        examples = List(
            "license" ->
                "Shows all licenses that are used on the grid.",
            "license -f=/path/to/new/license.xml -id=fbdea781-90e6-4d1b-b8b3-5b8c14aa2df7" ->
                "Copies new license file to all nodes that use license with provided ID."
        ),
        ref = VisorConsoleCommand(cmd.license, cmd.license)
    )

    /** Singleton command. */
    private val cmd = new VisorLicenseCommand

    /**
     * Singleton.
     */
    def apply() = cmd

    /**
     * Implicit converter from visor to commands "pimp".
     *
     * @param vs Visor tagging trait.
     */
    implicit def fromLicense2Visor(vs: VisorTag) = cmd
}
