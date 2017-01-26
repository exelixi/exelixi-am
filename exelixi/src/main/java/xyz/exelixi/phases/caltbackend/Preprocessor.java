package xyz.exelixi.phases.caltbackend;

import org.multij.Binding;
import org.multij.Module;

/**
 * C Preprocessor
 *
 * @author Endri Bezati
 */
@Module
public interface Preprocessor {

    @Binding
    ExelixiBackend backend();

    default Emitter emitter() {
        return backend().emitter();
    }


    default void preprocessor_ifndef(String token) {
        emitter().emit("#ifndef __%s__", token.toUpperCase());
    }

    default void preprocessor_define(String token) {
        emitter().emit("#define __%s__", token.toUpperCase());
        emitter().emit("");
    }

    default void preprocessor_endif() {
        emitter().emit("#endif");
    }

    default void preprocessor_system_include(String token) {
        emitter().emit("#include <%s.h>", token);
    }

    default void preprocessor_user_include(String token) {
        emitter().emit("#include \"%s.h\"", token);
    }

    default void preprocessor_pragma(String token) {
        emitter().emit("#pragma %s", token);
    }
}
