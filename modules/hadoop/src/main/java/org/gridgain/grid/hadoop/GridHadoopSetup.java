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

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.hadoop;

import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.text.*;
import java.util.*;

import static org.gridgain.grid.kernal.GridProductImpl.*;

/**
 * Setup tool to configure Hadoop client.
 */
public class GridHadoopSetup {
    /** */
    public static final String WINUTILS_EXE = "winutils.exe";

    /** */
    private static final FilenameFilter GRIDGAIN_JARS = new FilenameFilter() {
        @Override public boolean accept(File dir, String name) {
            return name.startsWith("gridgain-") && name.endsWith(".jar");
        }
    };

    /**
     * The main method.
     * @param ignore Params.
     * @throws IOException If fails.
     */
    public static void main(String[] ignore) throws IOException {
        X.println(
            "  _____     _     _______      _         ",
            " / ___/____(_)___/ / ___/___ _(_)___     ",
            "/ (_ // __/ // _  / (_ // _ `/ // _ \\   ",
            "\\___//_/ /_/ \\_,_/\\___/ \\_,_/_//_//_/",
            "                for Apache Hadoop        ",
            "  ");

        println("Version " + ACK_VER);

        configureHadoop();
    }

    /**
     * This operation prepares the clean unpacked Hadoop distributive to work as client with GridGain-Hadoop.
     * It performs these operations:
     * <ul>
     *     <li>Check for setting of HADOOP_HOME environment variable.</li>
     *     <li>Try to resolve HADOOP_COMMON_HOME or evaluate it relative to HADOOP_HOME.</li>
     *     <li>In Windows check if winutils.exe exists and try to fix issue with some restrictions.</li>
     *     <li>In Windows check new line character issues in CMD scripts.</li>
     *     <li>Scan Hadoop lib directory to detect GridGain JARs. If these don't exist tries to create ones.</li>
     * </ul>
     */
    private static void configureHadoop() {
        String gridgainHome = U.getGridGainHome();

        println("GRIDGAIN_HOME is set to '" + gridgainHome + "'.");

        checkGridGainHome(gridgainHome);

        String homeVar = "HADOOP_HOME";
        String hadoopHome = System.getenv(homeVar);

        if (F.isEmpty(hadoopHome)) {
            homeVar = "HADOOP_PREFIX";
            hadoopHome = System.getenv(homeVar);
        }

        if (F.isEmpty(hadoopHome))
            exit("Neither HADOOP_HOME nor HADOOP_PREFIX environment variable is set. Please set one of them to a " +
                "valid Hadoop installation directory and run setup tool again.", null);

        hadoopHome = hadoopHome.replaceAll("\"", "");

        println(homeVar + " is set to '" + hadoopHome + "'.");

        String hiveHome = System.getenv("HIVE_HOME");

        if (!F.isEmpty(hiveHome)) {
            hiveHome = hiveHome.replaceAll("\"", "");

            println("HIVE_HOME is set to '" + hiveHome + "'.");
        }

        File hadoopDir = new File(hadoopHome);
        
        if (!hadoopDir.exists())
            exit("Hadoop installation folder does not exist.", null);

        if (!hadoopDir.isDirectory())
            exit("HADOOP_HOME must point to a directory.", null);

        if (!hadoopDir.canRead())
            exit("Hadoop installation folder can not be read. Please check permissions.", null);

        File hadoopCommonDir;

        String hadoopCommonHome = System.getenv("HADOOP_COMMON_HOME");

        if (F.isEmpty(hadoopCommonHome)) {
            hadoopCommonDir = new File(hadoopDir, "share/hadoop/common");

            println("HADOOP_COMMON_HOME is not set, will use '" + hadoopCommonDir.getPath() + "'.");
        }
        else {
            println("HADOOP_COMMON_HOME is set to '" + hadoopCommonHome + "'.");

            hadoopCommonDir = new File(hadoopCommonHome);
        }

        if (!hadoopCommonDir.canRead())
            exit("Failed to read Hadoop common dir in '" + hadoopCommonHome + "'.", null);

        File hadoopCommonLibDir = new File(hadoopCommonDir, "lib");

        if (!hadoopCommonLibDir.canRead())
            exit("Failed to read Hadoop 'lib' folder in '" + hadoopCommonLibDir.getPath() + "'.", null);

        if (U.isWindows()) {
            checkJavaPathSpaces();

            File hadoopBinDir = new File(hadoopDir, "bin");

            if (!hadoopBinDir.canRead())
                exit("Failed to read subdirectory 'bin' in HADOOP_HOME.", null);

            File winutilsFile = new File(hadoopBinDir, WINUTILS_EXE);

            if (!winutilsFile.exists()) {
                if (ask("File '" + WINUTILS_EXE + "' does not exist. " +
                    "It may be replaced by a stub. Create it?")) {
                    println("Creating file stub '" + winutilsFile.getAbsolutePath() + "'.");

                    boolean ok = false;

                    try {
                        ok = winutilsFile.createNewFile();
                    }
                    catch (IOException ignore) {
                        // No-op.
                    }

                    if (!ok)
                        exit("Failed to create '" + WINUTILS_EXE + "' file. Please check permissions.", null);
                }
                else
                    println("Ok. But Hadoop client probably will not work on Windows this way...");
            }

            processCmdFiles(hadoopDir, "bin", "sbin", "libexec");
        }

        File gridgainLibs = new File(new File(gridgainHome), "libs");

        if (!gridgainLibs.exists())
            exit("GridGain 'libs' folder is not found.", null);

        Collection<File> jarFiles = new ArrayList<>();

        addJarsInFolder(jarFiles, gridgainLibs);
        addJarsInFolder(jarFiles, new File(gridgainLibs, "gridgain-hadoop"));

        boolean jarsLinksCorrect = true;

        for (File file : jarFiles) {
            File link = new File(hadoopCommonLibDir, file.getName());

            jarsLinksCorrect &= isJarLinkCorrect(link, file);

            if (!jarsLinksCorrect)
                break;
        }

        if (!jarsLinksCorrect) {
            if (ask("GridGain JAR files are not found in Hadoop 'lib' directory. " +
                "Create appropriate symbolic links?")) {
                File[] oldGridGainJarFiles = hadoopCommonLibDir.listFiles(GRIDGAIN_JARS);

                if (oldGridGainJarFiles.length > 0 && ask("The Hadoop 'lib' directory contains JARs from other GridGain " +
                    "installation. They must be deleted to continue. Continue?")) {
                    for (File file : oldGridGainJarFiles) {
                        println("Deleting file '" + file.getAbsolutePath() + "'.");

                        if (!file.delete())
                            exit("Failed to delete file '" + file.getPath() + "'.", null);
                    }
                }

                for (File file : jarFiles) {
                    File targetFile = new File(hadoopCommonLibDir, file.getName());

                    try {
                        println("Creating symbolic link '" + targetFile.getAbsolutePath() + "'.");

                        Files.createSymbolicLink(targetFile.toPath(), file.toPath());
                    }
                    catch (IOException e) {
                        if (U.isWindows()) {
                            warn("Ability to create symbolic links is required!");
                            warn("On Windows platform you have to grant permission 'Create symbolic links'");
                            warn("to your user or run the Accelerator as Administrator.");
                        }

                        exit("Creating symbolic link failed! Check permissions.", e);
                    }
                }
            }
            else
                println("Ok. But Hadoop client will not be able to talk to GridGain cluster without those JARs in classpath...");
        }

        File hadoopEtc = new File(hadoopDir, "etc" + File.separator + "hadoop");

        File gridgainDocs = new File(gridgainHome, "docs");

        if (!gridgainDocs.canRead())
            exit("Failed to read GridGain 'docs' folder at '" + gridgainDocs.getAbsolutePath() + "'.", null);

        if (hadoopEtc.canWrite()) { // TODO Bigtop
            if (ask("Replace 'core-site.xml' and 'mapred-site.xml' files with preconfigured templates " +
                "(existing files will be backed up)?")) {
                replaceWithBackup(new File(gridgainDocs, "core-site.gridgain.xml"), new File(hadoopEtc, "core-site.xml"));

                replaceWithBackup(new File(gridgainDocs, "mapred-site.gridgain.xml"), new File(hadoopEtc, "mapred-site.xml"));
            }
            else
                println("Ok. You can configure them later, the templates are available at GridGain's 'docs' directory...");
        }

        if (!F.isEmpty(hiveHome)) {
            File hiveConfDir = new File(hiveHome + File.separator + "conf");

            if (!hiveConfDir.canWrite())
                warn("Can not write to '" + hiveConfDir.getAbsolutePath() + "'. To run Hive queries you have to " +
                    "configure 'hive-site.xml' manually. The template is available at GridGain's 'docs' directory.");
            else if (ask("Replace 'hive-site.xml' with preconfigured template (existing file will be backed up)?"))
                replaceWithBackup(new File(gridgainDocs, "hive-site.gridgain.xml"), new File(hiveConfDir, "hive-site.xml"));
            else
                println("Ok. You can configure it later, the template is available at GridGain's 'docs' directory...");
        }

        println("Apache Hadoop setup is complete.");
    }

