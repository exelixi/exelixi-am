package xyz.exelixi.interp;

import org.multij.Binding;
import org.multij.Module;
import se.lth.cs.tycho.comp.CompilationTask;
import se.lth.cs.tycho.comp.Context;
import se.lth.cs.tycho.comp.UniqueNumbers;
import se.lth.cs.tycho.ir.network.Instance;
import se.lth.cs.tycho.phases.TreeShadow;
import se.lth.cs.tycho.phases.attributes.*;
import se.lth.cs.tycho.phases.cbackend.Emitter;
import se.lth.cs.tycho.phases.cbackend.util.Box;

import static org.multij.BindingKind.INJECTED;
import static org.multij.BindingKind.LAZY;

/**
 *
 * @author Endri Bezati
 */

@Module
public interface ExelixiInterpreterCore {

    // -- Attributes
    @Binding(INJECTED)
    CompilationTask task();

    @Binding(INJECTED)
    Context context();

    @Binding(LAZY)
    default Box<Instance> instance() {
        return Box.empty();
    }

    @Binding(LAZY)
    default Emitter emitter() {
        return new Emitter();
    }

    @Binding(LAZY)
    default Types types() {
        return context().getAttributeManager().getAttributeModule(Types.key, task());
    }

    @Binding(LAZY)
    default ConstantEvaluator constants() {
        return context().getAttributeManager().getAttributeModule(ConstantEvaluator.key, task());
    }

    @Binding(LAZY)
    default Names names() {
        return context().getAttributeManager().getAttributeModule(Names.key, task());
    }

    @Binding(LAZY)
    default GlobalNames globalNames() {
        return context().getAttributeManager().getAttributeModule(GlobalNames.key, task());
    }

    @Binding(LAZY)
    default UniqueNumbers uniqueNumbers() {
        return context().getUniqueNumbers();
    }

    @Binding(LAZY)
    default TreeShadow tree() {
        return context().getAttributeManager().getAttributeModule(TreeShadow.key, task());
    }

    @Binding(LAZY)
    default ActorMachineScopes scopes() {
        return context().getAttributeManager().getAttributeModule(ActorMachineScopes.key, task());
    }

}
