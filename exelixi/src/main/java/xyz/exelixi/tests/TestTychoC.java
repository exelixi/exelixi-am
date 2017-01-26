package xyz.exelixi.tests;


import se.lth.cs.tycho.ir.QID;
import se.lth.cs.tycho.settings.Configuration;
import se.lth.cs.tycho.settings.Setting;
import se.lth.cs.tycho.settings.SettingsManager;
import xyz.exelixi.compiler.C.ExelixiCompiler;
import xyz.exelixi.compiler.C.ExelixiLoader;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class TestTychoC {

    private static final String toolName = "exelixi-C";
    private static final String toolFullName = "The Exelixi Compiler";
    private static final String toolVersion = "0.0.1-SNAPSHOT";

    public static void main(String[] args) {
        TestTychoC main = new TestTychoC();
        main.run(args);
    }

    private void run(String... args) {
        SettingsManager settingsManager = ExelixiCompiler.defaultSettingsManager();
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
                        if (i+3 >= args.length) {
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
                                if (i+2 >= args.length) {
                                    printMissingArguments(args[i]);
                                    System.exit(1);
                                }
                                builder.set(args[i].substring(2), args[i + 1]);
                                i += 2;
                            } else {
                                printUnknownArgument(args[i]);
                                System.exit(1);
                            }
                        } else if (i == args.length-1) {
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
        ExelixiCompiler compiler = new ExelixiCompiler(config);
        if (!compiler.compile(qid)) {
            System.exit(1);
        }
    }

    private void printPhases() {
        ExelixiCompiler.phases.forEach(phase -> {
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
        System.out.println("tychoc --source-paths src"+ File.pathSeparator+"lib --target-path target com.example.Example");
        System.out.println("tychoc --set some-option a-value com.example.Example");
    }

    private List<String> promotedSettings() {
        return Stream.of(
                ExelixiCompiler.sourcePaths,
                ExelixiCompiler.orccSourcePaths,
                ExelixiCompiler.xdfSourcePaths,
                ExelixiCompiler.targetPath,
                ExelixiLoader.followLinks
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
