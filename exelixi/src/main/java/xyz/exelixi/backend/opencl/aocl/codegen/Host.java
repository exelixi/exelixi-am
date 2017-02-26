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
import se.lth.cs.tycho.settings.Configuration;
import se.lth.cs.tycho.types.Type;
import xyz.exelixi.backend.opencl.aocl.AoclBackendCore;
import xyz.exelixi.utils.Resolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;

import static xyz.exelixi.backend.opencl.aocl.phases.AoclBackendPhase.usePipes;
import static xyz.exelixi.backend.opencl.aocl.phases.AoclBackendPhase.profile;
import static xyz.exelixi.backend.opencl.aocl.phases.AoclBackendPhase.*;
import static xyz.exelixi.utils.Utils.union;

/**
 * Source code generation for the host
 *
 * @author Simone Casale-Brunet
 */
@Module
public interface Host {

    @Binding
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

    default Types types() {
        return backend().types();
    }

    default Code code() {
        return backend().code();
    }

    /**
     * Generate the host program
     *
     * @param sourcePath the path where the main.cpp file will be created
     */
    default void generateHost(Path sourcePath) {
        Network network = backend().task().getNetwork();

        // of the main
        emitter().open(sourcePath.resolve(sourcePath.resolve("main.cpp")));
        backend().fileNotice().generateNotice("Host source code");

        /* ================== CONSTANTS ==================*/
        // includes
        emitter().emit("#include \"AOCL.h\"");
        emitter().emit("#include \"utils.h\"");
        emitter().emit("#include <stdio.h>");
        emitter().emit("#include <string.h>");
        emitter().emit("#include \"sharedconstants.h\"");
        emitter().emit("");

        // timeout
        emitter().emit("#define TIMEOUT_SEC %f", configuration().get(timeOut).doubleValue());
        emitter().emit("");

        // the binary name
        emitter().emit("#define BINARY_NAME \"device.aocx\"");
        emitter().emit("");

        // the total number of queues used for the actors and the interfaces
        int queuesSize = network.getInstances().size() + resolver().getIncomings().size() + resolver().getOutgoings().size();
        emitter().emit("#define QUEUES_SIZE %d", queuesSize);

        // the queue identifiers
        int queue = 0;
        for (Instance instance : network.getInstances()) {
            emitter().emit("#define QUEUE_ACTOR_%s %d", instance.getInstanceName(), queue);
            queue++;
        }
        for (Connection connection : union(resolver().getIncomings(), resolver().getOutgoings())) {
            int id = resolver().getConnectionId(connection);
            emitter().emit("#define QUEUE_INTERFACE_%d %d", id, queue);
            queue++;
        }
        emitter().emit("");

        /* ================== HOST VARIABLES ==================*/
        // the host variables
        emitter().emit("cl_int status;");
        emitter().emit("cl_context context = NULL;");
        emitter().emit("cl_command_queue queues[QUEUES_SIZE];");
        emitter().emit("cl_program program = NULL;");
        network.getInstances().forEach(i -> emitter().emit("cl_kernel kernel_actor_%s = NULL;", i.getInstanceName()));
        union(resolver().getIncomings(), resolver().getOutgoings()).forEach(c -> {
            int id = resolver().getConnectionId(c);
            emitter().emit("cl_kernel kernel_interface_%d = NULL;", id);
        });
        emitter().emit("");

        /* ================== EVENTS VARIABLES ==================*/
        if (configuration().get(profile).booleanValue()) {
            network.getInstances().forEach(instance -> {
                emitter().emit("cl_event event_%s;", instance.getInstanceName());
            });
            emitter().emit("");
        }

        // the interfaces values
        union(resolver().getIncomings(), resolver().getOutgoings()).forEach(connection -> {
                    int id = resolver().getConnectionId(connection);
                    // ge the type
                    PortDecl portDecl = connection.getTarget().getInstance().isPresent() ? resolver().getTargetPortDecl(connection) : resolver().getSourcePortDecl(connection);
                    Type tokenType = backend().types().declaredPortType(portDecl);
                    String type = backend().code().type(tokenType);
                    // now create the buffer and the countes
                    emitter().emit("volatile %s *interface_%d_buffer;", type, id);
                    emitter().emit("volatile int *interface_%d_read;", id);
                    emitter().emit("volatile int *interface_%d_write;", id);
                }
        );
        emitter().emit("");

        // functions declaration
        emitter().emit("void cleanup();");
        emitter().emit("void schedule_host();");
        emitter().emit("");

        /* ================== MAIN FUNCTION ==================*/
        emitter().emit("int main(){");
        emitter().increaseIndentation();
        // change working directory
        emitter().emit("set_cwd_to_execdir();");
        emitter().emit("");

        // platform selection (here the 1st by default)
        emitter().emit("cl_uint num_platforms;");
        emitter().emit("cl_platform_id* platforms = get_platforms(&num_platforms, &status);");
        emitter().emit("test_error(status, \"ERROR: Unable to find any platform.\\n\", &cleanup);");
        emitter().emit("if (!num_platforms) {");
        emitter().increaseIndentation();
        emitter().emit("printf(\"No platforms found....\\n\");");
        emitter().emit("return 1;");
        emitter().decreaseIndentation();
        emitter().emit("}");
        emitter().emit("");
        emitter().emit("// take the first platform");
        emitter().emit("cl_platform_id platform = platforms[0];");
        emitter().emit("test_error(status, \"ERROR: Unable to find the OpenCL platform.\\n\", &cleanup);");
        emitter().emit("printf(\"Using platform %%s\\n\", get_platform_name(platform));");
        emitter().emit("");

        // device selection (here the 1st by default)
        emitter().emit("// get all the available devives");
        emitter().emit("cl_uint num_devices;");
        emitter().emit("cl_device_id* devices = get_devices(platform, CL_DEVICE_TYPE_ALL, &num_devices, &status);");
        emitter().emit("test_error(status, \"ERROR: Unable to find any device.\\n\", &cleanup);");
        emitter().emit("");
        emitter().emit("// take the first device");
        emitter().emit("cl_device_id device = devices[0];");
        emitter().emit("");

        // create the context
        emitter().emit("// create a context");
        emitter().emit("context = clCreateContext(NULL, 1, &device, &ocl_context_callback_message, NULL, &status);");
        emitter().emit("test_error(status, \"ERROR: Failed to open the context.\\n\", &cleanup);");
        emitter().emit("");

        // Create the command queues
        emitter().emit("// Create the command queues.");
        emitter().emit("for (int i = 0; i < QUEUES_SIZE; ++i) {");
        emitter().increaseIndentation();
        emitter().emit("queues[i] = clCreateCommandQueue(context, device, CL_QUEUE_PROFILING_ENABLE, &status);");
        emitter().emit("test_error(status, \"ERROR: Failed to create command queue.\\n\", &cleanup);");
        emitter().decreaseIndentation();
        emitter().emit("}");
        emitter().emit("");

        // create and build the program
        emitter().emit("// create and build the program");
        emitter().emit("program = create_program_from_binary(context, BINARY_NAME, &device, 1, &status);");
        emitter().emit("test_error(status, \"ERROR: Failed to create the program.\\n\", &cleanup);");
        emitter().emit("status = clBuildProgram(program, 1, &device, \"\", NULL, NULL);");
        emitter().emit("test_error(status, \"ERROR: Failed to create the program.\\n\", &cleanup);");
        emitter().emit("");

        // create the kernels
        emitter().emit("// create the kernels");
        network.getInstances().forEach(i -> {
            String name = i.getInstanceName();
            emitter().emit("kernel_actor_%s = clCreateKernel(program, \"%s\", &status);", name, name);
            emitter().emit("test_error(status, \"ERROR: Failed to create the kernel for actor %s.\\n\", &cleanup);", name);
        });
        union(resolver().getIncomings(), resolver().getOutgoings()).forEach(c -> {
                    int id = resolver().getConnectionId(c);
                    emitter().emit("kernel_interface_%d = clCreateKernel(program, \"interface_%d\", &status);", id, id);
                    emitter().emit("test_error(status, \"ERROR: Failed to create the kernel for interface %d.\\n\", &cleanup);", id);
                }
        );
        emitter().emit("");

        if (configuration().get(usePipes).booleanValue()) {
            // create the pipes
            emitter().emit("// create the pipes");
            network.getConnections().forEach(connection -> {
                int id = resolver().getConnectionId(connection);
                // get connection type
                PortDecl portDecl = connection.getTarget().getInstance().isPresent() ? resolver().getTargetPortDecl(connection) : resolver().getSourcePortDecl(connection);
                Type tokenType = backend().types().declaredPortType(portDecl);
                String type = backend().code().type(tokenType);
                // now create the pipe
                emitter().emit("cl_mem pipe_%d = clCreatePipe(context, 0, sizeof(%s), FIFO_DEPTH, NULL, &status);", id, type);
                emitter().emit("test_error(status, \"ERROR: Failed to create the pipe %d.\\n\", &cleanup);", id);
            });
            emitter().emit("");

            // set actor kernels arguments
            emitter().emit("// set actor kernels arguments");
            network.getInstances().forEach(instance -> {
                String name = instance.getInstanceName();
                int arg = 0;
                for (Connection connection : resolver().getIncomingsMap(instance.getInstanceName()).keySet()) { // input
                    int id = resolver().getConnectionId(connection);
                    emitter().emit("status = clSetKernelArg(kernel_actor_%s, %d, sizeof(cl_mem), &pipe_%d);", name, arg, id);
                    emitter().emit("test_error(status, \"ERROR: Failed to set argument %d on the kernel actor %s.\\n\", &cleanup);", arg, name);
                    arg++;
                }
                for (Connection connection : resolver().getOutgoingsMap(instance.getInstanceName()).keySet()) { // output
                    int id = resolver().getConnectionId(connection);
                    emitter().emit("status = clSetKernelArg(kernel_actor_%s, %d, sizeof(cl_mem), &pipe_%d);", name, arg, id);
                    emitter().emit("test_error(status, \"ERROR: Failed to set argument %d on the kernel actor %s.\\n\", &cleanup);", arg, name);
                    arg++;
                }
            });
            emitter().emit("");
        }

        // create the interface buffers
        emitter().emit("// create the interface buffers and link to the interface kernel");
        union(resolver().getIncomings(), resolver().getOutgoings()).forEach(connection -> {
                    int id = resolver().getConnectionId(connection);
                    // ge the type
                    PortDecl portDecl = connection.getTarget().getInstance().isPresent() ? resolver().getTargetPortDecl(connection) : resolver().getSourcePortDecl(connection);
                    Type tokenType = backend().types().declaredPortType(portDecl);
                    String type = backend().code().type(tokenType);
                    // now create the buffer and the countes
                    emitter().emit("// - interface %d", id);
                    emitter().emit("// -- create shared memory among host and device");

                    if (configuration().get(intelOpt).booleanValue()) { // see UG-OCL002 (v. 2016-10-31), page 87
                        emitter().emit("cl_mem mem_interface_%d_buffer = clCreateBuffer(context, CL_MEM_ALLOC_HOST_PTR, sizeof(%s) * FIFO_DEPTH, NULL, &status);", id, type);
                        emitter().emit("interface_%d_buffer = (%s *) clEnqueueMapBuffer(queues[QUEUE_INTERFACE_%d], mem_interface_%d_buffer, true, CL_MAP_READ | CL_MAP_WRITE, 0, sizeof(%s) * FIFO_DEPTH, 0, NULL, NULL, &status);", id, type, id, id, type);
                        emitter().emit("cl_mem mem_interface_%d_read = clCreateBuffer(context, CL_MEM_ALLOC_HOST_PTR, sizeof(int), NULL, &status);", id);
                        emitter().emit("interface_%d_read = (int *) clEnqueueMapBuffer(queues[QUEUE_INTERFACE_%d], mem_interface_%d_read, true, CL_MAP_READ | CL_MAP_WRITE, 0, sizeof(int), 0, NULL, NULL, &status);", id, id, id);
                        emitter().emit("cl_mem mem_interface_%d_write  = clCreateBuffer(context, CL_MEM_ALLOC_HOST_PTR, sizeof(int), NULL, &status);", id);
                        emitter().emit("interface_%d_write = (int *) clEnqueueMapBuffer(queues[QUEUE_INTERFACE_%d], mem_interface_%d_write, true, CL_MAP_READ | CL_MAP_WRITE, 0, sizeof(int), 0, NULL, NULL, &status);", id, id, id);
                    } else {
                        emitter().emit("interface_%d_buffer = (%s *) aligned_malloc(sizeof(%s) * FIFO_DEPTH);", id, type, type);
                        emitter().emit("cl_mem mem_interface_%d_buffer = clCreateBuffer(context, CL_MEM_USE_HOST_PTR, sizeof(%s) * FIFO_DEPTH, (void *) interface_%d_buffer, &status);", id, type, id);
                        emitter().emit("interface_%d_read = (int *) aligned_malloc(sizeof(int));", id);
                        emitter().emit("cl_mem mem_interface_%d_read = clCreateBuffer(context, CL_MEM_USE_HOST_PTR, sizeof(int),  (void *) interface_%d_read, &status);", id, id);
                        emitter().emit("interface_%d_write = (int *) aligned_malloc(sizeof(int));", id);
                        emitter().emit("cl_mem mem_interface_%d_write = clCreateBuffer(context, CL_MEM_USE_HOST_PTR, sizeof(int),  (void *) interface_%d_write, &status);", id, id);
                    }

                    emitter().emit("// -- link to the kernel");
                    emitter().emit("status = clSetKernelArg(kernel_interface_%d, 0, sizeof(cl_mem), &mem_interface_%d_buffer);", id, id);
                    emitter().emit("test_error(status, \"ERROR: Failed to set kernel interface_%d arg 0.\\n\", &cleanup);", id);
                    emitter().emit("status = clSetKernelArg(kernel_interface_%d, 1, sizeof(cl_mem), &mem_interface_%d_read);", id, id);
                    emitter().emit("test_error(status, \"ERROR: Failed to set kernel interface_%d arg 1.\\n\", &cleanup);", id);
                    emitter().emit("status = clSetKernelArg(kernel_interface_%d, 2, sizeof(cl_mem), &mem_interface_%d_write);", id, id);
                    emitter().emit("test_error(status, \"ERROR: Failed to set kernel interface_%d arg 2.\\n\", &cleanup);", id);

                    if (configuration().get(usePipes).booleanValue()) {
                        emitter().emit("status = clSetKernelArg(kernel_interface_%d, 3, sizeof(cl_mem), &pipe_%d);", id, id);
                        emitter().emit("test_error(status, \"ERROR: Failed to set kernel interface_%d arg 3.\\n\", &cleanup);", id);
                    }
                }
        );
        emitter().emit("");

        // print notice init ok
        emitter().emit("printf(\"Kernels initialization completed: launching the kernels.\\n\");");

        if (configuration().get(profile).booleanValue()) {
            emitter().emit("printf(\"execution profiling: ENABLED\\n\");");
        }

        emitter().emit("");

        emitter().emit("// launch the actor kernels");
        network.getInstances().forEach(instance -> {
            String name = instance.getInstanceName();
            String event = configuration().get(profile).booleanValue() ? "&event_" + name : "NULL";
            emitter().emit("status = clEnqueueTask(queues[QUEUE_ACTOR_%s], kernel_actor_%s, 0, NULL, %s);", name, name, event);
            emitter().emit("test_error(status, \"ERROR: Failed to launch kernel %s\", &cleanup);", name);
        });
        emitter().emit("");
        emitter().emit("schedule_host();");
        emitter().emit("");
        emitter().emit("cleanup();");
        emitter().emit("");
        emitter().emit("return 0;");

        emitter().decreaseIndentation();
        emitter().emit("}");
        emitter().emit("");

        // call the schedule and clean functions
        scheduleFunction();
        cleanupFunction();

        // finally, close the host generated file
        emitter().close();

    }

