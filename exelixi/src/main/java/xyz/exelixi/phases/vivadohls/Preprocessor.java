package xyz.exelixi.phases.vivadohls;

import org.multij.Binding;
import org.multij.Module;
import se.lth.cs.tycho.phases.cbackend.Emitter;

/**
 * C Preprocessor
 *
 * @author Endri Bezati
 */
@Module
public interface Preprocessor {

    @Binding
    VivadoHLSBackend backend();

    default Emitter emitter() {
        return backend().emitter();
    }

    // ------------------------------------------------------------------------
    // -- IF DEF

    default void ifndef(String token) {
        emitter().emit("#ifndef __%s__", token.toUpperCase());
    }

    default void define(String token) {
        emitter().emit("#define __%s__", token.toUpperCase());
        emitter().emit("");
    }

    default void endif() {
        emitter().emit("#endif");
    }

    // ------------------------------------------------------------------------
    // -- INCLUDES

    default void systemInclude(String header) {
        emitter().emit("#include <%s>", header);
    }

    default void userInclude(String header) {
        emitter().emit("#include \"%s\"", header);
    }

    // ------------------------------------------------------------------------
    // -- PRAGMAS

    default void pragma(String token) {
        emitter().emit("#pragma %s", token);
    }
}
