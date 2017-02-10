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
import se.lth.cs.tycho.ir.decl.GlobalVarDecl;
import se.lth.cs.tycho.ir.decl.VarDecl;
import se.lth.cs.tycho.ir.expr.ExprList;
import se.lth.cs.tycho.phases.attributes.Types;
import se.lth.cs.tycho.phases.cbackend.Emitter;
import se.lth.cs.tycho.types.CallableType;
import se.lth.cs.tycho.types.ListType;
import se.lth.cs.tycho.types.Type;
import xyz.exelixi.backend.hls.HlsBackendCore;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Global Variable and Functions/procedures emitter
 *
 * @author Endri Bezati
 */

@Module
public interface Global {
    @Binding
    HlsBackendCore backend();

    // -- Helpers
    default Emitter emitter() {
        return backend().emitter();
    }

    default Code code() {
        return backend().code();
    }

    default Types types() {
        return backend().types();
    }

    default Preprocessor preprocessor() {
        return backend().preprocessor();
    }

    default Callables callables() {
        return backend().callables();
    }


    default void generateSourceCode(Path path) {
        String fileName = "global";
        // -- Open File
        emitter().open(path.resolve(fileName + ".cpp"));

        // -- File notice
        backend().fileNotice().generateCNotice("Source code for globals");

        // -- Includes
        preprocessor().userInclude(fileName + ".h");
        emitter().emit("");

        // -- Get Callables
        callables().defineCallables();

        // -- Close File
        emitter().close();
    }

    default void generateHeaderCode(Path path) {
        String fileName = "global";
        emitter().open(path.resolve(fileName + ".h"));

        // -- File notice
        backend().fileNotice().generateCNotice("Header code for globals");
        emitter().emit("");

        // -- Preprocessor, ifndef, define
        preprocessor().ifndef(fileName);
        preprocessor().define(fileName);

        // -- Includes
        preprocessor().systemInclude("stdint.h");

        // -- Global variables
        emitter().emit("");
        emitter().emit("// ----------------------------------------------------------------------------");
        emitter().emit("// -- Global Constant Declarations");
        emitter().emit("");
        globalVariableInitializer(getGlobalVarDecls());
        emitter().emit("");

        backend().callables().declareCallables();
        emitter().emit("");

        // globalVariableDeclarations(getGlobalVarDecls());

        // -- ENDIF
        preprocessor().endif();

        // -- Close File
        emitter().close();

    }


    default List<GlobalVarDecl> getGlobalVarDecls() {
        return backend().task()
                .getSourceUnits().stream()
                .flatMap(unit -> unit.getTree().getVarDecls().stream())
                .collect(Collectors.toList());
    }

    default void globalVariableDeclarations(List<GlobalVarDecl> varDecls) {
        for (VarDecl decl : varDecls) {
            Type type = types().declaredType(decl);
            String d = code().declaration(type, backend().variables().declarationName(decl));
            emitter().emit("%s;", d);
        }
    }

    default void globalVariableInitializer(List<GlobalVarDecl> varDecls) {

        for (VarDecl decl : varDecls) {
            Type type = types().declaredType(decl);
            if (decl.isExternal()) {
                // -- Not Supported
            } else {
                if (decl.isConstant()) {
                    if (type instanceof ListType) {
                        ListType t = (ListType) type;
                        String declaration = code().declaration(t, backend().variables().declarationName(decl));
                        emitter().emit("%s = %s", declaration, code().evaluate(decl.getValue()));
                    } else {
                        preprocessor().defineDeclaration(backend().variables().declarationName(decl), code().evaluate(decl.getValue()));
                    }
                }
            }
        }

    }

}
