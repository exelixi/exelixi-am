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
import se.lth.cs.tycho.ir.network.Instance;
import se.lth.cs.tycho.phases.cbackend.Emitter;
import sun.nio.cs.Surrogate;
import xyz.exelixi.backend.hls.HlsBackendCore;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A Vivado HLS project TCL emitter
 *
 * @author Endri Bezati
 */
@Module
public interface TclHlsProject {
    @Binding
    HlsBackendCore backend();

    default Emitter emitter() {
        return backend().emitter();
    }

    default void generateTclProject(Instance instance, Path path) {
        // -- Get Name
        String instanceName = instance.getInstanceName();
        // -- Get Filename
        String fileName = instanceName + "_hls.tcl";

        // -- Open file for code generation
        emitter().open(path.resolve(fileName));

        // -- File Notice
        backend().fileNotice().generateTclNotice("Vivado HLS TCL project for Instance: " + instance.getInstanceName());

        emitter().emit("");

        // -- Set paths
        setSciptPaths();

        // -- Create project
        createProject(instanceName);

        // -- Add files
        List<String> fileNames = new ArrayList<String>();
        fileNames.add("global");
        fileNames.add(instanceName);
        addFiles(fileNames);

        // -- Add testbench file
        addFile("tbsrc_path", instanceName + "_tb", "\"-I$include_path\"", true);
        emitter().emit("");

        // -- Add fifo trace directory for testbench
        emitter().emit("add_files -tb ../fifo-traces");
        emitter().emit("");

        // -- Set top Level
        setTopLevel(instanceName);

        // -- Open Solution
        openSolution(instanceName);

        // -- Define Xilinx FPGA Part
        // -- FIXME: Get it from settings, or default value
        setPart("xc7z020clg484-1");

        // -- Create Clock
        // -- FIXME: Get it from settings, or default value
        createClock(10);

        // -- Exit
        emitter().emit("exit");

        // -- Close the File
        emitter().close();
    }

    default void setSciptPaths() {
        // -- Source path
        emitter().emit("## -- Set Paths");
        emitter().emit("set src_path ../code-gen/src");
        // -- C++ Testbench path
        emitter().emit("set tbsrc_path ../code-gen/src-tb");
        // -- Include path
        emitter().emit("set include_path ../code-gen/include");
        emitter().emit("");
    }

    default void createProject(String name) {
        emitter().emit("## -- Create Project");
        emitter().emit("open_project hls_%s", name);
        emitter().emit("");
    }

    default void addFiles(List<String> fileNames) {
        emitter().emit("## -- Add files");
        for (String fileName : fileNames) {
            addFile("src_path", fileName, "\"-I$include_path\"", false);
        }
        emitter().emit("");
    }

    default void addFile(String path, String fileName, String cflags, boolean isTestBench) {
        emitter().emit("add_files %s $%s/%s.cpp -cflags %s", isTestBench ? "-tb" : "", path, fileName, cflags);
    }


    default void setTopLevel(String top) {
        emitter().emit("## -- Set top");
        emitter().emit("set_top %s", top);
        emitter().emit("");
    }

    default void openSolution(String name) {
        emitter().emit("## -- Open Solution and reset it");
        emitter().emit("open_solution -reset %s_solution", name);
        emitter().emit("");
    }

    default void setPart(String part){
        emitter().emit("## -- Set Xilinx FPGA part");
        emitter().emit("set_part {%s}", part);
        emitter().emit("");
    }

    default void createClock(float period){
        emitter().emit("## -- Create Clock");
        emitter().emit("create_clock -period %.1f -name clk", period);
        emitter().emit("");
    }

}
