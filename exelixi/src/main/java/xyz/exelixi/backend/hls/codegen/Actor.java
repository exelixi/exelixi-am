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
import se.lth.cs.tycho.comp.SourceUnit;
import se.lth.cs.tycho.ir.decl.GlobalEntityDecl;
import se.lth.cs.tycho.ir.network.Instance;
import se.lth.cs.tycho.phases.cbackend.Emitter;
import xyz.exelixi.backend.hls.HlsBackendCore;

import java.nio.file.Path;

/**
 * Vivado HLS Actor Code Generation
 */

@Module
public interface Actor {
    @Binding
    HlsBackendCore backend();

    default Emitter emitter() {
        return backend().emitter();
    }

    default Preprocessor preprocessor() {
        return backend().preprocessor();
    }

    default Structure structure() {
        return backend().structure();
    }

    default void generateSourceCode(Instance instance, Path path) {
        backend().instance().set(instance);
        GlobalEntityDecl actor = backend().task().getSourceUnits().stream()
                .map(SourceUnit::getTree)
                .filter(ns -> ns.getQID().equals(instance.getEntityName().getButLast()))
                .flatMap(ns -> ns.getEntityDecls().stream())
                .filter(decl -> decl.getName().equals(instance.getEntityName().getLast().toString()))
                .findFirst().get();
        String fileNameBase = instance.getInstanceName();
        // -- Filename
        String fileName = fileNameBase + ".cpp";

        // -- Open file for code generation
        emitter().open(path.resolve(fileName));

        // -- File Notice
        backend().fileNotice().generateCNotice("Source code for Instance: " + instance.getInstanceName());
        emitter().emit("");

        // -- Includes
        preprocessor().systemInclude("stdint.h");
        preprocessor().systemInclude("hls_stream.h");
        preprocessor().userInclude("global.h");
        preprocessor().userInclude(fileNameBase + ".h");
        emitter().emit("");

        // -- Actor Structure Declaration
        structure().actorDecl(actor);

        // -- Close the File
        emitter().close();
        backend().instance().clear();
    }

    default void generateHeaderCode(Instance instance, Path path) {
        backend().instance().set(instance);
        GlobalEntityDecl actor = backend().task().getSourceUnits().stream()
                .map(SourceUnit::getTree)
                .filter(ns -> ns.getQID().equals(instance.getEntityName().getButLast()))
                .flatMap(ns -> ns.getEntityDecls().stream())
                .filter(decl -> decl.getName().equals(instance.getEntityName().getLast().toString()))
                .findFirst().get();
        String fileNameBase = instance.getInstanceName();
        // -- Filename
        String fileName = fileNameBase + ".h";

        // -- Open file for code generation
        emitter().open(path.resolve(fileName));
        backend().fileNotice().generateCNotice("Header code for Actor: " + instance.getInstanceName());
        emitter().emit("");
        // -- Preprocessor, ifndef, define
        preprocessor().ifndef(fileNameBase);
        preprocessor().define(fileNameBase);

        // -- Includes
        preprocessor().systemInclude("hls_stream.h");
        preprocessor().systemInclude("stdint.h");
        emitter().emit("");

        structure().actorDeclHeader(actor);

        // -- ENDIF
        preprocessor().endif();

        // -- Close the File
        emitter().close();
        backend().instance().clear();
    }

}
