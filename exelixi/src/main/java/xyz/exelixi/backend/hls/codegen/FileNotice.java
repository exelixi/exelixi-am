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
import se.lth.cs.tycho.phases.cbackend.Emitter;
import xyz.exelixi.backend.hls.HlsBackendCore;

/**
 * File Notice
 *
 * @author Endri Bezati
 */
@Module
public interface FileNotice {
    @Binding
    HlsBackendCore backend();

    default Emitter emitter() {
        return backend().emitter();
    }

    default void generateCNotice(String message) {
        generateNotice("/", message);
    }

    default void generateTCLNotice(String message) {
        generateNotice("#", message);
    }

    default void generateNotice(String comment, String message) {
        emitter().emit("%s%s ----------------------------------------------------------------------------", comment, comment);
        emitter().emit("%s%s  _____          _ _      _ ", comment, comment);
        emitter().emit("%s%s | ____|_  _____| (_)_  _(_)", comment, comment);
        emitter().emit("%s%s |  _| \\ \\/ / _ \\ | \\ \\/ / |", comment, comment);
        emitter().emit("%s%s | |___ >  <  __/ | |>  <| |", comment, comment);
        emitter().emit("%s%s |_____/_/\\_\\___|_|_/_/\\_\\_|", comment, comment);
        emitter().emit("%s%s ----------------------------------------------------------------------------", comment, comment);
        emitter().emit("%s%s -- Exelixi Dataflow Code Generation for Vivado HLS", comment, comment);
        emitter().emit("%s%s -- Based on Tycho Compiler", comment, comment);
        emitter().emit("%s%s ----------------------------------------------------------------------------", comment, comment);
        emitter().emit("%s%s -- This file is generated automatically by Exelixi HLS, please do not modify", comment, comment);
        emitter().emit("%s%s -- %s", comment, comment, message);
        emitter().emit("%s%s ----------------------------------------------------------------------------", comment, comment);
    }
}
