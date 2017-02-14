package xyz.exelixi.backend.opencl.aocl.codegen;

/**
 * Created by scb on 2/14/17.
 */

import org.multij.Binding;
import org.multij.Module;
import se.lth.cs.tycho.phases.cbackend.Emitter;
import xyz.exelixi.backend.opencl.aocl.AoclBackendCore;

import java.nio.file.Path;

import static org.multij.BindingKind.MODULE;

@Module
public interface SharedConstant {

    @Binding(MODULE)
    AoclBackendCore backend();

    default Emitter emitter() {
        return backend().emitter();
    }


    /**
     * Generate the sharedconstants.h file with contains all the shared host-device constant definition
     *
     * @param path
     */
    default void generateSharedParameters(Path path) {
        emitter().open(path.resolve(path.resolve("sharedconstants.h")));
        emitter().emit("#ifndef SHAREDCONSTANTS_H");
        emitter().emit("#define SHAREDCONSTANTS_H");
        emitter().emit("");
        emitter().emit("#define FIFO_DEPTH 512");
        emitter().emit("");
        emitter().emit("#endif");
        emitter().close();
    }


}