    /**
     * Copy the makefile to compile the host application
     *
     * @param path the path where the makefile will be copied
     */
    default void copyMakeFile(Path path) {
        emitter().open(path.resolve(path.resolve("Makefile")));
        try (InputStream in = ClassLoader.getSystemResourceAsStream("aocl_backend_code/Makefile")) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            reader.lines().forEach(emitter()::emitRawLine);
        } catch (IOException e) {
            throw CompilationException.from(e);
        }
        emitter().close();
    }

    /**
     * Copy the Exelixi AOCL library files
     *
     * @param sourcePath  the path where the .cpp files will be copied
     * @param includePath the path where the .h files will be copied
     */
    default void copyLibrary(Path sourcePath, Path includePath) {
        // copy the AOCL library resolver source code
        emitter().open(sourcePath.resolve(sourcePath.resolve("AOCL.cpp")));
        try (InputStream in = ClassLoader.getSystemResourceAsStream("aocl_backend_code/AOCL.cpp")) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            reader.lines().forEach(emitter()::emitRawLine);
        } catch (IOException e) {
            throw CompilationException.from(e);
        }
        emitter().close();

        // copy the AOCL library resolver header
        emitter().open(includePath.resolve(includePath.resolve("AOCL.h")));
        try (InputStream in = ClassLoader.getSystemResourceAsStream("aocl_backend_code/AOCL.h")) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            reader.lines().forEach(emitter()::emitRawLine);
        } catch (IOException e) {
            throw CompilationException.from(e);
        }
        emitter().close();

        // copy the utils library resolver header
        emitter().open(includePath.resolve(includePath.resolve("utils.h")));
        try (InputStream in = ClassLoader.getSystemResourceAsStream("aocl_backend_code/utils.h")) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            reader.lines().forEach(emitter()::emitRawLine);
        } catch (IOException e) {
            throw CompilationException.from(e);
        }
        emitter().close();

    }

     /*=================================================================================================================*/
    /*
    /* Utility methods
    /*
    /*=================================================================================================================*/

    default void scheduleFunction() {
        emitter().emit("void schedule_host(){");
        emitter().increaseIndentation();

        // used to check if all input files are availables
        emitter().emit("// check if all input files are availables");
        emitter().emit("bool files_available = true;");
        emitter().emit("");

        // initialize the interfaces variables
        resolver().getIncomings().forEach(connection -> {
            // get the id
            int id = resolver().getConnectionId(connection);
            // get the type
            PortDecl portDecl = connection.getTarget().getInstance().isPresent() ? resolver().getTargetPortDecl(connection) : resolver().getSourcePortDecl(connection);
            Type tokenType = backend().types().declaredPortType(portDecl);
            String type = backend().code().type(tokenType);

            emitter().emit("// input interface %d", id);
            emitter().emit("FILE *interface_%d_fp = fopen(\"%s.txt\", \"r\");", id, connection.getSource().getPort());
            emitter().emit("long interface_%d_tx_tokens = 0;", id);
            emitter().emit("%s interface_%d_value;", type, id);
            emitter().emit("*interface_%d_read = 0;", id);
            emitter().emit("*interface_%d_write = 0;", id);
            emitter().emit("");
            emitter().emit("if(interface_%d_fp==NULL){", id);
            emitter().increaseIndentation();
            emitter().emit("printf(\"ERROR: unable to find input file %s.txt\\n\");", connection.getSource().getPort());
            emitter().emit("files_available = false;");
            emitter().decreaseIndentation();
            emitter().emit("}");
            emitter().emit("");
        });

        resolver().getOutgoings().forEach(connection -> {
            int id = resolver().getConnectionId(connection);
            emitter().emit("// output interface %d", id);
            emitter().emit("FILE *interface_%d_fp = fopen(\"%s.txt\", \"w\");", id, connection.getTarget().getPort());
            emitter().emit("long interface_%d_rx_tokens = 0;", id);
            emitter().emit("*interface_%d_read = 0;", id);
            emitter().emit("*interface_%d_write = 0;", id);
            emitter().emit("");
            emitter().emit("if(interface_%d_fp==NULL){", id);
            emitter().increaseIndentation();
            emitter().emit("printf(\"ERROR: unable to create output file %s.txt\\n\");", connection.getTarget().getPort());
            emitter().emit("files_available = false;");
            emitter().decreaseIndentation();
            emitter().emit("}");
            emitter().emit("");
        });

        emitter().emit("if(!files_available){");
        emitter().increaseIndentation();
        emitter().emit("test_error(CL_CONFIGURATION_ERROR, \"ERROR: Failed to load/create input/output files\", &cleanup);");
        emitter().decreaseIndentation();
        emitter().emit("}");
        emitter().emit("");

        emitter().emit("// temporary variables");
        emitter().emit("int tmp_read, tmp_write, rooms, count, parsedTokens;");
        emitter().emit("");

        emitter().emit("printf(\"[host] Execution start\\n\");");
        emitter().emit("");

        emitter().emit("// schedule the interface");
        emitter().emit("double time_start = get_current_timestamp(); // timeout timer");
        emitter().emit("do{");
        emitter().increaseIndentation();

        // profiling
        if (configuration().get(profile).booleanValue()) {
            emitter().emit("// collect profiling data");
            backend().task().getNetwork().getInstances().forEach(instance -> {
                emitter().emit("clGetProfileInfoAltera(event_%s);", instance.getInstanceName());
            });
            emitter().emit("");
        }

        // parse input files
        emitter().emit("// parse input files");
        resolver().getIncomings().forEach(connection -> {
                    // get the id
                    int id = resolver().getConnectionId(connection);
                    // get the type
                    PortDecl portDecl = connection.getTarget().getInstance().isPresent() ? resolver().getTargetPortDecl(connection) : resolver().getSourcePortDecl(connection);
                    Type tokenType = backend().types().declaredPortType(portDecl);
                    String type = backend().code().type(tokenType);

                    emitter().emit("parsedTokens = 0;");
                    emitter().emit("tmp_read  = *interface_%d_read;", id);
                    emitter().emit("tmp_write = *interface_%d_write;", id);
                    emitter().emit("rooms = (FIFO_DEPTH + tmp_read - tmp_write - 1) %% FIFO_DEPTH;");
                    emitter().emit("while (rooms && read_%s_value(interface_%d_fp, &interface_%d_value)) {", type, id, id);
                    emitter().increaseIndentation();
                    emitter().emit("interface_%d_buffer[(tmp_write + parsedTokens) %% FIFO_DEPTH] = interface_%d_value;", id, id, id);
                    emitter().emit("parsedTokens++;");
                    emitter().emit("rooms--;");
                    emitter().decreaseIndentation();
                    emitter().emit("}"); // end while rooms
                    emitter().emit("if (parsedTokens) {");
                    emitter().increaseIndentation();
                    emitter().emit("*interface_%d_write = (tmp_write + parsedTokens) %% FIFO_DEPTH;", id);
                    emitter().emit("cl_int status = clEnqueueTask(queues[QUEUE_INTERFACE_%d], kernel_interface_%d, 0, NULL, NULL);", id, id);
                    emitter().emit("test_error(status, \"ERROR: Failed to launch interface %d kernel.\\n\", &cleanup);", id);
                    emitter().emit("status = clFinish(queues[QUEUE_INTERFACE_%d]);", id);
                    emitter().emit("interface_%d_tx_tokens += parsedTokens;", id);
                    emitter().emit("time_start = get_current_timestamp();");
                    emitter().decreaseIndentation();
                    emitter().emit("}"); // end if parsed tokens and schedule = true
                    emitter().emit("");
                }
        );

        // collect output data
        emitter().emit("// collect output data");
        resolver().getOutgoings().forEach(connection -> {
            // get the id
            int id = resolver().getConnectionId(connection);
            // get the type
            PortDecl portDecl = connection.getTarget().getInstance().isPresent() ? resolver().getTargetPortDecl(connection) : resolver().getSourcePortDecl(connection);
            Type tokenType = backend().types().declaredPortType(portDecl);
            String type = backend().code().type(tokenType);

            emitter().emit("status = clEnqueueTask(queues[QUEUE_INTERFACE_%d], kernel_interface_%d, 0, NULL, NULL);", id, id);
            emitter().emit("test_error(status, \"ERROR: Failed to launch  interface %d.\\n\", &cleanup);", id);
            emitter().emit("status = clFinish(queues[QUEUE_INTERFACE_%d]);", id);
            emitter().emit("count = (FIFO_DEPTH + *interface_%d_write - *interface_%d_read) %% FIFO_DEPTH;", id, id);
            emitter().emit("if(count){");
            emitter().increaseIndentation();
            emitter().emit("for(int i = 0; i < count; i++){");
            emitter().increaseIndentation();
            emitter().emit("write_%s_value(interface_%d_fp, interface_%d_buffer[(*interface_%d_read + i) %% FIFO_DEPTH]);", type, id, id, id);
            emitter().decreaseIndentation();
            emitter().emit("}");
            emitter().emit("interface_%d_rx_tokens += count;", id);
            emitter().emit("*interface_%d_read = (*interface_%d_read + count) %% FIFO_DEPTH;", id, id);
            emitter().emit("time_start = get_current_timestamp();");
            emitter().decreaseIndentation();
            emitter().emit("}");
            emitter().emit("");
        });

        emitter().decreaseIndentation();
        emitter().emit("}while((get_current_timestamp() - time_start) < TIMEOUT_SEC);");
        emitter().emit("");

        emitter().emit("printf(\"[host] Execution done\\n\");");

        // print tokens counts
        emitter().emit("// print tokens counts");
        resolver().getIncomings().forEach(connection -> {
            int id = resolver().getConnectionId(connection);
            emitter().emit("printf(\"[interface %d] TX tokens: %%d\\n\", interface_%d_tx_tokens);", id, id);
        });
        resolver().getOutgoings().forEach(connection -> {
            int id = resolver().getConnectionId(connection);
            emitter().emit("printf(\"[interface %d] RX tokens: %%d\\n\", interface_%d_rx_tokens);", id, id);
        });


        emitter().decreaseIndentation();
        emitter().emit("}");
        emitter().emit("");
    }

    default void cleanupFunction() {
        Network network = backend().task().getNetwork();

        emitter().emit("void cleanup(){");
        emitter().increaseIndentation();
        emitter().emit("");

        network.getInstances().forEach(instance -> {
            String name = instance.getInstanceName();
            emitter().emit("if(kernel_actor_%s){", name);
            emitter().increaseIndentation();
            emitter().emit("clReleaseKernel(kernel_actor_%s);", name);
            emitter().decreaseIndentation();
            emitter().emit("}");
            emitter().emit("");
        });

        union(resolver().getIncomings(), resolver().getOutgoings()).forEach(connection -> {
                    int id = resolver().getConnectionId(connection);
                    emitter().emit("if(kernel_interface_%d){", id);
                    emitter().increaseIndentation();
                    emitter().emit("clReleaseKernel(kernel_interface_%d);", id);
                    emitter().decreaseIndentation();
                    emitter().emit("}");
                    emitter().emit("");
                }
        );

        emitter().emit("if (program) {");
        emitter().increaseIndentation();
        emitter().emit("clReleaseProgram(program);");
        emitter().decreaseIndentation();
        emitter().emit("}");
        emitter().emit("");

        emitter().emit("for (int i = 0; i < QUEUES_SIZE; ++i) {");
        emitter().increaseIndentation();
        emitter().emit("cl_command_queue queue = queues[i];");
        emitter().emit("if (queue) {");
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
        emitter().emit("");
    }

}
