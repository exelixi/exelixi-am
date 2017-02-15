/*
 * EXELIXI
 *
 * Copyright (C) 2017 EPFL SCI-STI-MM
 *
 * This file is part of EXELIXI.
 *
 * EXELIXI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * EXELIXI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with EXELIXI. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7
 *
 * If you modify this Program, or any covered work, by linking or combining it
 * with Eclipse (or a modified version of Eclipse or an Eclipse plugin or
 * an Eclipse library), containing parts covered by the terms of the
 * Eclipse Public License (EPL), the licensors of this Program grant you
 * additional permission to convey the resulting work.  Corresponding Source
 * for a non-source form of such a combination shall include the source code
 * for the parts of Eclipse libraries used as well as that of the covered work.
 *
 */
package xyz.exelixi.backend.opencl.aocl.codegen;

import org.multij.Binding;
import org.multij.Module;
import se.lth.cs.tycho.ir.network.Connection;
import se.lth.cs.tycho.phases.cbackend.Emitter;
import se.lth.cs.tycho.settings.Configuration;
import se.lth.cs.tycho.types.Type;
import xyz.exelixi.backend.opencl.aocl.AoclBackendCore;
import xyz.exelixi.utils.Resolver;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static org.multij.BindingKind.MODULE;
import static xyz.exelixi.backend.opencl.aocl.phases.AoclBackendPhase.usePipes;

/**
 * @author Simone Casale-Brunet
 */
@Module
public interface Interfaces {

    @Binding(MODULE)
    AoclBackendCore backend();

    default Resolver resolver() {
        return backend().resolver().get();
    }

    default Emitter emitter() {
        return backend().emitter();
    }

    default Configuration configuration() {
        return backend().context().getConfiguration();
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

        // create the parameters
        List<String> parameters = new ArrayList<>();
        parameters.add("__global int * restrict volatile in");
        parameters.add("__global int * restrict volatile read_ptr");
        parameters.add("__global int * restrict volatile write_ptr");
        if (configuration().get(usePipes).booleanValue()) {
            parameters.add(format("write_only pipe %s __attribute__((blocking)) __attribute__((depth(FIFO_DEPTH))) FIFO_%d", type, id));
        }

        // kernel definition with attributes
        emitter().emit("__attribute__((max_global_work_dim(0)))");
        emitter().emit("__attribute__((reqd_work_group_size(1,1,1)))");
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
        if (configuration().get(usePipes).booleanValue()) {
            emitter().emit("write_pipe(FIFO_%d, &value);", id);
        } else {
            emitter().emit("write_channel_altera(FIFO_%d, value);", id);
        }
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
        if (configuration().get(usePipes).booleanValue()) {
            parameters.add(format("read_only pipe %s __attribute__((depth(FIFO_DEPTH))) FIFO_%d", type, fifoId));
        }
        emitter().emit("__kernel void %s(%s){", name, String.join(", ", parameters));
        emitter().emit("");
        emitter().increaseIndentation();

        emitter().emit("int tmp_read  = *read_ptr;");
        emitter().emit("int tmp_write = *write_ptr;");
        emitter().emit("int count = 0;");
        emitter().emit("%s value = %s;", type, backend().defaultValues().defaultValue(tokenType));
        emitter().emit("");

        emitter().emit("int rooms = (FIFO_DEPTH + tmp_read - tmp_write - 1) %% FIFO_DEPTH;");
        emitter().emit("");

        if (configuration().get(usePipes).booleanValue()) {
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
        }else{
            emitter().emit("while(rooms) {");
            emitter().increaseIndentation();

            emitter().emit("bool valid = false;");
            emitter().emit("value = read_channel_nb_altera(FIFO_%d, &valid);", fifoId);

            emitter().emit("if(valid){");
            emitter().increaseIndentation();
            emitter().emit("out[(tmp_write + count) %% FIFO_DEPTH] = value;");
            emitter().emit("count++;");
            emitter().emit("rooms--;");
            emitter().decreaseIndentation();
            emitter().emit("}else{");
            emitter().increaseIndentation();
            emitter().emit("rooms = 0;");
            emitter().decreaseIndentation();
            emitter().emit("}");

            emitter().decreaseIndentation();
            emitter().emit("}");

            emitter().emit("");

            emitter().emit("if(count){");
            emitter().increaseIndentation();
            emitter().emit("*write_ptr = (tmp_write + count) %% FIFO_DEPTH;");
            emitter().decreaseIndentation();
            emitter().emit("}");
        }

        emitter().decreaseIndentation();
        emitter().emit("}");
        emitter().increaseIndentation();

        emitter().close();
    }


}
