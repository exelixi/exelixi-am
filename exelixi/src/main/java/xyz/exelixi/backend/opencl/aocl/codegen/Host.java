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
import se.lth.cs.tycho.ir.Port;
import se.lth.cs.tycho.ir.entity.PortDecl;
import se.lth.cs.tycho.ir.network.Connection;
import se.lth.cs.tycho.ir.network.Instance;
import se.lth.cs.tycho.ir.network.Network;
import se.lth.cs.tycho.phases.attributes.Types;
import se.lth.cs.tycho.phases.cbackend.Emitter;
import se.lth.cs.tycho.reporting.CompilationException;
import xyz.exelixi.backend.opencl.aocl.AoclBackendCore;
import xyz.exelixi.utils.ModelHelper;
import xyz.exelixi.utils.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Simone Casale-Brunet
 */
@Module
public interface Host {

    @Binding
    AoclBackendCore backend();

    default ModelHelper helper() { return backend().helper().get(); }

    default Emitter emitter() {
        return backend().emitter();
    }

    default Structure structure() {
        return backend().structure();
    }

    default Types types() {
        return backend().types();
    }

    default Code code() {
        return backend().code();
    }

    default void generateMakeFile(Path path){
        emitter().open(path.resolve(path.resolve("Makefile")));
        try (InputStream in = ClassLoader.getSystemResourceAsStream("aocl_backend_code/Makefile")) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            reader.lines().forEach(emitter()::emitRawLine);
        } catch (IOException e) {
            throw CompilationException.from(e);
        }
        emitter().close();
    }

    default void generateLibrary(Path sourcePath, Path includePath) {
        // copy the AOCL library helper source code
        emitter().open(sourcePath.resolve(sourcePath.resolve("AOCL.cpp")));
        try (InputStream in = ClassLoader.getSystemResourceAsStream("aocl_backend_code/AOCL.cpp")) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            reader.lines().forEach(emitter()::emitRawLine);
        } catch (IOException e) {
            throw CompilationException.from(e);
        }
        emitter().close();

        // copy the AOCL library helper header
        emitter().open(includePath.resolve(includePath.resolve("AOCL.h")));
        try (InputStream in = ClassLoader.getSystemResourceAsStream("aocl_backend_code/AOCL.h")) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            reader.lines().forEach(emitter()::emitRawLine);
        } catch (IOException e) {
            throw CompilationException.from(e);
        }
        emitter().close();

    }

    default void generateSourceCode(Path path) {
        Network network = backend().task().getNetwork();

        Map<Object, Integer> kernelsIds = createKernelsIdMap(network);
        Map<Object, String> kernelsNames = createKernelsNameMap(network);

        // create the main
        emitter().open(path.resolve(path.resolve("main.cpp")));
        backend().fileNotice().generateNotice("Host source code");
        // includes
        emitter().emit("#include \"AOCL.h\"");
        emitter().emit("#include <stdio.h>");
        emitter().emit("#include <string.h>");
        emitter().emit("");
        // constants
        emitter().emit("#define PLATFORM_NAME \"Intel(R) FPGA\"");
        emitter().emit("#define BINARY_NAME \"device.aocx\"");
        emitter().emit("#define QUEUES_SIZE %d", kernelsIds.keySet().size());
        emitter().emit("#define PIPES_SIZE 512");

        emitter().emit("");
        kernelsNames.entrySet().stream().forEach(k -> emitter().emit("#define QUEUE_%s %d", k.getValue().toUpperCase(), kernelsIds.get(k.getKey())));
        emitter().emit("");

        // parameters and kernels
        emitter().emit("cl_int status;");
        emitter().emit("cl_context context = NULL;");
        emitter().emit("cl_command_queue queues[QUEUES_SIZE];");
        emitter().emit("cl_program program = NULL;");
        kernelsNames.values().forEach(k -> emitter().emit("cl_kernel kernel_%s = NULL;", k));
        emitter().emit("");

        emitter().emit("void cleanup();");
        emitter().emit("");

        emitter().emit("int main() {");//FIXME add options
        emitter().increaseIndentation();
        emitter().emit("set_cwd_to_execdir();");
        emitter().emit("");

        emitter().emit("// find the platform defined by the PLATFORM_NAME variable");
        emitter().emit("cl_platform_id platform = find_platform(PLATFORM_NAME, &status);");
        emitter().emit("test_error(status, \"ERROR: Unable to find the OpenCL platform.\\n\", &cleanup);");
        emitter().emit("");

        emitter().emit("// get all the available devives");
        emitter().emit("cl_uint num_devices;");
        emitter().emit("cl_device_id* devices = get_devices(platform, CL_DEVICE_TYPE_ALL, &num_devices, &status);");
        emitter().emit("test_error(status, \"ERROR: Unable to find any device.\\n\", &cleanup);");
        emitter().emit("");

        emitter().emit("// take the first device");
        emitter().emit("cl_device_id device = devices[0];");
        emitter().emit("");

        emitter().emit("// create a context");
        emitter().emit("context = clCreateContext(NULL, 1, &device, &ocl_context_callback_message, NULL, &status);");
        emitter().emit("test_error(status, \"ERROR: Failed to open the context.\\n\", &cleanup);");
        emitter().emit("");

        emitter().emit("// Create the command queues.");
        emitter().emit("for(int i = 0; i < QUEUES_SIZE; ++i){");
        emitter().increaseIndentation();
        emitter().emit("queues[i] = clCreateCommandQueue(context, device, CL_QUEUE_PROFILING_ENABLE, &status);");
        emitter().emit("test_error(status, \"ERROR: Failed to create command queue.\\n\", &cleanup);");
        emitter().decreaseIndentation();
        emitter().emit("}");
        emitter().emit("");

        emitter().emit("// create the program");
        emitter().emit("program = create_program_from_binary(context, BINARY_NAME, &device, 1, &status);");
        emitter().emit("test_error(status, \"ERROR: Failed to create the program.\\n\", &cleanup);");
        emitter().emit("");

        emitter().emit("// create the program");
        emitter().emit("status = clBuildProgram(program, 1, &device, \"\", NULL, NULL);");
        emitter().emit("test_error(status, \"ERROR: Failed to create the program.\\n\", &cleanup);");
        emitter().emit("");

        emitter().emit("// create the kernels");
        for (String kernel : kernelsNames.values()) {
            emitter().emit("kernel_%s = clCreateKernel(program, \"%s\", &status);", kernel, kernel);
            emitter().emit("test_error(status, \"ERROR: Failed to create the kernel %s.\\n\", &cleanup);", kernel);
        }
        emitter().emit("");

        emitter().emit("// create the pipe"); // FIXME add pipe TYPE
        for (Connection connection : network.getConnections()) {
            int fifoId = helper().getConnectionId(connection);
            emitter().emit("cl_mem pipe_%d = clCreatePipe(context, 0, sizeof(cl_int), PIPES_SIZE, NULL, &status); ", fifoId);
            emitter().emit("test_error(status, \"ERROR: Failed to create the pipe %d.\\n\", &cleanup);", fifoId);
        }
        emitter().emit("");

        //TODO fictitious interface buffers

        emitter().emit("// Set the kernel arguments");
        for (Instance instance : network.getInstances()){
            int i = 0;
            String kernel_name = kernelsNames.get(instance);
            for(Pair<PortDecl, Connection> incoming : helper ().getIncomings(instance.getInstanceName())){
                PortDecl port = incoming.v1;
                Connection connection = incoming.v2;
                int fifoId = helper().getConnectionId(connection);
                emitter().emit("// instance: %s, in-port: %s", instance.getInstanceName(), port.getName());
                emitter().emit("status = clSetKernelArg(kernel_%s, %d, sizeof(cl_mem), &pipe_%d);", kernel_name, i, fifoId);
                i++;
            }
            for(Pair<PortDecl, Connection> outgoing : helper().getOutgoings(instance.getInstanceName())){
                PortDecl port = outgoing.v1;
                Connection connection = outgoing.v2;
                int fifoId = helper().getConnectionId(connection);
                emitter().emit("// instance: %s, out-port: %s", instance.getInstanceName(), port.getName());
                emitter().emit("status = clSetKernelArg(kernel_%s, %d, sizeof(cl_mem), &pipe_%d);", kernel_name, i, fifoId);
                i++;
            }
        }
        emitter().emit("");

        //TODO pipes for fictitious kernel interfaces


        emitter().emit("printf(\"\\nKernels initialization is complete.\\n\");");
        emitter().emit("printf(\"Launching the kernels...\\n\");");
        emitter().emit("");

        emitter().emit("cl_event sync;");
        emitter().emit("");

        // a task for each instance
        for (Instance instance : network.getInstances()) {
            emitter().emit("status = clEnqueueTask(queues[QUEUE_%s], kernel_%s, 0, NULL, NULL);", kernelsNames.get(instance).toUpperCase(), kernelsNames.get(instance));
            emitter().emit("test_error(status, \"ERROR: Failed to launch kernel %s\", &cleanup);", kernelsNames.get(instance));
        }
        emitter().emit("");

        // the input interfaces


        // the output interfaces


        // end here
        emitter().emit("cleanup();");
        emitter().emit("");
        emitter().emit("return 0;");

        emitter().decreaseIndentation();
        emitter().emit("}");
        emitter().emit("");

        // cleanup function
        emitter().emit("void cleanup() {");
        emitter().increaseIndentation();
        emitter().emit("");

        for(String kernel : kernelsNames.values()) {
            emitter().emit("if(kernel_%s)", kernel);
            emitter().emit("{");
            emitter().increaseIndentation();
            emitter().emit("clReleaseKernel(kernel_%s);", kernel);
            emitter().decreaseIndentation();
            emitter().emit("}");
            emitter().emit("");
        }

        emitter().emit("if(program){");
        emitter().increaseIndentation();
        emitter().emit("clReleaseProgram(program);");
        emitter().decreaseIndentation();
        emitter().emit("}");
        emitter().emit("");

        emitter().emit("for (int i = 0; i < QUEUES_SIZE; ++i) {");
        emitter().increaseIndentation();
        emitter().emit("cl_command_queue queue = queues[i];");
        emitter().emit("if(queue){");
        emitter().increaseIndentation();
        emitter().emit("clReleaseCommandQueue(queue);");
        emitter().decreaseIndentation();
        emitter().emit("}");
        emitter().decreaseIndentation();
        emitter().emit("}");
        emitter().emit("");

        emitter().emit("if (context) {");
        emitter().increaseIndentation();
        emitter().emit("clReleaseContext(context);");
        emitter().decreaseIndentation();
        emitter().emit("}");
        emitter().emit("");

        emitter().decreaseIndentation();
        emitter().emit("}");


        emitter().close();
    }

    //FIXME move in a transformation or in a utility class?
    default Map<Connection, Integer> createConnectionsIdMap(Network network) {
        Map<Connection, Integer> map = new HashMap<>();
        int id = 0;
        for (Connection connection : network.getConnections()) {
            map.put(connection, ++id);
        }
        return map;
    }

    default Map<Object, Integer> createKernelsIdMap(Network network) {
        Map<Object, Integer> map = new HashMap<>();
        int index = 0;
        for (Instance instance : network.getInstances()) {
            map.put(instance, index++);
        }
        for (PortDecl port : network.getInputPorts()) {
            map.put(port, index++);
        }
        for (PortDecl port : network.getOutputPorts()) {
            map.put(port, index++);
        }

        return map;
    }

    default Map<Object, String> createKernelsNameMap(Network network) {
        Map<Object, String> map = new HashMap<>();
        network.getInstances().forEach(i -> map.put(i, i.getInstanceName()));
        network.getInputPorts().forEach(p -> map.put(p, p.getName()));
        network.getOutputPorts().forEach(p -> map.put(p, p.getName()));
        return map;
    }

}
