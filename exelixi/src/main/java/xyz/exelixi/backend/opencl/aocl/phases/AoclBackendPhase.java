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
import se.lth.cs.tycho.ir.decl.GlobalVarDecl;
import se.lth.cs.tycho.ir.network.Instance;
import se.lth.cs.tycho.phases.Phase;
import se.lth.cs.tycho.reporting.CompilationException;
import xyz.exelixi.Settings;
import xyz.exelixi.backend.hls.HlsBackendCore;
import xyz.exelixi.backend.opencl.aocl.AoclBackendCore;
import xyz.exelixi.utils.Utils;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

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

        // create the directories
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


        //** generate the devices code **//
        // globals definitions
        core.device().generateGlobals(device_srcPath);

        // actors kernel implementations
        for (Instance instance : core.task().getNetwork().getInstances()) {
            // Generate CL source code
            core.device().generateActor(instance, device_srcPath);
        }

        // fictitious actors as kernel interfaces
        core.device().generateInterfaces(device_srcPath);

        //**  generate the host code **/
        core.host().generateSourceCode(host_srcPath);
        core.host().generateHeaders(host_includePath);

        /** generate the Makefile **/
        core.host().generateMakeFile(targetPath);

        return task;
    }

}
