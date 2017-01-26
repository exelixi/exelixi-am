package xyz.exelixi.backend.c.codegen;

import org.multij.Binding;
import org.multij.Module;
import se.lth.cs.tycho.comp.CompilationTask;
import se.lth.cs.tycho.ir.decl.GlobalEntityDecl;
import xyz.exelixi.backend.c.CBackendCore;

import java.util.List;
import java.util.stream.Collectors;

import static org.multij.BindingKind.MODULE;

/**
 * CMakeLists Emitter
 *
 * @author Endri Bezati
 */
@Module
public interface CMakeLists {
    @Binding(MODULE)
    CBackendCore backend();

    default Emitter emitter() {
        return backend().emitter();
    }


    default void generateMainCMakeList() {
        CompilationTask task = backend().task();
        String targetName = task.getIdentifier().getLast().toString();
        emitter().emit("# -- Generated for %s", targetName);
        emitter().emit("");
        emitter().emit("cmake_minimum_required (VERSION 2.6)");
        emitter().emit("");
        emitter().emit("project (%s)", targetName);
        emitter().emit("");
        emitter().emit("# -- Executable output folder");
        emitter().emit("set(EXECUTABLE_OUTPUT_PATH ${CMAKE_SOURCE_DIR}/bin)");
        emitter().emit("");
        emitter().emit("# -- Includes");
        emitter().emit("include_directories(./include)");
        emitter().emit("");
        emitter().emit("# -- Source sub-directory");
        emitter().emit("add_subdirectory(src)");
    }

    default void generateSourceCMakeList() {
        CompilationTask task = backend().task();
        String targetName = task.getIdentifier().getLast().toString();
        emitter().emit("# -- Generated for %s", targetName);
        emitter().emit("");
        emitter().emit("# -- Source files");
        emitter().emit("set(filenames");
        emitter().increaseIndentation();
        emitter().emit("%s.c", "callables");
        List<GlobalEntityDecl> entityDecls = task.getSourceUnits().stream().flatMap(unit -> unit.getTree().getEntityDecls().stream()).collect(Collectors.toList());
        for (GlobalEntityDecl globalEntityDecl : entityDecls) {
            String entityName = globalEntityDecl.getName();
            emitter().emit("%s.c", entityName);
        }
        emitter().emit("%s.c", targetName);
        emitter().emit("%s.c", "main");
        emitter().decreaseIndentation();
        emitter().emit(")");
        emitter().emit("");
        emitter().emit("# -- Add executable");
        emitter().emit("add_executable(%s ${filenames})", targetName);
        emitter().emit("");
    }

}
