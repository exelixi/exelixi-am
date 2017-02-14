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

package xyz.exelixi.backend.opencl.aocl.codegen;

import org.multij.Binding;
import org.multij.Module;
import se.lth.cs.tycho.phases.cbackend.Emitter;
import xyz.exelixi.backend.opencl.aocl.AoclBackendCore;

/**
 * File Notice (licence template)
 *
 * @author Simone Casale-Brunet
 */
@Module
public interface FileNotice {
    @Binding
    AoclBackendCore backend();

    default Emitter emitter() {
        return backend().emitter();
    }

    /**
     * Append to the current opened emitter a file notice containing the licence template
     * @param message
     */
    default void generateNotice(String message) {
        emitter().emit("// ----------------------------------------------------------------------------");
        emitter().emit("//   _____          _ _      _ ");
        emitter().emit("//  | ____|_  _____| (_)_  _(_)");
        emitter().emit("//  |  _| \\ \\/ / _ \\ | \\ \\/ / |");
        emitter().emit("//  | |___ >  <  __/ | |>  <| |");
        emitter().emit("//  |_____/_/\\_\\___|_|_/_/\\_\\_|");
        emitter().emit("//  ----------------------------------------------------------------------------");
        emitter().emit("//  -- Exelixi Dataflow Code Generation for Altera OpenCL HLS");
        emitter().emit("//  ----------------------------------------------------------------------------");
        emitter().emit("//  -- This file is generated automatically by Exelixi AM, please do not modify");
        emitter().emit("//  -- %s", message);
        emitter().emit("//  ----------------------------------------------------------------------------");
        emitter().emit("");
    }
}
