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

/**
 * @author Simone Casale-Brunet
 */
@Module
public interface Host {

    @Binding
    AoclBackendCore backend();

    default ModelHelper helper() {
        return backend().helper().get();
    }

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

    default void generateMakeFile(Path path) {
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

        // copy the utils library helper header
        emitter().open(includePath.resolve(includePath.resolve("utils.h")));
        try (InputStream in = ClassLoader.getSystemResourceAsStream("aocl_backend_code/utils.h")) {
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
        emitter().emit("#include \"utils.h\"");
        emitter().emit("#include <stdio.h>");
        emitter().emit("#include <string.h>");
        emitter().emit("#include <pthread.h>");
        emitter().emit("#include <chrono>");
        emitter().emit("");
        // constants
        emitter().emit("#define PLATFORM_NAME \"Intel(R) FPGA\"");
        emitter().emit("#define BINARY_NAME \"device.aocx\"");
        emitter().emit("#define QUEUES_SIZE %d", kernelsIds.keySet().size());
        emitter().emit("#define PIPES_SIZE 512");
        emitter().emit("#define THREADS_SIZE %d", helper().getBorders().size());

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

        // interfaces buffers
        emitter().emit("pthread_t threads[THREADS_SIZE];");
        emitter().emit("");

        // timers
        emitter().emit("using Clock = std::chrono::high_resolution_clock;");
        emitter().emit("using Ms = std::chrono::milliseconds;");
        emitter().emit("template<class Duration>");
        emitter().emit("using TimePoint = std::chrono::time_point<Clock, Duration>;");
        emitter().emit("");

        for(Connection connection : helper().getBorders()){
            int fifoId = helper().getConnectionId(connection);
            emitter().emit("int *interface_%d_buffer;", fifoId); //TODO add TYPE
            emitter().emit("int *interface_%d_read;", fifoId);
            emitter().emit("int *interface_%d_write;", fifoId);
            emitter().emit("");
        }

        emitter().emit("void cleanup();");
        emitter().emit("");

        // input interface threads
        for (Connection input : helper().getInputs()) {
            int fifoId = helper().getConnectionId(input);
            emitter().emit("// input interface thread for FIFO %d", fifoId);
            emitter().emit("void *ft_interface_%d(void *) {", fifoId);
            emitter().increaseIndentation();
            emitter().emit("FILE *fp;");
            emitter().emit("fp = fopen(\"%s.txt\", \"r\");", input.getTarget().getPort());
            emitter().emit("char * line;");

            emitter().emit("size_t len = 0;");
            emitter().emit("ssize_t read = 0;");
            emitter().emit("int read_status = 0;");

            emitter().emit("*interface_%d_read = 0;", fifoId);
            emitter().emit("*interface_%d_write = 0;", fifoId);

            emitter().emit("if (fp != NULL) {");
            emitter().increaseIndentation();
            emitter().emit("while (read != -1) {");
            emitter().increaseIndentation();
            emitter().emit("int parsedTokens = 0;");
            emitter().emit("int rooms = (PIPES_SIZE + *interface_%d_read - *interface_%d_write - 1) %% PIPES_SIZE;", fifoId, fifoId);
            emitter().emit("while (rooms && (read = getline(&line, &len, fp)) != -1) {");
            emitter().increaseIndentation();
            emitter().emit("int value = parse_int(line, &read_status);");
            emitter().emit("if (read_status) {");
            emitter().increaseIndentation();
            emitter().emit("interface_%d_buffer[(*interface_%d_write + parsedTokens) %% PIPES_SIZE] = value;", fifoId, fifoId);
            emitter().emit("parsedTokens++;");
            emitter().emit("rooms--;");
            emitter().decreaseIndentation();
            emitter().emit("} else {");
            emitter().increaseIndentation();
            emitter().emit("read = -1;");
            emitter().emit("break;");
            emitter().decreaseIndentation();
            emitter().emit("}");
            emitter().decreaseIndentation();
            emitter().emit("}");
            emitter().emit("if (parsedTokens) {");
            emitter().increaseIndentation();
            emitter().emit("*interface_%d_write += parsedTokens;", fifoId);
            emitter().emit("cl_int status = clEnqueueTask(queues[QUEUE_INTERFACE_%d], kernel_interface_%d, 0, NULL, NULL);", fifoId, fifoId);
            emitter().emit("test_error(status, \"ERROR: Failed to launch interface %d kernel.\\n\", &cleanup);", fifoId);
            emitter().emit("status = clFinish(queues[QUEUE_INTERFACE_%d]);", fifoId);
            emitter().decreaseIndentation();
            emitter().emit("}else{");
            emitter().increaseIndentation();
            emitter().emit("pthread_yield();");
            emitter().decreaseIndentation();
            emitter().emit("}");
            emitter().decreaseIndentation();
            emitter().emit("}");
            emitter().decreaseIndentation();
            emitter().emit("} else {");
            emitter().increaseIndentation();
            emitter().emit("printf(\"ERROR: unable to read file %s.txt\\n\");", input.getTarget().getPort());
            emitter().decreaseIndentation();
            emitter().emit("}");

            emitter().emit("return NULL;");
        emitter().decreaseIndentation();
        emitter().emit("}");
        }
        emitter().emit("");

        // output interface threads
        for (Connection output : helper().getOutputs()) {
            int fifoId = helper().getConnectionId(output);
            emitter().emit("// input interface thread for FIFO %d", fifoId);
            emitter().emit("");
            emitter().emit("void *ft_interface_%d(void *) {", fifoId);
            emitter().increaseIndentation();
            emitter().emit("*interface_%d_read = 0;", fifoId);
            emitter().emit("*interface_%d_write = 0;", fifoId);

            emitter().emit("Clock::time_point _start = Clock::now();");
            emitter().emit("bool stop = false;");
            emitter().emit("long count = 0;");

            emitter().emit("while (!stop) {");
            emitter().increaseIndentation();
            emitter().emit("count++;");
            emitter().emit("cl_int status = clEnqueueTask(queues[QUEUE_INTERFACE_%d], kernel_interface_%d, 0, NULL, NULL);", fifoId, fifoId);
            emitter().emit("test_error(status, \"ERROR: Failed to launch  interface fifo %d.\\n\", &cleanup);", fifoId);
            emitter().emit("status = clFinish(queues[QUEUE_INTERFACE_%d]);", fifoId);

            emitter().emit("int count = (PIPES_SIZE + *interface_%d_write - *interface_%d_read ) %% PIPES_SIZE;", fifoId, fifoId);
            emitter().emit("if(count){");
            emitter().increaseIndentation();
                    // consume values
            emitter().emit("//for(int i = 0; i < count; i++){");
            emitter().emit("//    printf(\"%%d \", interface_%d_buffer[(*interface_%d_read + i) %% PIPES_SIZE]);", fifoId, fifoId);
            emitter().emit("//}");
            emitter().emit("*interface_%d_read = (*interface_%d_read + count) %% PIPES_SIZE;", fifoId, fifoId);
            emitter().emit("_start = Clock::now();");
            emitter().decreaseIndentation();
            emitter().emit("}else{");
            emitter().increaseIndentation();
            emitter().emit("stop = TimePoint<Ms>(std::chrono::duration_cast < Ms > (Clock::now() - _start)) > TimePoint<Ms>(Ms(3000));");
            emitter().emit("pthread_yield();");
            emitter().decreaseIndentation();
            emitter().emit("}");
            emitter().decreaseIndentation();
            emitter().emit("}");

            emitter().emit("return NULL;");

            emitter().decreaseIndentation();
            emitter().emit("}");
            emitter().emit("");
        }
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

        // for each instance and in/out interface create a kernel
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

        emitter().emit("// Set the kernel arguments");
        for (Instance instance : network.getInstances()) {
            int i = 0;
            String kernel_name = kernelsNames.get(instance);
            for (Pair<PortDecl, Connection> incoming : helper().getIncomings(instance.getInstanceName())) {
                PortDecl port = incoming.v1;
                Connection connection = incoming.v2;
                int fifoId = helper().getConnectionId(connection);
                emitter().emit("// instance: %s, in-port: %s", instance.getInstanceName(), port.getName());
                emitter().emit("status = clSetKernelArg(kernel_%s, %d, sizeof(cl_mem), &pipe_%d);", kernel_name, i, fifoId);
                i++;
            }
            for (Pair<PortDecl, Connection> outgoing : helper().getOutgoings(instance.getInstanceName())) {
                PortDecl port = outgoing.v1;
                Connection connection = outgoing.v2;
                int fifoId = helper().getConnectionId(connection);
                emitter().emit("// instance: %s, out-port: %s", instance.getInstanceName(), port.getName());
                emitter().emit("status = clSetKernelArg(kernel_%s, %d, sizeof(cl_mem), &pipe_%d);", kernel_name, i, fifoId);
                i++;
            }
        }
        emitter().emit("");


        // create the buffers for the interfaces
        emitter().emit("// create the interface buffers");
        for (Connection connection : helper().getBorders()) {
            int fifoId = helper().getConnectionId(connection);
            emitter().emit("interface_%d_buffer = (int *) aligned_malloc(sizeof(int) * PIPES_SIZE);", fifoId); //TODO add fifo TYPE
            emitter().emit("cl_mem mem_interface_%d_buffer = clCreateBuffer(context, CL_MEM_USE_HOST_PTR, sizeof(int) * PIPES_SIZE, interface_%d_buffer, &status);", fifoId, fifoId);
            emitter().emit("interface_%d_read   = (int *) aligned_malloc(sizeof(int));", fifoId);
            emitter().emit("cl_mem mem_interface_%d_read = clCreateBuffer(context, CL_MEM_USE_HOST_PTR, sizeof(int), interface_%d_read, &status);", fifoId, fifoId);
            emitter().emit("interface_%d_write  = (int *) aligned_malloc(sizeof(int));", fifoId);
            emitter().emit("cl_mem mem_interface_%d_write = clCreateBuffer(context, CL_MEM_USE_HOST_PTR, sizeof(int), interface_%d_write, &status);", fifoId, fifoId);
        }
        emitter().emit("");

        emitter().emit("// link buffers and interface kernels");
        for (Connection connection : helper().getBorders()) {
            // Set the kernel arguments
            int fifoId = helper().getConnectionId(connection);
            String kernelName = kernelsNames.get(connection);
            emitter().emit("status = clSetKernelArg(kernel_%s, 0, sizeof(cl_mem), &mem_interface_%d_buffer);", kernelName, fifoId);
            emitter().emit("test_error(status, \"ERROR: Failed to set kernel %s arg 0.\\n\", &cleanup);", kernelName);
            emitter().emit("status = clSetKernelArg(kernel_%s, 1, sizeof(cl_mem), &mem_interface_%d_read);", kernelName, fifoId);
            emitter().emit("test_error(status, \"ERROR: Failed to set kernel %s arg 1.\\n\", &cleanup);", kernelName);
            emitter().emit("status = clSetKernelArg(kernel_%s, 2, sizeof(cl_mem), &mem_interface_%d_write);", kernelName, fifoId);
            emitter().emit("test_error(status, \"ERROR: Failed to set kernel %s arg 2.\\n\", &cleanup);", kernelName);
            emitter().emit("status = clSetKernelArg(kernel_%s, 3, sizeof(cl_mem), &pipe_%d);", kernelName, fifoId);
            emitter().emit("test_error(status, \"ERROR: Failed to set kernel %s arg 3.\\n\", &cleanup);", kernelName);
        }
        emitter().emit("");

        emitter().emit("printf(\"\\nKernels initialization is complete.\\n\");");
        emitter().emit("printf(\"Launching the kernels...\\n\");");
        emitter().emit("");

        // a task for each instance
        for (Instance instance : network.getInstances()) {
            emitter().emit("status = clEnqueueTask(queues[QUEUE_%s], kernel_%s, 0, NULL, NULL);", kernelsNames.get(instance).toUpperCase(), kernelsNames.get(instance));
            emitter().emit("test_error(status, \"ERROR: Failed to launch kernel %s\", &cleanup);", kernelsNames.get(instance));
        }
        emitter().emit("");

        // the kernel interfaces
        int threadId = 0;
        for (Connection connection : helper().getBorders()) {
            int fifoId = helper().getConnectionId(connection);
            emitter().emit("pthread_create(&threads[%d], NULL, ft_interface_%d, NULL);", threadId, fifoId);
            threadId++;
        }
        emitter().emit("");

        emitter().emit("for(int i = 0; i < THREADS_SIZE; i++){");
        emitter().increaseIndentation();
        emitter().emit("pthread_join(threads[i], NULL);");
        emitter().decreaseIndentation();
        emitter().emit("}");
        emitter().emit("");


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

        for (String kernel : kernelsNames.values()) {
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
        for (Connection input : helper().getInputs()) {
            map.put(input, index++);
        }
        for (Connection output : helper().getOutputs()) {
            map.put(output, index++);
        }

        return map;
    }

    default Map<Object, String> createKernelsNameMap(Network network) {
        Map<Object, String> map = new HashMap<>();
        network.getInstances().forEach(i -> map.put(i, i.getInstanceName()));
        for (Connection input : helper().getInputs()) {
            int fifoId = helper().getConnectionId(input);
            map.put(input, "interface_" + fifoId);
        }
        for (Connection output : helper().getOutputs()) {
            int fifoId = helper().getConnectionId(output);
            map.put(output, "interface_" + fifoId);
        }
        return map;
    }

}
