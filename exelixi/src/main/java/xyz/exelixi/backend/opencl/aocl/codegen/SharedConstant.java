/*
 * EXELIXI
 *
 * Copyright (C) 2017 EPFL SCI-STI-MM
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
import se.lth.cs.tycho.phases.cbackend.Emitter;
import xyz.exelixi.backend.opencl.aocl.AoclBackendCore;

import java.nio.file.Path;

import static org.multij.BindingKind.MODULE;

/**
 * @author Simone Casale-Brunet
 */
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
