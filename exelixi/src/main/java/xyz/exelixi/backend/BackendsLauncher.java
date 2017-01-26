package xyz.exelixi.backend;

import static xyz.exelixi.Settings.*;

import se.lth.cs.tycho.reporting.Reporter;
import se.lth.cs.tycho.settings.Setting;
import se.lth.cs.tycho.settings.SettingsManager;
import xyz.exelixi.Settings;
import xyz.exelixi.compiler.HLS.ExelixiHLSLoader;

import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * @author Simone Casale-Brunet
 */
public class BackendsLauncher {

    private BackendsFactory backendsFactory;
    private SettingsManager settingsManager;

    public static void main(String[] args) {
        BackendsLauncher launcher = new BackendsLauncher();
        launcher.run(args);
    }

    public BackendsLauncher() {
        backendsFactory = BackendsFactory.INSTANCE;

        // build a set of unique settings
        Collection<Setting> settings = new HashSet<>();

        // these are the default options required by every backend
        settings.add(sourcePaths);
        settings.add(orccSourcePaths);
        settings.add(xdfSourcePaths);
        settings.add(targetPath);
        settings.add(Reporter.reportingLevel);
        settings.add(phaseTimer);
        settings.add(backend);

        //FIXME to be corrected
        settings.add(ExelixiHLSLoader.followLinks);

        // collect all the settings required by the available phases
        settings.addAll(backendsFactory.getRegisteredPhases().stream().flatMap(phase -> phase.getPhaseSettings().stream())
                .collect(Collectors.toList()));

        // create the list to be passed to the builder
        List<Setting<?>> list = new ArrayList<>();
        settings.forEach(s -> list.add(s));

        // finally create the settings manager
        settingsManager = SettingsManager.builder().addAll(list).build();

    }

    public void run(String[] args) {

        System.out.println(backend.getDescription());

    }
}
