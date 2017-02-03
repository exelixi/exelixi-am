/*
 * EXELIXI
 *
 * Copyright (C) 2017 EPFL SCI-STI-MM
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
package xyz.exelixi;

import se.lth.cs.tycho.ir.QID;
import se.lth.cs.tycho.phases.Phase;
import se.lth.cs.tycho.settings.Configuration;
import se.lth.cs.tycho.settings.Setting;
import se.lth.cs.tycho.settings.SettingsManager;
import xyz.exelixi.backend.BackendsRegister;
import xyz.exelixi.backend.ExelixiBackend;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static xyz.exelixi.Settings.*;
import static xyz.exelixi.frontend.FrontendLoader.*;
import static se.lth.cs.tycho.reporting.Reporter.*;

/**
 * @author Simone Casale-Brunet
 */
public class ExelixiAm {

    private static final String toolName = "exelixi_am";
    private static final String toolFullName = "Exelixi Actors-Machine Dataflow Compiler";
    private static final String toolVersion = "0.0.1-SNAPSHOT";

    private BackendsRegister backendsRegister;
    private SettingsManager settingsManager;
    private List<String> promotedSettings;

    public static void main(String[] args) {
        ExelixiAm launcher = new ExelixiAm();
        launcher.run(args);
    }

    public ExelixiAm() {
        backendsRegister = BackendsRegister.INSTANCE;

        // create a set of unique settings
        Collection<Setting> settings = new HashSet<>();

        // these are the default options required by every backend
        settings.add(sourcePaths);
        settings.add(orccSourcePaths);
        settings.add(xdfSourcePaths);
        settings.add(targetPath);
        settings.add(reportingLevel);
        settings.add(phaseTimer);
        settings.add(selectedBackend);
        settings.add(followLinks);

        // collect all the settings required by the available phases
        settings.addAll(backendsRegister.getRegisteredPhases().stream().flatMap(phase -> phase.getPhaseSettings().stream())
                .collect(Collectors.toList()));

        // create the list to be passed to the builder
        List<Setting<?>> list = new ArrayList<>();
        promotedSettings = new ArrayList<>();
        settings.forEach(s -> {list.add(s); promotedSettings.add(s.getKey());});

        // finally create the settings manager
        settingsManager = SettingsManager.builder().addAll(list).build();

    }

    public void run(String[] args) {
        Configuration.Builder builder = Configuration.builder(settingsManager);
        //List<String> promotedSettings = promotedSettings();

        QID qid = null;
        String backendId = null;
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
                    //FIXME add compiler
                    case "--print-phases": { // hidden option
                        printPhases(backendsRegister.getRegisteredPhases());
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
                            String argument = args[i].substring(2);
                            if (promotedSettings.contains(argument)) {
                                if (i + 2 >= args.length) {
                                    printMissingArguments(args[i]);
                                    System.exit(1);
                                }
                                builder.set(argument, args[i + 1]);

                                if(selectedBackend.getKey().equals(argument)){
                                    backendId = args[i + 1];
                                }

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

        if(backendId == null){
            printMissingBackend();
            System.exit(1);
        }else if(!backendsRegister.hasBackend(backendId)){
            printUnavailableBackend(backendId);
            System.exit(1);
        }

        // ok, now create the configuration
        Configuration configuration = builder.build();

        // select the backend
        ExelixiBackend backend = backendsRegister.getBackend(backendId);
        backend.setConfiguration(configuration);

        if (!backend.compile(qid)) {
            System.exit(1);
        }




    }

    private static void printUnavailableBackend(String name) {
        System.out.println("The backend \""+name+"\" is not registered");
        printUsage();
        System.out.println("For a description of available options: " + toolName + " --help");
    }

    private static void printMissingBackend() {
        System.out.println("No backend was specified.");
        printUsage();
        System.out.println("For a description of available options: " + toolName + " --help");
    }

    private static void printSettings(SettingsManager settingsManager) {
        System.out.println("These are the settings available through --set <key> <value>");
        System.out.println();
        for (Setting<?> setting : settingsManager.getAllSettings()) {
            System.out.println(setting.getKey() + " <" + setting.getType() + ">");
            System.out.println("\t" + setting.getDescription());
        }
    }

    private static void printMissingArguments(String option) {
        System.out.println("Missing argument to " + option);
        printUsage();
    }

    private static void printUnknownArgument(String arg) {
        System.out.println("Unknown argument \"" + arg + "\"");
        printUsage();
    }

    private static void printHelp(List<String> promotedSettings, SettingsManager settingsManager) {
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
        System.out.println(toolName + " --source-paths src" + File.pathSeparator + "lib --target-path target com.example.Example");
        System.out.println(toolName + " --set some-option a-value com.example.Example");
    }

    private static void printVersion() {
        System.out.println(toolFullName + ", version " + toolVersion);
    }

    private static void printUsage() {
        System.out.println("Usage: " + toolName + " [options] [entity]");
        System.out.println("For more information: " + toolName + " --help");
    }



    private static void printMissingEntity() {
        System.out.println("No entity was specified.");
        printUsage();
        System.out.println("For a description of available options: " + toolName + " --help");
    }

    private static void printPhases(Collection<Phase> phases) {
        phases.forEach(phase -> {
            System.out.println(phase.getName());
            System.out.println(phase.getDescription());
            System.out.println();
        });
    }

}
