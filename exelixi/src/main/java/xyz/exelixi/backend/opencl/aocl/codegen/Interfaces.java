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
        parameters.add("__global int * restrict in");
        parameters.add("__global int * restrict read_ptr");
        parameters.add("__global int * restrict write_ptr");
        parameters.add("write_only pipe int __attribute__((blocking)) FIFO_" + fifoId);
        emitter().emit("__kernel void %s(%s){", name, String.join(", ", parameters));
        emitter().emit("");
        emitter().increaseIndentation();

        emitter().emit("int tmp_read  = *read_ptr;");
        emitter().emit("int tmp_write = *write_ptr;");
        emitter().emit("int value = 0;"); // FIXME add fifo type
        emitter().emit("int count = (FIFO_DEPTH + tmp_write - tmp_read) %% (FIFO_DEPTH);");
        emitter().emit("for (int i = 0; i < count; ++i) {");
        emitter().increaseIndentation();
        emitter().emit("value = in[(i + tmp_read) %% FIFO_DEPTH];");
        emitter().emit("write_pipe(FIFO_%d, &value);", fifoId);
        emitter().decreaseIndentation();
        emitter().emit("}");
        emitter().emit("");
        emitter().emit("if(count){");
        emitter().increaseIndentation();
        emitter().emit("*read_ptr = (tmp_read + count) %% FIFO_DEPTH;");
        emitter().decreaseIndentation();
        emitter().emit(" }");
        emitter().decreaseIndentation();
        emitter().emit("}");

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
        parameters.add("__global int * restrict out");
        parameters.add("__global int * restrict read_ptr");
        parameters.add("__global int * restrict write_ptr");
        parameters.add("read_only pipe int FIFO_" + fifoId);
        emitter().emit("__kernel void %s(%s){", name, String.join(", ", parameters));
        emitter().emit("");
        emitter().increaseIndentation();

        emitter().emit("int value = 0;");
        emitter().emit("int tmp_read = *read_ptr;");
        emitter().emit("int tmp_write = *write_ptr;");
        emitter().emit("int count = 0;");
        emitter().emit("int rooms = (FIFO_DEPTH + tmp_read - tmp_write - 1) %% FIFO_DEPTH;");
        emitter().emit("if(rooms) {");
        emitter().increaseIndentation();
        emitter().emit("while(read_pipe(FIFO_%d, &value)>=0) {", fifoId);
        emitter().increaseIndentation();
        emitter().emit("out[(tmp_write + count) %% FIFO_DEPTH] = value;");
        emitter().emit("rooms--;");
        emitter().emit("count++;");
        emitter().emit("if(rooms == 0) break;");
        emitter().decreaseIndentation();
        emitter().emit("}");
        emitter().emit("");
        emitter().emit("if(count) {");
        emitter().increaseIndentation();
        emitter().emit("*write_ptr = (tmp_write + count) %% FIFO_DEPTH;");
        emitter().decreaseIndentation();
        emitter().emit("}");
        emitter().decreaseIndentation();
        emitter().emit("}");

        emitter().decreaseIndentation();
        emitter().emit("}");
        emitter().increaseIndentation();

        emitter().close();
    }


}
