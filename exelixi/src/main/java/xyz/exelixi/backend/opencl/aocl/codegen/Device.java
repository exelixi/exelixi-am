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
import se.lth.cs.tycho.comp.SourceUnit;
import se.lth.cs.tycho.ir.decl.GlobalEntityDecl;
import se.lth.cs.tycho.ir.decl.GlobalVarDecl;
import se.lth.cs.tycho.ir.decl.VarDecl;
import se.lth.cs.tycho.ir.entity.Entity;
import se.lth.cs.tycho.ir.entity.cal.CalActor;
import se.lth.cs.tycho.ir.entity.cal.ProcessDescription;
import se.lth.cs.tycho.ir.network.Instance;
import se.lth.cs.tycho.ir.network.Network;
import se.lth.cs.tycho.ir.type.FunctionTypeExpr;
import se.lth.cs.tycho.phases.attributes.Types;
import se.lth.cs.tycho.phases.cbackend.Emitter;
import se.lth.cs.tycho.reporting.CompilationException;
import se.lth.cs.tycho.settings.Configuration;
import se.lth.cs.tycho.types.CallableType;
import se.lth.cs.tycho.types.Type;
import xyz.exelixi.backend.opencl.aocl.AoclBackendCore;
import xyz.exelixi.utils.Resolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static xyz.exelixi.backend.opencl.aocl.phases.AoclBackendPhase.usePipes;
import static xyz.exelixi.utils.Utils.union;

/**
 * Source code generation for the devices
 *
 * @author Simone Casale-Brunet
 */
@Module
public interface Device {

    @Binding
    AoclBackendCore backend();

    default Resolver resolver() {
        return backend().resolver().get();
    }

    default Emitter emitter() {
        return backend().emitter();
    }

    default Types types() {
        return backend().types();
    }

    default Code code() {
        return backend().code();
    }

    default Callables callables() {
        return backend().callables();
    }

    default DefaultValues defaultValues() {
        return backend().defaultValues();
    }

    default Configuration configuration() {
        return backend().context().getConfiguration();
    }

    /**
     * Generate the source code for an instance. Only processes are supported
     *
     * @param instance
     * @param path
     */
    default void generateInstance(Instance instance, Path path) {
        backend().instance().set(instance);

        GlobalEntityDecl actorEntity = backend().task().getSourceUnits().stream()
                .map(SourceUnit::getTree)
                .filter(ns -> ns.getQID().equals(instance.getEntityName().getButLast()))
                .flatMap(ns -> ns.getEntityDecls().stream())
                .filter(decl -> decl.getName().equals(instance.getEntityName().getLast().toString()))
                .findFirst().get();

        // check if the actor is a process, if not thrown an exception
        Entity entity = actorEntity.getEntity();
        if (!(entity instanceof CalActor)) {
            throw new Error("unsupported actor type: only processes are supported");
        }

        System.out.println("Generating source code for actor: " + backend().instance().get().getInstanceName());

        // open the emitter. the file is <instance_name>.cl
        emitter().open(path.resolve(instance.getInstanceName() + ".cl"));

        // add file Notice
        backend().fileNotice().generateNotice("Source code for Instance: " + instance.getInstanceName() + ", Actor: " + instance.getEntityName());

        // source code generation
        generateActor((CalActor) entity);

        // close the File
        emitter().close();
        backend().instance().clear();
    }

    default void generateDeviceContainer(Path path) {
        Network network = backend().task().getNetwork();

        emitter().open(path.resolve("device.cl"));

        emitter().emit("#include \"../host/include/sharedconstants.h\"");
        emitter().emit("#include \"global.h\"");

        network.getInstances().forEach(instance -> {
            emitter().emit("#include \"%s.cl\"", instance.getInstanceName());
        });

        union(resolver().getIncomings(), resolver().getOutgoings()).forEach(connection -> {
            int id = resolver().getConnectionId(connection);
            emitter().emit("#include \"interface_%d.cl\"", id);
        });

        emitter().close();
    }


    /**
     * Generate the global.h file with contains all the constant definition shared by the kernels of the network
     *
     * @param path
     */
    default void generateGlobals(Path path) {
        emitter().open(path.resolve(path.resolve("global.h")));
        emitter().emit("#ifndef GLOBAL_H");
        emitter().emit("#define GLOBAL_H");
        emitter().emit("");

        List<GlobalVarDecl> globalVarDecls = backend().task()
                .getSourceUnits().stream()
                .flatMap(unit -> unit.getTree().getVarDecls().stream())
                .collect(Collectors.toList());

        for (VarDecl decl : globalVarDecls) {
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

        // altera channels
        if (!configuration().get(usePipes).booleanValue()) {
            emitter().emit("#pragma OPENCL EXTENSION cl_altera_channels : enable");
            backend().task().getNetwork().getConnections().forEach(connection -> {
                emitter().emit("%s __attribute__((depth(FIFO_DEPTH)));", code().alteraChannelDefinition(connection));
            });
        }

        // callables
        callables().defineCallables();

        emitter().emit("");
        emitter().emit("#endif");
        emitter().close();
    }

    /**
     * Copy the AOCL launch synthesis scripts
     *
     * @param path
     */
    default void copySynthesisScript(Path path) {
        emitter().open(path.resolve(path.resolve("aocl_synthesis.sh")));
        try (InputStream in = ClassLoader.getSystemResourceAsStream("aocl_backend_code/aocl_synthesis.sh")) {
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

    default void generateActor(CalActor actor) {
        String name = backend().instance().get().getInstanceName();

        List<String> parameters = new ArrayList<>();
        if (configuration().get(usePipes).booleanValue()) {
            backend().resolver().get().getIncomingsMap(name).entrySet().forEach(p -> {
                String parameter = code().inputPipeDeclaration(p.getValue(), p.getKey());
                parameters.add(parameter);
            });

            backend().resolver().get().getOutgoingsMap(name).entrySet().forEach(p -> {
                String parameter = code().outputPipeDeclaration(p.getValue(), p.getKey());
                parameters.add(parameter);
            });
        }


        emitter().emit("//__attribute__((autorun))"); // FIXME should be used when altera channels will be implemented
        emitter().emit("__attribute__((max_global_work_dim(0)))");
        emitter().emit("__attribute__((reqd_work_group_size(1,1,1)))");
        emitter().emit("__kernel void %s(%s){", name, String.join(", ", parameters));
        emitter().emit("");
        emitter().increaseIndentation();

        // state variables
        generateStateVariables(actor);
        // process description
        generateProcessDescription(actor.getProcessDescription());

        emitter().decreaseIndentation();
        emitter().emit("}");
    }

    default void generateStateVariables(CalActor actor) {
        if (!actor.getVarDecls().isEmpty()) emitter().emit("// state variables");
        actor.getVarDecls().stream().filter(var -> !(var.getType() instanceof FunctionTypeExpr)).forEach(localVar -> {
            String decl = code().declaration(types().declaredType(localVar), backend().variables().declarationName(localVar));
            String value = localVar.getValue() != null ? code().evaluate(localVar.getValue()) : defaultValues().defaultValue(types().declaredType(localVar));
            emitter().emit("%s = %s;", decl, value);
        });

        emitter().emit("");
    }

    default void generateProcessDescription(ProcessDescription process) {
        if (process.isRepeated()) {
            emitter().emit("while(1){");
            emitter().increaseIndentation();
        }

        emitter().emit("// process body");
        process.getStatements().forEach(backend().code()::execute);

        if (process.isRepeated()) {
            emitter().decreaseIndentation();
            emitter().emit("}");
        }
    }
}
