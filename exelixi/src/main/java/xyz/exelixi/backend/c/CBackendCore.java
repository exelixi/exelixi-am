package xyz.exelixi.backend.c;

import org.multij.Binding;
import org.multij.Module;
import se.lth.cs.tycho.comp.CompilationTask;
import se.lth.cs.tycho.comp.Context;
import se.lth.cs.tycho.comp.UniqueNumbers;
import se.lth.cs.tycho.ir.network.Instance;
import se.lth.cs.tycho.phases.TreeShadow;
import se.lth.cs.tycho.phases.attributes.ActorMachineScopes;
import se.lth.cs.tycho.phases.attributes.GlobalNames;
import se.lth.cs.tycho.phases.attributes.Names;
import se.lth.cs.tycho.phases.attributes.Types;
import xyz.exelixi.backend.c.codegen.*;

import java.io.Closeable;

import static org.multij.BindingKind.*;

/**
 * An Exelixi Backend
 *
 * @author Endri Bezati
 */

@Module
public interface CBackendCore extends Closeable {

    // Attributes
    @Binding(INJECTED)
    CompilationTask task();

    @Binding(INJECTED)
    Context context();

    @Binding(INJECTED)
    Instance instance();

    @Binding(INJECTED)
    Emitter emitter();

    @Override
    default void close() {
        emitter().close();
    }

    @Binding(LAZY)
    default Types types() {
        return context().getAttributeManager().getAttributeModule(Types.key, task());
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

    // Code generator
    @Binding(MODULE)
    Variables variables();

    @Binding(MODULE)
    Structure structure();

    @Binding(MODULE)
    Code code();

    @Binding(MODULE)
    Controllers controllers();

    @Binding(MODULE)
    CMainFunction main();

    @Binding(MODULE)
    MainNetwork mainNetwork();

    @Binding(MODULE)
    CMakeLists cmakeLists();

    @Binding(MODULE)
    Global global();

    @Binding(MODULE)
    DefaultValues defaultValues();

    @Binding(MODULE)
    Callables callables();

    @Binding(MODULE)
    ExelixiBasicChannels channels();

    @Binding(MODULE)
    Preprocessor preprocessor();

}
