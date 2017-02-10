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

/**
 * @author Simone Casale-Brunet
 */

import org.multij.Binding;
import org.multij.Module;
import se.lth.cs.tycho.comp.SourceUnit;
import se.lth.cs.tycho.ir.decl.GlobalEntityDecl;
import se.lth.cs.tycho.ir.decl.GlobalVarDecl;
import se.lth.cs.tycho.ir.decl.VarDecl;
import se.lth.cs.tycho.ir.network.Connection;
import se.lth.cs.tycho.ir.network.Instance;
import se.lth.cs.tycho.phases.attributes.Types;
import se.lth.cs.tycho.phases.cbackend.Emitter;
import se.lth.cs.tycho.types.CallableType;
import se.lth.cs.tycho.types.Type;
import xyz.exelixi.backend.opencl.aocl.AoclBackendCore;
import xyz.exelixi.utils.Resolver;
import xyz.exelixi.utils.Utils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Module
public interface Device {

    @Binding
    AoclBackendCore backend();

    default Resolver resolver(){return  backend().resolver().get();}

    default Emitter emitter() { return backend().emitter(); }

    default Structure structure() { return backend().structure(); }

    default Types types() {
        return backend().types();
    }

    default Code code() {
        return backend().code();
    }

    default void generateInstance(Instance instance, Path path) {
        backend().instance().set(instance);

        GlobalEntityDecl actor = backend().task().getSourceUnits().stream()
                .map(SourceUnit::getTree)
                .filter(ns -> ns.getQID().equals(instance.getEntityName().getButLast()))
                .flatMap(ns -> ns.getEntityDecls().stream())
                .filter(decl -> decl.getName().equals(instance.getEntityName().getLast().toString()))
                .findFirst().get();

        String fileNameBase = instance.getInstanceName();
        // -- Filename
        String fileName = fileNameBase + ".cl";

        // -- Open file for code generation
        emitter().open(path.resolve(fileName));

        // -- File Notice
        backend().fileNotice().generateNotice("Source code for Instance: " + instance.getInstanceName() + ", Actor: " + instance.getEntityName());
        emitter().emit("");

        // -- Actor Structure Declaration
        structure().actorGeneration(actor);

        // -- Close the File
        emitter().close();
        backend().instance().clear();
    }

    default void generateGlobals(Path path) {
        emitter().open(path.resolve(path.resolve("global.h")));
        emitter().emit("#ifndef GLOBAL_H");
        emitter().emit("#define GLOBAL_H");
        emitter().emit("");
        globalVariableDeclarations(getGlobalVarDecls());
        emitter().emit("#define FIFO_DEPTH 512");
        emitter().emit("");
        emitter().emit("#endif");
        emitter().close();
    }

    default void globalVariableDeclarations(List<GlobalVarDecl> varDecls) {
        for (VarDecl decl : varDecls) {
            Type type = types().declaredType(decl);
            if (decl.isExternal() && type instanceof CallableType) {
                throw new Error("Not supported");
                /*
                String wrapperName = backend().callables().externalWrapperFunctionName(decl);
                String variableName = backend().variables().declarationName(decl);
                String t = backend().callables().mangle(type).encode();
                emitter().emit("%s = (%s) { *%s, NULL };", variableName, t, wrapperName);
                */
            } else {
                emitter().emit("#define %s %s", backend().variables().declarationName(decl), code().evaluate(decl.getValue()));
            }
        }
    }

    default List<GlobalVarDecl> getGlobalVarDecls() {
        return backend().task()
                .getSourceUnits().stream()
                .flatMap(unit -> unit.getTree().getVarDecls().stream())
                .collect(Collectors.toList());
    }

    default void generateSynthesisScript(Path path) {
        emitter().open(path.resolve(path.resolve("aocl_synthesis.sh")));
        List<String> kernels = new ArrayList<>();
        for(Instance instance : backend().task().getNetwork().getInstances()){
            kernels.add("device/" + instance.getInstanceName()+".cl");
        }

        List<Connection> borderConnections = Utils.union(resolver().getIncomings(), resolver().getOutgoings());
        for(Connection connection : borderConnections){
            int id = backend().resolver().get().getConnectionId(connection);
            kernels.add("device/interface_"+id+".cl");
        }

        emitter().emit("#!/bin/bash");
        emitter().emit("aoc -march=emulator %s -o bin/device.aocx", String.join(" ", kernels));
        emitter().close();
    }
}
