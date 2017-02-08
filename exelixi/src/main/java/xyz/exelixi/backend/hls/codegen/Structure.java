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

package xyz.exelixi.backend.hls.codegen;

import org.multij.Binding;
import org.multij.Module;
import se.lth.cs.tycho.ir.Port;
import se.lth.cs.tycho.ir.decl.GlobalEntityDecl;
import se.lth.cs.tycho.ir.decl.VarDecl;
import se.lth.cs.tycho.ir.entity.Entity;
import se.lth.cs.tycho.ir.entity.PortDecl;
import se.lth.cs.tycho.ir.entity.am.*;
import se.lth.cs.tycho.phases.attributes.Names;
import se.lth.cs.tycho.phases.attributes.Types;
import se.lth.cs.tycho.phases.cbackend.Emitter;
import se.lth.cs.tycho.types.CallableType;
import se.lth.cs.tycho.types.Type;
import xyz.exelixi.backend.hls.HlsBackendCore;
import xyz.exelixi.utils.PortOrderComparator;

import java.util.ArrayList;
import java.util.List;

/**
 * Vivado HLS Backend, Structure
 *
 * @author Endri Bezati
 */

@Module
public interface Structure {
    @Binding
    HlsBackendCore backend();

    default Emitter emitter() {
        return backend().emitter();
    }

    default Types types() {
        return backend().types();
    }

