package xyz.exelixi.backend.opencl.aocl.codegen;

/**
 * Created by scb on 2/4/17.
 */

import org.multij.Binding;
import org.multij.Module;
import se.lth.cs.tycho.ir.network.Connection;
import se.lth.cs.tycho.phases.cbackend.Emitter;
import xyz.exelixi.backend.opencl.aocl.AoclBackendCore;
import xyz.exelixi.utils.ModelHelper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.multij.BindingKind.MODULE;

@Module
public interface Interfaces {

    @Binding(MODULE)
    AoclBackendCore backend();

    default ModelHelper helper() {
        return backend().helper().get();
    }

    default Emitter emitter() {
        return backend().emitter();
    }

    default void generateInputInterface(Connection connection, Path path) {
        int fifoId = helper().getConnectionId(connection);

        // kernel name
        String name = "interface_" + fifoId;
        // -- Filename
        String fileName = name + ".cl";

        // -- Open file for code generation
        emitter().open(path.resolve(fileName));

        emitter().emit("#include \"global.h\"");
        emitter().emit("");

        List<String> parameters = new ArrayList<>();
        parameters.add("__global int *in");
        parameters.add("__global int *read_ptr");
        parameters.add("__global int *write_ptr");
        parameters.add("write_only pipe int __attribute__((blocking)) FIFO_" + fifoId);
        emitter().emit("__kernel void %s(%s){", name, String.join(", ", parameters));
        emitter().emit("");
        emitter().increaseIndentation();

        emitter().emit("int tmp_read = *write_ptr;");
        emitter().emit("int tmp_write = *read_ptr;");


        emitter().emit("int count = (FIFO_DEPTH + tmp_write - tmp_read) %% (FIFO_DEPTH);");
        emitter().emit("for (int i = 0; i < count; ++i) {");
        emitter().increaseIndentation();
        emitter().emit("int value = in[(i + tmp_read) %% FIFO_DEPTH];");
        emitter().emit("write_pipe(AB, & value);");
        emitter().decreaseIndentation();
        emitter().emit("}");
        emitter().emit("*read_ptr = (tmp_read + count) %% FIFO_DEPTH;");

        emitter().decreaseIndentation();
        emitter().emit("}");
        emitter().increaseIndentation();

        emitter().close();
    }

    default void generateOutputInterface(Connection connection, Path path) {
        int fifoId = helper().getConnectionId(connection);

        // kernel name
        String name = "interface_" + fifoId;
        // -- Filename
        String fileName = name + ".cl";

        // -- Open file for code generation
        emitter().open(path.resolve(fileName));

        emitter().emit("#include \"global.h\"");
        emitter().emit("");

        List<String> parameters = new ArrayList<>();
        parameters.add("__global int *out");
        parameters.add("__global int *read_ptr");
        parameters.add("__global int *write_ptr");
        parameters.add("read_only pipe int __attribute__((blocking)) FIFO_" + fifoId);
        emitter().emit("__kernel void %s(%s){", name, String.join(", ", parameters));
        emitter().emit("");
        emitter().increaseIndentation();

        //emitter().emit("int tmp_read = *write_ptr;");
        //emitter().emit("int tmp_write = *read_ptr;");


        //emitter().emit("int count = (FIFO_DEPTH + tmp_write - tmp_read) %% (FIFO_DEPTH);");
        //emitter().emit("for (int i = 0; i < count; ++i) {");
        //emitter().increaseIndentation();
        //emitter().emit("int value = in[(i + tmp_read) %% FIFO_DEPTH];");
        //emitter().emit("write_pipe(AB, & value);");
        //emitter().decreaseIndentation();
        //emitter().emit("}");
        //emitter().emit("*read_ptr = (tmp_read + count) %% FIFO_DEPTH;");

        emitter().decreaseIndentation();
        emitter().emit("}");
        emitter().increaseIndentation();

        emitter().close();
    }


}