    /**
     * @param jarFiles Jars.
     * @param folder Folder.
     */
    private static void addJarsInFolder(Collection<File> jarFiles, File folder) {
        if (!folder.exists())
            exit("Folder '" + folder.getAbsolutePath() + "' is not found.", null);

        jarFiles.addAll(Arrays.asList(folder.listFiles(GRIDGAIN_JARS)));
    }

    /**
     * Checks that JAVA_HOME does not contain space characters.
     */
    private static void checkJavaPathSpaces() {
        String javaHome = System.getProperty("java.home");

        if (javaHome.contains(" ")) {
            warn("Java installation path contains space characters!");
            warn("Hadoop client will not be able to start using '" + javaHome + "'.");
            warn("Please install JRE to path which does not contain spaces and point JAVA_HOME to that installation.");
        }
    }

    /**
     * Checks GridGain home.
     *
     * @param ggHome GridGain home.
     */
    private static void checkGridGainHome(String ggHome) {
        URL jarUrl = U.class.getProtectionDomain().getCodeSource().getLocation();

        try {
            Path jar = Paths.get(jarUrl.toURI());
            Path gg = Paths.get(ggHome);

            if (!jar.startsWith(gg))
                exit("GridGain JAR files are not under GRIDGAIN_HOME.", null);
        }
        catch (Exception e) {
            exit(e.getMessage(), e);
        }
    }

