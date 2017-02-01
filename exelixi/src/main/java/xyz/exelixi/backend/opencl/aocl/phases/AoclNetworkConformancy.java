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
import se.lth.cs.tycho.comp.SourceUnit;
import se.lth.cs.tycho.ir.decl.GlobalEntityDecl;
import se.lth.cs.tycho.ir.entity.cal.CalActor;
import se.lth.cs.tycho.ir.network.Instance;
import se.lth.cs.tycho.ir.network.Network;
import se.lth.cs.tycho.phases.Phase;
import se.lth.cs.tycho.reporting.CompilationException;
import xyz.exelixi.backend.opencl.aocl.AoclBackendCore;

/**
 * Check if the network is composed only by processes
 *
 * @author Simone Casale-Brunet
 */
public class AoclNetworkConformancy implements Phase {
    @Override
    public String getDescription() {
        return "Network Conformancy test for Altera OpenCL Phase";
    }

    @Override
    public CompilationTask execute(CompilationTask task, Context context) throws CompilationException {

        // check if in the network there are some actors
        for (Instance instance : task.getNetwork().getInstances()) {
            GlobalEntityDecl actor = task.getSourceUnits().stream()
                    .map(SourceUnit::getTree)
                    .filter(ns -> ns.getQID().equals(instance.getEntityName().getButLast()))
                    .flatMap(ns -> ns.getEntityDecls().stream())
                    .filter(decl -> decl.getName().equals(instance.getEntityName().getLast().toString()))
                    .findFirst().get();
            if (!(actor.getEntity() instanceof CalActor)) {
                throw new Error("Only network of processes are supported by the AOCL backend");
            }
        }

        return task;
    }

}
