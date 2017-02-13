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
package xyz.exelixi.backend.hls.codegen;

import org.multij.Binding;
import org.multij.Module;
import se.lth.cs.tycho.comp.SourceUnit;
import se.lth.cs.tycho.ir.Port;
import se.lth.cs.tycho.ir.decl.GlobalEntityDecl;
import se.lth.cs.tycho.ir.entity.Entity;
import se.lth.cs.tycho.ir.entity.PortDecl;
import se.lth.cs.tycho.ir.entity.am.ActorMachine;
import se.lth.cs.tycho.ir.network.Instance;
import se.lth.cs.tycho.phases.attributes.Types;
import se.lth.cs.tycho.phases.cbackend.Emitter;
import xyz.exelixi.backend.hls.HlsBackendCore;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A C++ Testbench for Vivado HLS
 *
 * @author Endri Bezati
 */
@Module
public interface Testbench {
    @Binding
    HlsBackendCore backend();

    default Emitter emitter() {
        return backend().emitter();
    }

    default Preprocessor preprocessor() {
        return backend().preprocessor();
    }

    default Types types() {
        return backend().types();
    }

    default void generateTestbench(Instance instance, Path path) {
        // -- Get Name
        String instanceName = instance.getInstanceName();

        // -- Get Filename
        String fileName = instanceName + "_tb.cpp";

        // -- Open file for code generation
        emitter().open(path.resolve(fileName));

        // -- File Notice
        backend().fileNotice().generateCNotice("Testbench for Instance: " + instanceName);
        emitter().emit("");

        // -- Includes
        preprocessor().systemInclude("fstream");
        preprocessor().systemInclude("sstream");
        preprocessor().systemInclude("string");
        preprocessor().systemInclude("stdint.h");
        preprocessor().systemInclude("hls_stream.h");
        preprocessor().userInclude(instanceName + ".h");
        emitter().emit("");

        // -- Main
        emitter().emit("int main(int argc, char *argv[]){");
        emitter().increaseIndentation();

        // -- Global Entity
        GlobalEntityDecl actor = backend().task().getSourceUnits().stream()
                .map(SourceUnit::getTree)
                .filter(ns -> ns.getQID().equals(instance.getEntityName().getButLast()))
                .flatMap(ns -> ns.getEntityDecls().stream())
                .filter(decl -> decl.getName().equals(instance.getEntityName().getLast().toString()))
                .findFirst().get();

        // -- I/O File streams
        ioFileStreams(instanceName, actor.getEntity());

        // -- I/O Channels
        ioChannels(actor.getEntity());

        // -- Fill Input streams
        fillStreams(actor.getEntity());

        // -- Output port(s) Token Counter
        outputTokenCounters(actor.getEntity());

        // -- I/O port Counts
        ioPortCounts(actor.getEntity());

        // -- End of execution boolean
        emitter().emit("// -- End of execution condition");
        emitter().emit("bool end_of_execution = false;");
        emitter().emit("");

        // -- unit under test loop
        uutLoop(instanceName, actor.getEntity());

        // -- Emit Report
        emitReport(instanceName, actor.getEntity());

        emitter().decreaseIndentation();
        emitter().emit("}");

        // -- Close the File
        emitter().close();
    }


    // ------------------------------------------------------------------------
    // -- IO File Streams

    default void ioFileStream(String name, PortDecl port, String errorMessage) {
        emitter().emit("std::ifstream %s_file(\"fifo-traces/%s/%s.txt\");", port.getName(), name, port.getName());
        emitter().emit("if(%s_file == NULL){", port.getName());
        emitter().increaseIndentation();
        emitter().emit("std::cout << \"%s.txt %s\" << std::endl;", port.getName(), errorMessage);
        emitter().emit("return 1;");
        emitter().decreaseIndentation();
        emitter().emit("}");
        emitter().emit("");
    }

    default void ioFileStreams(String name, Entity entity) {
    }

    default void ioFileStreams(String name, ActorMachine actorMachine) {
        // -- IO ports
        List<PortDecl> inputs = actorMachine.getInputPorts();
        List<PortDecl> outputs = actorMachine.getOutputPorts();
        emitter().emit("// -- File Streams for Input Ports");

        for (PortDecl port : inputs) {
            ioFileStream(name, port, "input file not found !!!");
        }

        for (PortDecl port : outputs) {
            ioFileStream(name, port, "golden reference not found !!!");
        }
    }

    // ------------------------------------------------------------------------
    // -- IO Channels

    default void ioChannels(Entity entity) {
    }

    default void ioChannels(ActorMachine actorMachine) {
        // -- IO ports
        List<PortDecl> inputs = actorMachine.getInputPorts();
        List<PortDecl> outputs = actorMachine.getOutputPorts();

        emitter().emit("// -- Input Channels");
        for (String value : backend().code().portIO(inputs, false, false)) {
            emitter().emit("%s;", value);
        }
        emitter().emit("// -- Output Channels");
        for (String value : backend().code().portIO(outputs, false, false)) {
            emitter().emit("%s;", value);
        }

        if (!outputs.isEmpty()) {
            // -- Output reference Queues
            for (PortDecl portDecl : outputs) {
                emitter().emit("std::queue< %s > qref_%s;", backend().code().type(types().declaredPortType(portDecl)), portDecl.getName());
            }
        }
        emitter().emit("");
    }

    // ------------------------------------------------------------------------
    // -- I/O Port Counts

    default void ioPortCounts(Entity entity) {
    }

