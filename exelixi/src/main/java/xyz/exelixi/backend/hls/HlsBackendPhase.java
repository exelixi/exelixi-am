package xyz.exelixi.backend.hls;

import org.multij.MultiJ;
import se.lth.cs.tycho.comp.CompilationTask;
import se.lth.cs.tycho.comp.Context;
import se.lth.cs.tycho.ir.network.Instance;
import se.lth.cs.tycho.phases.Phase;
import se.lth.cs.tycho.settings.Configuration;
import se.lth.cs.tycho.settings.OnOffSetting;
import se.lth.cs.tycho.settings.Setting;
import se.lth.cs.tycho.settings.StringSetting;
import xyz.exelixi.Settings;
import xyz.exelixi.backend.BackendsRegister;
import xyz.exelixi.backend.hls.HlsBackendCore;
import xyz.exelixi.utils.Utils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Vivado HLS Backend Phase
 */
public class HlsBackendPhase implements Phase {

    /**
     * Code gen Path
     */
    private Path codeGenPath;

    /**
     * Source path
     */
    private Path srcPath;

    /**
     * Source testbench path
     */
    private Path srcTestbenchPath;

    /**
     * Include Path
     */
    private Path includePath;

    /**
     * Projects Path
     */
    private Path projectsPath;

    /**
     * Fifo Trace Path
     */
    private Path fifoTracePath;

    /**
     * Target Path
     */
    private Path targetPath;

    @Override
    public String getDescription() {
        return "Vivado HLS Phase";
    }

    @Override
    public CompilationTask execute(CompilationTask task, Context context) {
        String targetName = task.getIdentifier().getLast().toString();

        // -- Create Directories
        createDirectories(context);

        // -- Get Vivado HLS Backend
        HlsBackendCore core = openBackend(task, context);

        // -- Genrate Globals
        generateGlobals(core);

        // -- Generator Actor Code
        generateActors(core);

        // -- Generate Vivado HLS Projects
        generateTclProjects(core);
        return task;
    }

    /**
     * Generate Globals source code and header files
     */
    private void generateGlobals(HlsBackendCore backend) {
        // -- Generate Global header file
        backend.global().generateHeaderCode(includePath);
        // -- Generate Global source file
        backend.global().generateSourceCode(srcPath);
    }


    /**
     * Generate Actor Machine source code and header files
     *
     * @param backend
     */
    private void generateActors(HlsBackendCore backend) {
        for (Instance instance : backend.task().getNetwork().getInstances()) {
            // -- Generate header code
            backend.actor().generateHeaderCode(instance, includePath);
            // -- Generate source code
            backend.actor().generateSourceCode(instance, srcPath);
        }
    }

    /**
     * Generate Vivado HLS TCL projects for the design
     *
     * @param backend
     */
    private void generateTclProjects(HlsBackendCore backend) {
        for (Instance instance : backend.task().getNetwork().getInstances()) {
            backend.tclHlsProject().generateTclProject(instance, projectsPath);
        }
    }


    /**
     * Create Vivado HLS Backend directories
     *
     * @param context
     */
    private void createDirectories(Context context) {
        // -- Get target Path
        targetPath = context.getConfiguration().get(Settings.targetPath);

        // -- Code Generation Paths
        codeGenPath = Utils.createDirectory(targetPath, "code-gen");
        srcPath = Utils.createDirectory(codeGenPath, "src");
        srcTestbenchPath = Utils.createDirectory(codeGenPath, "src-tb");
        includePath = Utils.createDirectory(codeGenPath, "include");

        // -- Scripts Path
        projectsPath = Utils.createDirectory(targetPath, "projects");

        // -- Fifo trace path
        Utils.createDirectory(targetPath, "fifo-traces");
    }

    /**
     * Get the Backend
     *
     * @param task
     * @param context
     * @return
     * @throws IOException
     */
    private HlsBackendCore openBackend(CompilationTask task, Context context) {
        return MultiJ.from(HlsBackendCore.class)
                .bind("task").to(task)
                .bind("context").to(context)
                .instance();
    }

    /**
     * Board Setting
     */
    public static final Setting<String> fpgaBoard = new StringSetting() {
        @Override
        public String getKey() {
            return "Xilinx Board";
        }

        @Override
        public String getDescription() {
            return "Choose a Xilinx Board";
        }

        @Override
        public String defaultValue(Configuration configuration) {
            return "ZC702";
        }
    };

    @Override
    public List<Setting<?>> getPhaseSettings() {
        return Collections.singletonList(fpgaBoard);
    }
}
