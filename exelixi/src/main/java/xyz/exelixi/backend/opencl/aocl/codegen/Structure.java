/*
 * EXELIXI
 *
 * Copyright (C) 2017 Endri Bezati, EPFL SCI-STI-MM
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
import se.lth.cs.tycho.ir.decl.GlobalEntityDecl;
import se.lth.cs.tycho.ir.decl.VarDecl;
import se.lth.cs.tycho.ir.entity.Entity;
import se.lth.cs.tycho.ir.entity.cal.CalActor;
import se.lth.cs.tycho.ir.entity.cal.ProcessDescription;
import se.lth.cs.tycho.phases.attributes.Names;
import se.lth.cs.tycho.phases.attributes.Types;
import se.lth.cs.tycho.phases.cbackend.Emitter;
import xyz.exelixi.backend.opencl.aocl.AoclBackendCore;

import java.util.ArrayList;
import java.util.List;

/**
 * Altera OpenCL Backend, Structure
 *
 * @author Simone Casale-Brunet
 */

@Module
public interface Structure {

    @Binding
    AoclBackendCore backend();

    default Emitter emitter() {
        return backend().emitter();
    }

    default Types types() {
        return backend().types();
    }

    default Code code() {
        return backend().code();
    }

    default Names names() {
        return backend().names();
    }

    default DefaultValues defVals() {
        return backend().defaultValues();
    }

    default void actorGeneration(GlobalEntityDecl decl) {
        String name = backend().instance().get().getInstanceName();
        System.out.println("generating source code for actor: " + name);

        Entity entity = decl.getEntity();
        if(!(entity instanceof  CalActor)){
            throw new Error("unsupported actor type: only processes are supported");
        }

        actorGeneration((CalActor) entity);
    }


    default void actorGeneration(CalActor actor) {
        String name = backend().instance().get().getInstanceName();

        List<String> parameters = new ArrayList<>();
        actor.getInputPorts().forEach(x -> parameters.add(code().inputPortDeclaration(x)));
        actor.getOutputPorts().forEach(x -> parameters.add(code().outputPortDeclaration(x)));

        emitter().emit("#include \"global.h\"");
        emitter().emit("");


        emitter().emit("__kernel void %s(%s){", name, String.join(", ", parameters));
        emitter().emit("");
        emitter().increaseIndentation();

        // state variables
        calActorStateVariables(actor);
        // process description
        processDescription(actor.getProcessDescription());

        emitter().decreaseIndentation();
        emitter().emit("}");
    }

    default void calActorStateVariables(CalActor actor) {
        if(!actor.getVarDecls().isEmpty()) emitter().emit("// state variables");
        for (VarDecl var : actor.getVarDecls()) {
            String decl = code().declaration(types().declaredType(var), backend().variables().declarationName(var));
            emitter().emit("%s;", decl);
        }
        emitter().emit("");
    }

    default void processDescription(ProcessDescription process) {
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
