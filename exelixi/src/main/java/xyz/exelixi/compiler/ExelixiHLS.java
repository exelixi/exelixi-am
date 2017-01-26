/*
 * EXELIXI
 *
 * Copyright (C) 2017 Endri Bezati, EPFL SCI-STI-MM
 *
 * This file is part of EXELIXI.
 *
 * EXELIXI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * EXELIXI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with EXELIXI. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7
 *
 * If you modify this Program, or any covered work, by linking or combining it
 * with Eclipse (or a modified version of Eclipse or an Eclipse plugin or
 * an Eclipse library), containing parts covered by the terms of the
 * Eclipse Public License (EPL), the licensors of this Program grant you
 * additional permission to convey the resulting work.  Corresponding Source
 * for a non-source form of such a combination shall include the source code
 * for the parts of Eclipse libraries used as well as that of the covered work.
 *
 */

package xyz.exelixi.compiler;

import se.lth.cs.tycho.ir.QID;
import se.lth.cs.tycho.settings.Configuration;
import se.lth.cs.tycho.settings.Setting;
import se.lth.cs.tycho.settings.SettingsManager;
import xyz.exelixi.compiler.HLS.ExelixiHLSCompiler;
import xyz.exelixi.compiler.HLS.ExelixiHLSLoader;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Exelixi for Vivado HLS Compiler
 *
 * @author Endri Bezati
 */
public class ExelixiHLS {
    private static final String toolName = "exelixi_hls";
    private static final String toolFullName = "Exelixi for Vivado HLS Compiler";
    private static final String toolVersion = "0.0.1-SNAPSHOT";

    public static void main(String[] args) {
        ExelixiHLS main = new ExelixiHLS();
        main.run(args);
    }

    private void run(String... args) {
        SettingsManager settingsManager = ExelixiHLSCompiler.defaultSettingsManager();
        Configuration.Builder builder = Configuration.builder(settingsManager);
        List<String> promotedSettings = promotedSettings();
        QID qid = null;
        int i = 0;
        try {
            while (i < args.length) {
                switch (args[i]) {
                    case "--help": {
                        printHelp(promotedSettings, settingsManager);
                        System.exit(0);
                    }
                    case "--version": {
                        printVersion();
                        System.exit(0);
                    }
                    case "--print-phases": { // hidden option
                        printPhases();
                        System.exit(0);
                    }
                    case "--settings": {
                        printSettings(settingsManager);
                        System.exit(0);
                    }
                    case "--set": {
                        if (i + 3 >= args.length) {
                            printMissingArguments("--set");
                            System.exit(1);
                        }
                        builder.set(args[i + 1], args[i + 2]);
                        i += 3;
                        break;
                    }
                    default: {
                        if (args[i].startsWith("--")) {
                            if (promotedSettings.contains(args[i].substring(2))) {
                                if (i + 2 >= args.length) {
                                    printMissingArguments(args[i]);
                                    System.exit(1);
                                }
                                builder.set(args[i].substring(2), args[i + 1]);
                                i += 2;
                            } else {
                                printUnknownArgument(args[i]);
                                System.exit(1);
                            }
                        } else if (i == args.length - 1) {
                            qid = QID.parse(args[i]);
                            i += 1;
                        } else {
                            printUnknownArgument(args[i]);
                            System.exit(1);
                        }
                    }
                }
            }
        } catch (Configuration.Builder.UnknownKeyException e) {
            System.out.println("Unknown setting \"" + e.getKey() + "\"");
            System.exit(1);
        } catch (Configuration.Builder.ReadException e) {
            System.out.println("Could not parse value \"" + e.getValue() + "\" for setting \"" + e.getKey() + "\"");
            System.exit(1);
        }

        if (qid == null) {
            printMissingEntity();
            System.exit(1);
        }


        Configuration config = builder.build();
        ExelixiHLSCompiler compiler = new ExelixiHLSCompiler(config);
        if (!compiler.compile(qid)) {
            System.exit(1);
        }
    }

    private void printPhases() {
        ExelixiHLSCompiler.phases.forEach(phase -> {
            System.out.println(phase.getName());
            System.out.println(phase.getDescription());
            System.out.println();
        });
    }

    private void printSettings(SettingsManager settingsManager) {
        System.out.println("These are the settings available through --set <key> <value>");
        System.out.println();
        for (Setting<?> setting : settingsManager.getAllSettings()) {
            System.out.println(setting.getKey() + " <" + setting.getType() + ">");
            System.out.println("\t" + setting.getDescription());
        }
    }

    private void printMissingArguments(String option) {
        System.out.println("Missing argument to " + option);
        printUsage();
    }

    private void printUnknownArgument(String arg) {
        System.out.println("Unknown argument \"" + arg + "\"");
        printUsage();
    }

    private void printHelp(List<String> promotedSettings, SettingsManager settingsManager) {
        printVersion();
        printUsage();
        System.out.println();
        System.out.println("Available options:");
        System.out.println("--help");
        System.out.println("\tPrints this help message and exits.");
        System.out.println("--version");
        System.out.println("\tPrints the version and exits.");
        System.out.println("--set <key> <value>");
        System.out.println("\tSets the compiler setting <key> to <value>.");
        System.out.println("--settings");
        System.out.println("\tPrints all available settings and exits.");
        for (String key : promotedSettings) {
            Setting<?> setting = settingsManager.get(key);
            System.out.println("--" + key + " <" + setting.getType() + ">");
            System.out.println("\t" + setting.getDescription());
        }
        System.out.println();
        System.out.println("Examples:");
        System.out.println("exelixi_hls --source-paths src" + File.pathSeparator + "lib --target-path target com.example.Example");
        System.out.println("exelixi_hls --set some-option a-value com.example.Example");
    }

    private List<String> promotedSettings() {
        return Stream.of(
                ExelixiHLSCompiler.sourcePaths,
                ExelixiHLSCompiler.orccSourcePaths,
                ExelixiHLSCompiler.xdfSourcePaths,
                ExelixiHLSCompiler.targetPath,
                ExelixiHLSLoader.followLinks
        ).map(Setting::getKey).collect(Collectors.toList());
    }

    private void printVersion() {
        System.out.println(toolFullName + ", version " + toolVersion);
    }

    private void printUsage() {
        System.out.println("Usage: " + toolName + " [options] [entity]");
        System.out.println("For more information: " + toolName + " --help");
    }

    private void printMissingEntity() {
        System.out.println("No entity was specified.");
        printUsage();
        System.out.println("For a description of available options: " + toolName + " --help");
    }
}
