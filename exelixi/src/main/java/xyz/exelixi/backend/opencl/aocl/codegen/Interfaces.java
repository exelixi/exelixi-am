package xyz.exelixi.backend.opencl.aocl.codegen;

/**
 * Created by scb on 2/4/17.
 */

import org.multij.Binding;
import org.multij.Module;
import se.lth.cs.tycho.ir.entity.PortDecl;
import se.lth.cs.tycho.ir.network.Connection;
import se.lth.cs.tycho.phases.cbackend.Emitter;
import se.lth.cs.tycho.types.Type;
import xyz.exelixi.backend.opencl.aocl.AoclBackendCore;
import xyz.exelixi.utils.Resolver;

import static java.lang.String.format;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.multij.BindingKind.MODULE;

@Module
public interface Interfaces {

    @Binding(MODULE)
    AoclBackendCore backend();

    default Resolver resolver() { return backend().resolver().get(); }

    default Emitter emitter() {
        return backend().emitter();
    }

    default void generateInputInterface(Connection connection, Path path) {
        int id = resolver().getConnectionId(connection);

        // kernel name
        String name = "interface_" + id;

        // filename
        String fileName = name + ".cl";

        // the interface type
        Type tokenType = backend().types().declaredPortType(resolver().getTargetPortDecl(connection));
        String type = backend().code().type(tokenType);

        // open file for code generation
        emitter().open(path.resolve(fileName));

        backend().fileNotice().generateNotice("Interface source code");

        List<String> parameters = new ArrayList<>();
        parameters.add("__global int * restrict volatile in");
        parameters.add("__global int * restrict volatile read_ptr");
        parameters.add("__global int * restrict volatile write_ptr");
        parameters.add(format("write_only pipe %s __attribute__((blocking)) __attribute__((depth(FIFO_DEPTH))) FIFO_%d", type, id));
        emitter().emit("__kernel void %s(%s){", name, String.join(", ", parameters));
        emitter().emit("");
        emitter().increaseIndentation();

        // the interface code
        emitter().emit("int tmp_read  = *read_ptr;");
        emitter().emit("int tmp_write = *write_ptr;");
        emitter().emit("");
        emitter().emit("int count = (FIFO_DEPTH + tmp_write - tmp_read) %% (FIFO_DEPTH);");
        emitter().emit("");

        emitter().emit("for (int i = 0; i < count; ++i) {");
        emitter().increaseIndentation();
        emitter().emit("%s value = in[(i + tmp_read) %% FIFO_DEPTH];", type);
        emitter().emit("write_pipe(FIFO_%d, &value);", id);
        emitter().decreaseIndentation();
        emitter().emit("}");
        emitter().emit("");

        emitter().emit("if(count){");
        emitter().increaseIndentation();
        emitter().emit("*read_ptr = (tmp_read + count) %% FIFO_DEPTH;");
        emitter().decreaseIndentation();
        emitter().emit("}");
        emitter().emit("");

        emitter().decreaseIndentation();
        emitter().emit("}");

        emitter().close();
    }

    default void generateOutputInterface(Connection connection, Path path) {
        int fifoId = resolver().getConnectionId(connection);

        // kernel name
        String name = "interface_" + fifoId;

        // filename
        String fileName = name + ".cl";

        // the interface type
        Type tokenType = backend().types().declaredPortType(resolver().getSourcePortDecl(connection));
        String type = backend().code().type(tokenType);

        // open file for code generation
        emitter().open(path.resolve(fileName));

        emitter().emit("#include \"global.h\"");
        emitter().emit("");

        List<String> parameters = new ArrayList<>();
        parameters.add("__global int * volatile restrict out");
        parameters.add("__global int * volatile restrict read_ptr");
        parameters.add("__global int * volatile restrict write_ptr");
        parameters.add(format("read_only pipe %s __attribute__((depth(FIFO_DEPTH))) FIFO_%d", type, fifoId));
        emitter().emit("__kernel void %s(%s){", name, String.join(", ", parameters));
        emitter().emit("");
        emitter().increaseIndentation();

        emitter().emit("int tmp_read  = *read_ptr;");
        emitter().emit("int tmp_write = *write_ptr;");
        emitter().emit("int count = 0;");
        emitter().emit("%s value;", type); //FIXME initialize the variable
        emitter().emit("");

        emitter().emit("int rooms = (FIFO_DEPTH + tmp_read - tmp_write - 1) %% FIFO_DEPTH;");
        emitter().emit("");

        emitter().emit("if(rooms) {");
        emitter().emit("");
        emitter().increaseIndentation();
        emitter().emit("while(rooms && (read_pipe(FIFO_%d, &value)>=0)){", fifoId);
        emitter().increaseIndentation();
        emitter().emit("out[(tmp_write + count) %% FIFO_DEPTH] = value;");
        emitter().emit("count++;");
        emitter().emit("rooms--;");
        emitter().decreaseIndentation();
        emitter().emit("}");
        emitter().emit("");
        emitter().emit("if(count){");
        emitter().increaseIndentation();
        emitter().emit("*write_ptr = (tmp_write + count) %% FIFO_DEPTH;");
        emitter().decreaseIndentation();
        emitter().emit("}");
        emitter().emit("");
        emitter().decreaseIndentation();
        emitter().emit("}");
        emitter().emit("");

        emitter().decreaseIndentation();
        emitter().emit("}");
        emitter().increaseIndentation();

        emitter().close();
    }


}
