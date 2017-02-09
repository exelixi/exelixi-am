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
import se.lth.cs.tycho.ir.entity.PortDecl;
import se.lth.cs.tycho.ir.entity.am.ActorMachine;
import se.lth.cs.tycho.ir.entity.am.Condition;
import se.lth.cs.tycho.ir.entity.am.PortCondition;
import se.lth.cs.tycho.ir.entity.am.Transition;
import se.lth.cs.tycho.ir.entity.am.ctrl.*;
import se.lth.cs.tycho.phases.attributes.Types;
import se.lth.cs.tycho.phases.cbackend.Emitter;
import xyz.exelixi.backend.hls.HlsBackendCore;
import xyz.exelixi.utils.PortOrderComparator;

import java.util.*;

/**
 * Actor machine controllers for Vivado HLS
 *
 * @author Endri Bezati
 * @author Gustav
 */
@Module
public interface Controllers {
    @Binding
    HlsBackendCore backend();

    default Emitter emitter() {
        return backend().emitter();
    }

    default Types types() {
        return backend().types();
    }

    default String emitControllerPrototype(String name, ActorMachine actorMachine) {
        String result = "";
        List<PortDecl> inputs = actorMachine.getInputPorts();
        List<PortDecl> outputs = actorMachine.getOutputPorts();

        List<String> parameters = backend().code().actorMachineIO(actorMachine);

        parameters.addAll(backend().structure().getEntityInitParameters(actorMachine));

        String resultType = "void";
        result += resultType;
        result += " ";
        result += name;
        result += "(";
        result += String.join(", ", parameters);
        result += ")";

        return result;
    }

    default void emitControllerHeader(String name, ActorMachine actorMachine) {
        emitter().emit(emitControllerPrototype(name, actorMachine) + ";");
    }

    default void emitController(String name, ActorMachine actorMachine) {
        emitter().emit("// ----------------------------------------------------------------------------");
        emitter().emit("// -- Controller");
        emitter().emit("");
        emitter().emit(emitControllerPrototype(name, actorMachine) + "{");
        List<? extends State> stateList = actorMachine.controller().getStateList();
        Map<State, Integer> stateMap = stateMap(stateList);
        Set<State> waitTargets = collectWaitTargets(stateList);

        emitter().increaseIndentation();
        jumpInto(waitTargets.stream().mapToInt(stateMap::get).collect(BitSet::new, BitSet::set, BitSet::or));
        /*
        emitter().emit("INIT:");
        emitter().increaseIndentation();
        List<String> arguments = new ArrayList<String>();

        actorMachine.getValueParameters().forEach(d -> {
            arguments.add(backend().variables().declarationName(d));
        });

        emitter().emit("%s_init_actor(%s);", name, String.join(", ", arguments));
        emitter().emit("goto S%d;", stateMap.get(stateList.get(0)));
        emitter().emit("");
        */
        //emitter().decreaseIndentation();
        for (State s : stateList) {
            emitter().emit("S%d:", stateMap.get(s));
            Instruction instruction = s.getInstructions().get(0);
            backend().scopes().init(actorMachine, instruction).stream().forEach(scope ->
                    emitter().emit("%s_init_scope_%d(%s);", name, scope, String.join(", ", backend().code().scopeIONames(actorMachine.getScopes().get(scope))))
            );
            emitInstruction(name, instruction, stateMap, actorMachine);
        }

        emitter().decreaseIndentation();
        emitter().emit("}");
    }

    default Map<State, Integer> stateMap(List<? extends State> stateList) {
        int i = 0;
        Map<State, Integer> result = new HashMap<>();
        for (State s : stateList) {
            result.put(s, i++);
        }
        return result;
    }

    void emitInstruction(String name, Instruction instruction, Map<State, Integer> stateNumbers, ActorMachine actorMachine);

    default void emitInstruction(String name, Test test, Map<State, Integer> stateNumbers, ActorMachine actorMachine) {
        Condition condition = actorMachine.getCondition(test.condition());
        List<String> argument = new ArrayList<String>();
        if (condition instanceof PortCondition) {
            PortCondition portCondition = (PortCondition) condition;
            argument.add(portCondition.getPortName().getName());
            String count = portCondition.getPortName().getName() + "_count";
            argument.add(count);
        }
        emitter().emit("if (%s_condition_%d(%s)) {", name, test.condition(), String.join(", ",argument));
        emitter().increaseIndentation();
        emitter().emit("goto S%d;", stateNumbers.get(test.targetTrue()));
        emitter().decreaseIndentation();
        emitter().emit("} else {");
        emitter().increaseIndentation();
        emitter().emit("goto S%d;", stateNumbers.get(test.targetFalse()));
        emitter().decreaseIndentation();
        emitter().emit("}");
        emitter().emit("");
    }

    default void emitInstruction(String name, Wait wait, Map<State, Integer> stateNumbers, ActorMachine actorMachine) {
        emitter().emit("program_counter = %d;", stateNumbers.get(wait.target()));
        emitter().emit("return;");
        emitter().emit("");
    }

    default void emitInstruction(String name, Exec exec, Map<State, Integer> stateNumbers, ActorMachine actorMachine) {
        Transition transition = actorMachine.getTransitions().get(exec.transition());
        List<String> arguments = backend().code().transitionIOName(transition, false);
        List<String> actorMachineIO = backend().code().actorMachineIOName(actorMachine, false);
        arguments.sort(new PortOrderComparator(actorMachineIO));
        emitter().emit("%s_transition_%d(%s);", name, exec.transition(), String.join(", ", arguments));
        emitter().emit("program_counter = %d;", stateNumbers.get(exec.target()));
        emitter().emit("return;");
        emitter().emit("");
    }

    default void jumpInto(BitSet waitTargets) {
        emitter().emit("switch (program_counter) {");
        //emitter().emit("case -1: goto INIT;");
        waitTargets.stream().forEach(s -> emitter().emit("case %d: goto S%1$d;", s));
        emitter().emit("}");
        emitter().emit("");
    }

    default Set<State> collectWaitTargets(List<? extends State> stateList) {
        Set<State> targets = new HashSet<>();
        for (State state : stateList) {
            Instruction i = state.getInstructions().get(0);
            if (i.getKind() == InstructionKind.WAIT || i.getKind() == InstructionKind.EXEC) {
                i.forEachTarget(targets::add);
            }
        }
        return targets;
    }


}