    default void ioPortCounts(ActorMachine actorMachine) {
        // -- IO ports
        List<PortDecl> inputs = actorMachine.getInputPorts();
        List<PortDecl> outputs = actorMachine.getOutputPorts();

        emitter().emit("// -- I/O port counts, on output emulate infinite buffers");
        for (PortDecl portDecl : inputs) {
            emitter().emit("uint32_t %s_count = 0;", portDecl.getName());
        }

        for (PortDecl portDecl : outputs) {
            emitter().emit("uint32_t %s_count = 0;", portDecl.getName());
        }

        emitter().emit("");

    }

    // ------------------------------------------------------------------------
    // -- Fill Streams


    default void fillStream(PortDecl portDecl, boolean isInput) {
        String portName = portDecl.getName();
        String type = backend().code().type(types().declaredPortType(portDecl));
        emitter().emit("// -- Write the tokens from the filestream to the queue");
        emitter().emit("std::string %s_line;", portName);
        emitter().emit("while(std::getline(%s_file,%s_line)){", portName, portName);
        emitter().increaseIndentation();
        emitter().emit("std::istringstream iss(%s_line);", portName);
        emitter().emit("%s %s_tmp;", type, portName);
        emitter().emit("iss >> %s_tmp;", portName);
        if (isInput) {
            emitter().emit("%s.write(%s_tmp);", portName, portName);
        } else {
            emitter().emit("qref_%s.push(%s_tmp);", portName, portName);
        }
        emitter().decreaseIndentation();
        emitter().emit("}");
        emitter().emit("");
    }

    default void fillStreams(Entity entity) {
    }

    default void fillStreams(ActorMachine actorMachine) {
        // -- IO ports
        List<PortDecl> inputs = actorMachine.getInputPorts();
        List<PortDecl> outputs = actorMachine.getOutputPorts();

        // -- Inputs
        for (PortDecl portDecl : inputs) {
            fillStream(portDecl, true);
        }

        // -- Outputs
        for (PortDecl portDecl : outputs) {
            fillStream(portDecl, false);
        }
    }

    // ------------------------------------------------------------------------
    // -- Output token counters


    default void outputTokenCounters(Entity entity) {

    }

    default void outputTokenCounters(ActorMachine actorMachine) {
        List<PortDecl> outputs = actorMachine.getOutputPorts();

        if (!outputs.isEmpty()) {
            emitter().emit("// -- Output port token counters");
            for (PortDecl portDecl : outputs) {
                String type = "uint32_t";
                emitter().emit("%s %s_token_counter;", type, portDecl.getName());
            }
            emitter().emit("");
        }
    }


    // ------------------------------------------------------------------------
    // -- Unit Under test Loop

    default void uutLoop(String name, Entity entity) {

    }

    default void uutLoop(String name, ActorMachine actorMachine) {
        emitter().emit("// -- Execute the Unit Under Test");
        emitter().emit("while(!end_of_execution){");
        emitter().increaseIndentation();
        // -- unit under test body

        emitter().emit("// -- Update Input Counts");
        for (PortDecl portDecl : actorMachine.getInputPorts()) {
            emitter().emit("%s_count = %s.size();", portDecl.getName(), portDecl.getName());
        }
        emitter().emit("// -- Call UUT");
        List<String> parameters = backend().code().actorMachineIOName(actorMachine, true, true);
        emitter().emit("%s(%s);", name, String.join(", ", parameters));
        List<String> outputPortEmpty = new ArrayList<String>();
        for (PortDecl portDecl : actorMachine.getOutputPorts()) {
            checkOutputPort(portDecl);
            outputPortEmpty.add(portDecl.getName() + ".empty()");
        }

        emitter().emit("end_of_execution = %s;", String.join(" && ", outputPortEmpty));

        emitter().decreaseIndentation();
        emitter().emit("}");
        emitter().emit("");
    }


    default void checkOutputPort(PortDecl portDecl) {
        String portName = portDecl.getName();
        emitter().emit("if(%s.empty() && qref_%s.empty()){", portDecl.getName(), portName);
        emitter().increaseIndentation();

        String type = backend().code().type(types().declaredPortType(portDecl));
        emitter().emit("%s got_value = %s.read();", type, portName);
        emitter().emit("%s ref_value = qref_%s.front();", type, portName);
        emitter().emit("qref_%s.pop();", portName);

        emitter().emit("if(got_value != ref_value){");
        emitter().increaseIndentation();
        emitter().emit("std::cout << \"Port %s: Error !!! Expected value does not match golden reference, Token Counter: \" << %s_token_counter << std::endl;", portName, portName);
        emitter().emit("std::cout << \"Expected: \" << ref_value << std::endl;");
        emitter().emit("std::cout << \"Got: \" << got_value << std::endl;");
        emitter().emit("return 1;");
        emitter().decreaseIndentation();
        emitter().emit("}");
        emitter().emit("");
        emitter().emit("%s_token_counter++;", portName);

        emitter().decreaseIndentation();
        emitter().emit("}");
        emitter().emit("");
    }


    // ------------------------------------------------------------------------
    // -- Report
    default void emitReport(String instanceName, Entity entity) {

    }

    default void emitReport(String instanceName, ActorMachine actorMachine) {
        emitter().emit("// -- Report");
        emitter().emit("std::cout << \"Passed Testbench for %s ! \" << std::endl;", instanceName);
        emitter().emit("");

        for (PortDecl portDecl : actorMachine.getOutputPorts()) {
            emitter().emit("std::cout << \"Port %s : \" << %s_token_counter << \" produced.\" << std::endl;", portDecl.getName(), portDecl.getName());
        }
    }


}
