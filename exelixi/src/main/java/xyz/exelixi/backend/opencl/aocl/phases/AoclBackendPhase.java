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
package xyz.exelixi.backend.opencl.aocl.phases;

import org.multij.MultiJ;
import se.lth.cs.tycho.comp.CompilationTask;
import se.lth.cs.tycho.comp.Context;
import se.lth.cs.tycho.ir.network.Connection;
import se.lth.cs.tycho.ir.network.Instance;
import se.lth.cs.tycho.ir.network.Network;
import se.lth.cs.tycho.phases.Phase;
import se.lth.cs.tycho.reporting.CompilationException;
import xyz.exelixi.Settings;
import xyz.exelixi.backend.opencl.aocl.AoclBackendCore;
import xyz.exelixi.utils.Resolver;
import xyz.exelixi.utils.Utils;

import java.io.File;
import java.nio.file.Path;

/**
 * @author Simone Casale-Brunet
 */
public class AoclBackendPhase implements Phase {

    /**
     * Device source path
     */
    private Path device_srcPath;

    /**
     * Host source path
     */
    private Path host_srcPath;

    /**
     * Host include path
     */
    private Path host_includePath;

    /**
     * Target Path
     */
    private Path targetPath;


    @Override
    public String getDescription() {
        return "Altera OpenCL backend Phase";
    }

    @Override
    public CompilationTask execute(CompilationTask task, Context context) throws CompilationException {

        // of the directories
        // Get target Path
        targetPath = context.getConfiguration().get(Settings.targetPath);

        host_srcPath = Utils.createDirectory(targetPath, "host" + File.separator + "src");
        host_includePath = Utils.createDirectory(targetPath, "host" + File.separator + "include");

        device_srcPath = Utils.createDirectory(targetPath, "device");

        // open the core
        AoclBackendCore core = MultiJ.from(AoclBackendCore.class)
                .bind("task").to(task)
                .bind("context").to(context)
                .instance();

        Resolver helper = Resolver.create(task);
        core.resolver().set(helper);

        //** generate the devices code **//
        // globals definitions
        core.device().generateGlobals(device_srcPath);

        // actors kernel implementations
        Network network = core.task().getNetwork();

        // Generate CL source code for every instance
        for (Instance instance : network.getInstances()) {
            core.device().generateInstance(instance, device_srcPath);
        }

        // Generate CL source code for every input port
        for(Connection connection : helper.getIncomings()){
            core.interfaces().generateInputInterface(connection, device_srcPath);
        }

        // Generate CL source code for every output port
        for(Connection connection : helper.getOutgoings()){
            core.interfaces().generateOutputInterface(connection, device_srcPath);
        }

        //**  generate the host code **/
        core.host().generateLibrary(host_srcPath, host_includePath); // the AOCL library
        core.host().generateSourceCode(host_srcPath);

        /** generate the Makefile **/
        core.host().generateMakeFile(targetPath);

        /** generate the Synthesis script **/
        core.device().generateSynthesisScript(targetPath);

        // clear the model resolver, we do not need it anymore
        core.resolver().clear();

        return task;
    }

}
