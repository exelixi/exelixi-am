package xyz.exelixi;

import se.lth.cs.tycho.settings.*;
import xyz.exelixi.backend.BackendsFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

/**
 * @author Simone Casale-Brunet
 */
public class Settings {

    public static final Setting<String> backend = new StringSetting() {
        @Override
        public String getKey() {
            return "backend";
        }

        @Override
        public String getDescription() {
            StringBuffer stringBuffer = new StringBuffer();
            BackendsFactory.INSTANCE.getBackends().forEach(b -> stringBuffer.append("\"").append(b.getId()).append("\" "));
            return "The backend name that will be used to be used. Available options are: " + stringBuffer.toString();
        }

        @Override
        public String defaultValue(Configuration configuration) { return ""; }


    };

    public static final Setting<List<Path>> orccSourcePaths = new PathListSetting() {
        @Override
        public String getKey() {
            return "orcc-source-paths";
        }

        @Override
        public String getDescription() {
            return "A " + File.pathSeparator + "-separated list of search paths for Orcc-compatible source files.";
        }

        @Override
        public List<Path> defaultValue(Configuration configuration) {
            return Collections.emptyList();
        }
    };

    public static final Setting<List<Path>> sourcePaths = new PathListSetting() {
        @Override
        public String getKey() {
            return "source-paths";
        }

        @Override
        public String getDescription() {
            return "A " + File.pathSeparator + "-separated list of search paths for source files.";
        }

        @Override
        public List<Path> defaultValue(Configuration configuration) {
            return configuration.isDefined(orccSourcePaths) ? Collections.emptyList() : Collections.singletonList(Paths.get(""));
        }
    };

    public static final Setting<List<Path>> xdfSourcePaths = new PathListSetting() {
        @Override
        public String getKey() {
            return "xdf-source-paths";
        }

        @Override
        public String getDescription() {
            return "A " + File.pathSeparator + "-separated list of search paths for XDF networks.";
        }

        @Override
        public List<Path> defaultValue(Configuration configuration) {
            return configuration.get(orccSourcePaths);
        }
    };

    public static final Setting<Path> targetPath = new PathSetting() {
        @Override
        public String getKey() {
            return "target-path";
        }

        @Override
        public String getDescription() {
            return "Output directory for the compiled files.";
        }

        @Override
        public Path defaultValue(Configuration configuration) {
            return Paths.get("");
        }
    };

    public static final Setting<Boolean> phaseTimer = new OnOffSetting() {
        @Override
        public String getKey() {
            return "phase-timer";
        }

        @Override
        public String getDescription() {
            return "Measures the execution time of the compilation phases.";
        }

        @Override
        public Boolean defaultValue(Configuration configuration) {
            return false;
        }
    };

    public static final Setting<Boolean> fpgaDevice = new OnOffSetting() {
        @Override
        public String getKey() {
            return "fpga-device";
        }

        @Override
        public String getDescription() {
            return "The Xilinx FPGA Device.";
        }

        @Override
        public Boolean defaultValue(Configuration configuration) {
            return false;
        }
    };
}