    /**
     * Replaces target file with source file.
     *
     * @param from From.
     * @param to To.
     */
    private static void replaceWithBackup(File from, File to) {
        if (!from.canRead())
            exit("Failed to read source file '" + from.getAbsolutePath() + "'.", null);

        println("Replacing file '" + to.getAbsolutePath() + "'.");

        try {
            U.copy(from, renameToBak(to), true);
        }
        catch (IOException e) {
            exit("Failed to replace file '" + to.getAbsolutePath() + "'.", e);
        }
    }

    /**
     * Renames file for backup.
     *
     * @param file File.
     * @return File.
     */
    private static File renameToBak(File file) {
        DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

        if (file.exists() && !file.renameTo(new File(file.getAbsolutePath() + "." + fmt.format(new Date()) + ".bak")))
            exit("Failed to rename file '" + file.getPath() + "'.", null);

        return file;
    }

    /**
     * Checks if link is correct.
     *
     * @param link Symbolic link.
     * @param correctTarget Correct link target.
     * @return {@code true} If link target is correct.
     */
    private static boolean isJarLinkCorrect(File link, File correctTarget) {
        if (!Files.isSymbolicLink(link.toPath()))
            return false; // It is a real file or it does not exist.

        Path target = null;

        try {
            target = Files.readSymbolicLink(link.toPath());
        }
        catch (IOException e) {
            exit("Failed to read symbolic link: " + link.getAbsolutePath(), e);
        }

        return Files.exists(target) && target.toFile().equals(correctTarget);
    }