    default Controllers controllers() {
        return backend().controllers();
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

    // ------------------------------------------------------------------------
    // -- Source

    default void actorDecl(GlobalEntityDecl decl) {
        String name = backend().instance().get().getInstanceName();
        actor(name, decl.getEntity());
    }

    default void actor(String name, Entity entity) {
    }

    // -- Actor Structure
    default void actor(String name, ActorMachine actorMachine) {
        actorMachineState(name, actorMachine);
        actorMachineScopes(name, actorMachine);
        //actorMachineInit(name, actorMachine);
        actorMachineTransitions(name, actorMachine);
        actorMachineConditions(name, actorMachine);
        actorMachineController(name, actorMachine);
    }

    // ------------------------------------------------------------------------
    // -- Header

    default void actorDeclHeader(GlobalEntityDecl decl) {
        String name = backend().instance().get().getInstanceName();
        actorHeader(name, decl.getEntity());
    }

    default void actorHeader(String name, Entity entity) {
    }

    default void actorHeader(String name, ActorMachine actorMachine) {
        controllers().emitControllerHeader(name, actorMachine);
    }


    // ------------------------------------------------------------------------
    // -- State variables

    default void actorMachineState(String name, ActorMachine actorMachine) {
        emitter().emit("// ----------------------------------------------------------------------------");
        emitter().emit("// -- State Variables");
        emitter().emit("");


        if (!actorMachine.getValueParameters().isEmpty()) {
            emitter().emit("// -- parameters");

            for (VarDecl param : actorMachine.getValueParameters()) {
                String decl = code().declaration(types().declaredType(param), backend().variables().declarationName(param));
                emitter().emit("static %s;", decl);
            }
            emitter().emit("");
        }

        int i = 0;
        for (Scope scope : actorMachine.getScopes()) {
            emitter().emit("// -- Scope %d", i);
            backend().callables().declareEnvironmentForCallablesInScope(scope);
            for (VarDecl var : scope.getDeclarations()) {
                if (scope.isPersistent()) {
                    String decl = "";
                    if (actorMachine.getValueParameters().contains(var)) {
                        // -- FIXME : Find out that a variable is initalized by a parameter
                    } else {
                        decl = code().declaration(types().declaredType(var), backend().variables().declarationName(var));
                    }
                    if(var.getValue() != null) {
                        String value = code().evaluate(var.getValue());
                        if(var.isConstant()){
                            backend().preprocessor().defineDeclaration(backend().variables().declarationName(var), value);
                        }else {
                            emitter().emit("static %s = %s;", decl, value);
                        }
                    }else{
                        emitter().emit("static %s;", decl);
                    }
                } else {
                    String decl = code().declaration(types().declaredType(var), backend().variables().declarationName(var));
                    emitter().emit("static %s;", decl);
                }
            }
            emitter().emit("");
            i++;
        }

        emitter().emit("// -- Actor Machine Program Counter");
        emitter().emit("static int program_counter = 0;");
        emitter().emit("");
    }

    default void actorMachineScopes(String name, ActorMachine actorMachine) {
        if (!actorMachine.getScopes().isEmpty()) {
            emitter().emit("// ----------------------------------------------------------------------------");
            emitter().emit("// -- Scopes");
            emitter().emit("");
        }
        int i = 0;
        for (Scope scope : actorMachine.getScopes()) {
            if (!scope.isPersistent()) {
                emitter().emit("static void %s_init_scope_%d(%s) {", name, i, String.join(", ", code().scopeIOArguments(scope)));
                emitter().increaseIndentation();
                for (VarDecl var : scope.getDeclarations()) {
                    Type type = types().declaredType(var);
                    if (var.isExternal() && type instanceof CallableType) {
                        String wrapperName = backend().callables().externalWrapperFunctionName(var);
                        String variableName = backend().variables().declarationName(var);
                        String t = backend().callables().mangle(type).encode();
                        emitter().emit("%s = (%s) { *%s, NULL };", variableName, t, wrapperName);
                    } else if (var.getValue() != null) {
                        emitter().emit("{");
                        emitter().increaseIndentation();
                        code().assign(types().declaredType(var), "" + backend().variables().declarationName(var), var.getValue());
                        emitter().decreaseIndentation();
                        emitter().emit("}");
                    }
                }
                emitter().decreaseIndentation();
                emitter().emit("}");
                emitter().emit("");
            }
            i++;
        }
    }


    // ------------------------------------------------------------------------
    // -- Actor Machine Initialization

    default void actorMachineInit(String name, ActorMachine actorMachine) {
        emitter().emit("// ----------------------------------------------------------------------------");
        emitter().emit("// -- Initialization");
        emitter().emit("");
        List<String> parameters = getEntityInitParameters(actorMachine);
        emitter().emit("static void %s_init_actor(%s) {", name, String.join(", ", parameters));
        emitter().increaseIndentation();
        emitter().emit("");

        if (!actorMachine.getValueParameters().isEmpty()) {
            emitter().emit("// -- parameters");
            actorMachine.getValueParameters().forEach(d -> {
                emitter().emit("%s = %1$s;", backend().variables().declarationName(d));
            });
            emitter().emit("");
        }
        emitter().emit("// -- init persistent scopes");
        int i = 0;
        for (Scope s : actorMachine.getScopes()) {
            if (s.isPersistent()) {
                emitter().emit("%s_init_scope_%d();", name, i);
            }
            i = i + 1;
        }
        emitter().decreaseIndentation();
        emitter().emit("}");
        emitter().emit("");
        emitter().emit("");
    }

    default List<String> getEntityInitParameters(Entity actorMachine) {
        List<String> parameters = new ArrayList<>();
        actorMachine.getValueParameters().forEach(d -> {
            parameters.add(code().declaration(types().declaredType(d), backend().variables().declarationName(d)));
        });
        return parameters;
    }

    // ------------------------------------------------------------------------
    // -- Transitions

    default void actorMachineTransitions(String name, ActorMachine actorMachine) {
        int i = 0;
        if (!actorMachine.getTransitions().isEmpty()) {
            emitter().emit("// ----------------------------------------------------------------------------");
            emitter().emit("// -- Transition%s", (actorMachine.getTransitions().size() > 1) ? "s" : "");
            emitter().emit("");
        }


        for (Transition transition : actorMachine.getTransitions()) {
            List<String> parameters = new ArrayList<String>(transition.getInputRates().size() + transition.getOutputRates().size());
            // -- Transitions has outputs
            if (!transition.getOutputRates().isEmpty()) {
                for (Port port : transition.getOutputRates().keySet()) {
                    // -- FIXME: Temporary Hack
                    for (PortDecl portDecl : actorMachine.getOutputPorts()) {
                        if (portDecl.getName().equals(port.getName())) {
                            parameters.add(backend().code().type(portDecl));
                        }
                    }
                }
            }

            List<String> actoMachineIO = backend().code().actorMachineIO(actorMachine);
            parameters.sort(new PortOrderComparator(actoMachineIO));

            emitter().emit("static void %s_transition_%d(%s) {", name, i, String.join(", ", parameters));
            backend().preprocessor().pragma("HLS INLINE");
            emitter().increaseIndentation();
            transition.getBody().forEach(backend().code()::execute);
            emitter().decreaseIndentation();
            emitter().emit("}");
            emitter().emit("");
            i++;
        }
    }

    // ------------------------------------------------------------------------
    // -- Conditions

    default void actorMachineConditions(String name, ActorMachine actorMachine) {
        if (!actorMachine.getConditions().isEmpty()) {
            emitter().emit("// ----------------------------------------------------------------------------");
            emitter().emit("// -- Condition%s", (actorMachine.getConditions().size() > 1) ? "s" : "");
            emitter().emit("");
        }
        int i = 0;
        for (Condition condition : actorMachine.getConditions()) {
            evaluateCondition(condition, name, i);
            i++;
        }
    }

    void evaluateCondition(Condition condition, String name, int i);

    default void evaluateCondition(PredicateCondition condition, String name, int i) {
        emitter().emit("static bool %s_condition_%d() {", name, i);
        emitter().increaseIndentation();
        emitter().emit("return %s;", backend().code().evaluate(condition.getExpression()));
        emitter().decreaseIndentation();
        emitter().emit("}");
        emitter().emit("");
    }

    default void evaluateCondition(PortCondition condition, String name, int i) {
        String parameterPort = backend().code().type(condition.getPortName());
        String parameterCount = "uint32_t " + condition.getPortName().getName() + "_count";
        emitter().emit("static bool %s_condition_%d(%s, %s) {", name, i, parameterPort, parameterCount);
        emitter().increaseIndentation();
        if (condition.isInputCondition()) {
            emitter().emit("return !%s.empty() && %s_count > %d;", condition.getPortName().getName(), condition.getPortName().getName(), condition.N());
        } else {
            // FIXME : Add FIFO SIZE
            emitter().emit("return !%s.full() && %s_count > %d;", condition.getPortName().getName(), condition.getPortName().getName(), condition.N());
        }
        emitter().decreaseIndentation();
        emitter().emit("}");
        emitter().emit("");
    }

    // ------------------------------------------------------------------------
    // -- Controller

    // -- Actor machine controller
    default void actorMachineController(String name, ActorMachine actorMachine) {
        controllers().emitController(name, actorMachine);
    }


}
