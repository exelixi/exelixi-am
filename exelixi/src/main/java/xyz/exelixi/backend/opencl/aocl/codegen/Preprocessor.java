package xyz.exelixi.backend.opencl.aocl.codegen;

import org.multij.Binding;
import org.multij.Module;
import se.lth.cs.tycho.phases.cbackend.Emitter;
import xyz.exelixi.backend.opencl.aocl.AoclBackendCore;

/**
 * C Preprocessor
 *
 * @author Simone Casale-Brunet
 */
@Module
public interface Preprocessor {

    @Binding
    AoclBackendCore backend();

    default Emitter emitter() {
        return backend().emitter();
    }

    // ------------------------------------------------------------------------
    // -- IF DEF

    default void ifndef(String token) {
        emitter().emit("#ifndef __%s__", token.toUpperCase());
    }

    default void define(String token, String value) {
        emitter().emit("#define %s %s", token.toUpperCase(), value);
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
