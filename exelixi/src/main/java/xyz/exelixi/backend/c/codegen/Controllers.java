package xyz.exelixi.backend.c.codegen;

import org.multij.Binding;
import org.multij.Module;
import se.lth.cs.tycho.ir.entity.am.ActorMachine;
import se.lth.cs.tycho.ir.entity.am.ctrl.*;
import xyz.exelixi.backend.c.CBackendCore;

import java.util.*;

@Module
public interface Controllers {
    @Binding
    CBackendCore backend();

    default Emitter emitter() {
        return backend().emitter();
    }


    default void emitControllerPrototype(String name, ActorMachine actorMachine) {
        emitter().emit("_Bool %s_run(%1$s_state *self);", name);
    }

    default void emitController(String name, ActorMachine actorMachine) {
        List<? extends State> stateList = actorMachine.controller().getStateList();
        Map<State, Integer> stateMap = stateMap(stateList);
        Set<State> waitTargets = collectWaitTargets(stateList);

        emitter().emit("_Bool %s_run(%1$s_state *self) {", name);
        emitter().increaseIndentation();

        emitter().emit("_Bool progress = false;");
        emitter().emit("");

        jumpInto(waitTargets.stream().mapToInt(stateMap::get).collect(BitSet::new, BitSet::set, BitSet::or));

        for (State s : stateList) {
            emitter().emit("S%d:", stateMap.get(s));
            Instruction instruction = s.getInstructions().get(0);
            backend().scopes().init(actorMachine, instruction).stream().forEach(scope ->
                    emitter().emit("%s_init_scope_%d(self);", name, scope)
            );
            emitInstruction(name, instruction, stateMap);
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

    void emitInstruction(String name, Instruction instruction, Map<State, Integer> stateNumbers);

    default void emitInstruction(String name, Test test, Map<State, Integer> stateNumbers) {
        emitter().emit("if (%s_condition_%d(self)) {", name, test.condition());
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

    default void emitInstruction(String name, Wait wait, Map<State, Integer> stateNumbers) {
        emitter().emit("self->program_counter = %d;", stateNumbers.get(wait.target()));
        emitter().emit("return progress;");
        emitter().emit("");
    }

    default void emitInstruction(String name, Exec exec, Map<State, Integer> stateNumbers) {
        emitter().emit("%s_transition_%d(self);", name, exec.transition());
        emitter().emit("progress = true;");
        emitter().emit("goto S%d;", stateNumbers.get(exec.target()));
        emitter().emit("");
    }

    default void jumpInto(BitSet waitTargets) {
        emitter().emit("switch (self->program_counter) {");
        waitTargets.stream().forEach(s -> emitter().emit("case %d: goto S%1$d;", s));
        emitter().emit("}");
        emitter().emit("");
    }

    default Set<State> collectWaitTargets(List<? extends State> stateList) {
        Set<State> targets = new HashSet<>();
        for (State state : stateList) {
            Instruction i = state.getInstructions().get(0);
            if (i.getKind() == InstructionKind.WAIT) {
                i.forEachTarget(targets::add);
            }
        }
        return targets;
    }

}
