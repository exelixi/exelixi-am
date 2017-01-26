package xyz.exelixi.phases;

import org.multij.MultiJ;
import se.lth.cs.tycho.comp.CompilationTask;
import se.lth.cs.tycho.comp.Context;
import se.lth.cs.tycho.ir.network.Instance;
import se.lth.cs.tycho.phases.Phase;
import xyz.exelixi.Settings;
import xyz.exelixi.phases.vivadohls.VivadoHLSBackend;
import xyz.exelixi.utils.Utils;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Vivado HLS Backend Phase
 */
public class VivadoHLSBackendPhase implements Phase {

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
        VivadoHLSBackend backend = openBackend(task, context);

        // -- Generator Actor Code
        generateActors(backend);

        return task;
    }

    private void generateActors(VivadoHLSBackend backend) {
        for (Instance instance : backend.task().getNetwork().getInstances()) {
            // -- Generate header code
            backend.actor().generateHeaderCode(instance, includePath);
            // -- Generate source code
            backend.actor().generateSourceCode(instance, srcPath);
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
    private VivadoHLSBackend openBackend(CompilationTask task, Context context) {
        return MultiJ.from(VivadoHLSBackend.class)
                .bind("task").to(task)
                .bind("context").to(context)
                .instance();
    }

}