    /**
     * Writes the question end read the boolean answer from the console.
     *
     * @param question Question to write.
     * @return {@code true} if user inputs 'Y' or 'y', {@code false} otherwise.
     */
    private static boolean ask(String question) {
        X.println();
        X.print(" <  " + question + " (Y/N): ");

        String answer = null;

        if (!F.isEmpty(System.getenv("GRIDGAIN_HADOOP_SETUP_YES")))
            answer = "Y";
        else {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

            try {
                answer = br.readLine();
            }
            catch (IOException e) {
                exit("Failed to read answer: " + e.getMessage(), e);
            }
        }

        if (answer != null && "Y".equals(answer.toUpperCase().trim())) {
            X.println(" >  Yes.");

            return true;
        }
        else {
            X.println(" >  No.");

            return false;
        }
    }

    /**
     * Exit with message.
     *
     * @param msg Exit message.
     */
    private static void exit(String msg, Exception e) {
        X.println("    ");
        X.println("  # " + msg);
        X.println("  # Setup failed, exiting... ");

        if (e != null && !F.isEmpty(System.getenv("GRIDGAIN_HADOOP_SETUP_DEBUG")))
            e.printStackTrace();

        System.exit(1);
    }

    /**
     * Prints message.
     *
     * @param msg Message.
     */
    private static void println(String msg) {
        X.println("  > " + msg);
    }

    /**
     * Prints warning.
     *
     * @param msg Message.
     */
    private static void warn(String msg) {
        X.println("  ! " + msg);
    }

    /**
     * Checks that CMD files have valid MS Windows new line characters. If not, writes question to console and reads the
     * answer. If it's 'Y' then backups original files and corrects invalid new line characters.
     *
     * @param rootDir Root directory to process.
     * @param dirs Directories inside of the root to process.
     */
    private static void processCmdFiles(File rootDir, String... dirs) {
        boolean answer = false;

        for (String dir : dirs) {
            File subDir = new File(rootDir, dir);

            File[] cmdFiles = subDir.listFiles(new FilenameFilter() {
                @Override public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".cmd");
                }
            });

            for (File file : cmdFiles) {
                String content = null;

                try (Scanner scanner = new Scanner(file)) {
                    content = scanner.useDelimiter("\\Z").next();
                }
                catch (FileNotFoundException e) {
                    exit("Failed to read file '" + file + "'.", e);
                }

                boolean invalid = false;

                for (int i = 0; i < content.length(); i++) {
                    if (content.charAt(i) == '\n' && (i == 0 || content.charAt(i - 1) != '\r')) {
                        invalid = true;

                        break;
                    }
                }

                if (invalid) {
                    answer = answer || ask("One or more *.CMD files has invalid new line character. Replace them?");

                    if (!answer) {
                        println("Ok. But Windows most probably will fail to execute them...");

                        return;
                    }

                    println("Fixing newline characters in file '" + file.getAbsolutePath() + "'.");

                    renameToBak(file);

                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                        for (int i = 0; i < content.length(); i++) {
                            if (content.charAt(i) == '\n' && (i == 0 || content.charAt(i - 1) != '\r'))
                                writer.write("\r");

                            writer.write(content.charAt(i));
                        }
                    }
                    catch (IOException e) {
                        exit("Failed to write file '" + file.getPath() + "': " + e.getMessage(), e);
                    }
                }
            }
        }
    }
}
