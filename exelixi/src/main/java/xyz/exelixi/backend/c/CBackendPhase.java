package xyz.exelixi.backend.c;

import org.multij.MultiJ;
import se.lth.cs.tycho.comp.CompilationTask;
import se.lth.cs.tycho.comp.Context;
import se.lth.cs.tycho.ir.decl.GlobalEntityDecl;
import se.lth.cs.tycho.ir.network.Instance;
import se.lth.cs.tycho.phases.Phase;
import se.lth.cs.tycho.reporting.Diagnostic;
import xyz.exelixi.Settings;
import xyz.exelixi.backend.c.codegen.Emitter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * An alterative C Backend for Tycho
 *
 * @author Endri Bezati
 */
public class CBackendPhase implements Phase {

    /**
     * Source path
     */
    private Path srcPath;

    /**
     * Include Path
     */
    private Path includePath;

    /**
     * Target Path
     */
    private Path targetPath;


    @Override
    public String getDescription() {
        return "C code Generation for Actor Machines";
    }

    @Override
    public CompilationTask execute(CompilationTask task, Context context) {
        String targetName = task.getIdentifier().getLast().toString();

        // -- Create src and include directories
        targetPath = context.getConfiguration().get(Settings.targetPath);
        srcPath = createDirectory(targetPath, "src");
        includePath = createDirectory(targetPath, "include");
        createDirectory(targetPath, "create");
        createDirectory(targetPath, "bin");

        // -- Generate Libs
        generateLibs(task, context);

        // -- Generate Main File
        generateMain(task, context);

        // -- Generate Actors Machine
        generateActorMachines(task, context);

        // -- Generate Callables
        generateCallables(task, context);

        // -- Generate Network
        generateNetwork(task, context);

        // -- Generate CMakeLists
        generateCMakeLists(task, context);
        return task;
    }

    private void generateCMakeLists(CompilationTask task, Context context) {
        // -- Main CMakeLists
        Path mainCMakeList = targetPath.resolve("CMakeLists.txt");
        withBackend(task, context, mainCMakeList, backend -> backend.cmakeLists().generateMainCMakeList());
        // -- Source CMakeLists
        Path srcCMakeList = srcPath.resolve("CMakeLists.txt");
        withBackend(task, context, srcCMakeList, backend -> backend.cmakeLists().generateSourceCMakeList());
    }

    private void generateCallables(CompilationTask task, Context context) {
        // -- Header Callables
        Path header = includePath.resolve("callables.h");
        withBackend(task, context, header, backend -> backend.callables().generateHeaderCode());

        // -- Source Callables
        Path source = srcPath.resolve("callables.c");
        withBackend(task, context, source, backend -> backend.callables().generateSourceCode());
    }

    /**
     * Create a Directory
     *
     * @param parent
     * @param name
     * @return
     */
    private Path createDirectory(Path parent, String name) {
        try {
            Path path = parent.resolve(name);
            if (!path.toFile().exists()) {
                return Files.createDirectory(parent.resolve(name));
            } else {
                return path;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Generate Network Code
     *
     * @param task
     * @param context
     */
    private void generateNetwork(CompilationTask task, Context context) {
        String targetName = task.getIdentifier().getLast().toString();
        withBackend(task, context, srcPath.resolve(targetName + ".c"), backend -> backend.mainNetwork().generateCode());
    }


    /**
     * Generate Main Function File
     *
     * @param task
     * @param context
     */
    private void generateMain(CompilationTask task, Context context) {
        Path path = srcPath.resolve("main.c");
        withBackend(task, context, path, backend -> backend.main().generateCode());
    }


    /**
     * Generate Library for the generated code
     *
     * @param task
     * @param context
     */
    private void generateLibs(CompilationTask task, Context context) {
        withBackend(task, context, includePath.resolve("fifo.h"), backend -> backend.channels().fifo_h());
    }


    /**
     * Generate the C code for Actor Machines
     *
     * @param task
     * @param context
     */
    private void generateActorMachines(CompilationTask task, Context context) {
        List<GlobalEntityDecl> entityDecls = task.getSourceUnits().stream().flatMap(unit -> unit.getTree().getEntityDecls().stream()).collect(Collectors.toList());
        for (GlobalEntityDecl globalEntityDecl : entityDecls) {
            String name = globalEntityDecl.getName();
            // -- Header File
            Path actorTargetHeader = includePath.resolve(name + ".h");
            withBackend(task, context, actorTargetHeader, backend -> backend.structure().actorHeaderDecl(globalEntityDecl));

            // -- Source File
            Path actorTargetSource = srcPath.resolve(name + ".c");
            withBackend(task, context, actorTargetSource, backend -> backend.structure().actorSourceDecl(globalEntityDecl));
        }
    }

    /**
     * Generate the source code
     *
     * @param task
     * @param context
     * @param target
     * @param action
     */
    private void withBackend(CompilationTask task, Context context, Path target, Consumer<CBackendCore> action) {
        try (CBackendCore backend = openBackend(task, context, null, target)) {
            action.accept(backend);
        } catch (IOException e) {
            context.getReporter().report(new Diagnostic(Diagnostic.Kind.ERROR, "Could not generate code to \"" + target + "\""));
        }
    }

    /**
     * Get the Backend
     *
     * @param task
     * @param context
     * @param instance
     * @param path
     * @return
     * @throws IOException
     */
    private CBackendCore openBackend(CompilationTask task, Context context, Instance instance, Path path) throws IOException {
        return MultiJ.from(CBackendCore.class)
                .bind("task").to(task)
                .bind("context").to(context)
                .bind("instance").to(instance)
                .bind("emitter").to(new Emitter(path))
                .instance();
    }
}
