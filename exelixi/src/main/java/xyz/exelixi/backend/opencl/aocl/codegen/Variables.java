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
import se.lth.cs.tycho.ir.IRNode;
import se.lth.cs.tycho.ir.NamespaceDecl;
import se.lth.cs.tycho.ir.QID;
import se.lth.cs.tycho.ir.Variable;
import se.lth.cs.tycho.ir.decl.ClosureVarDecl;
import se.lth.cs.tycho.ir.decl.VarDecl;
import se.lth.cs.tycho.ir.entity.am.ActorMachine;
import se.lth.cs.tycho.ir.entity.am.Scope;
import se.lth.cs.tycho.ir.expr.ExprGlobalVariable;
import se.lth.cs.tycho.ir.expr.ExprRef;
import xyz.exelixi.backend.opencl.aocl.AoclBackendCore;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.multij.BindingKind.MODULE;

/**
 * @author Simone Casale-Brunet
 */
@Module
public interface Variables {

    @Binding(MODULE)
    AoclBackendCore backend();

    default String generateTemp() {
        return "t_" + backend().uniqueNumbers().next();
    }

    default String declarationName(VarDecl decl) {
        IRNode parent = backend().tree().parent(decl);
        if (parent instanceof Scope) {
            return "a_" + decl.getName();
        } else if (parent instanceof ActorMachine) {
            return "a_" + decl.getName();
        } else if (parent instanceof NamespaceDecl) {
            QID ns = ((NamespaceDecl) parent).getQID();
            return Stream.concat(ns.parts().stream(), Stream.of(decl.getName()))
                    .collect(Collectors.joining("_", "g_", ""));
        } else {
            return "f_" + decl.getName();
        }
    }

    default String globalName(ExprGlobalVariable var) {
        return var.getGlobalName().parts().stream()
                .collect(Collectors.joining("_", "g_", ""));
    }

    default String reference(VarDecl decl) {
        IRNode parent = backend().tree().parent(decl);
        if (parent instanceof Scope || parent instanceof ActorMachine) {
            return "&(self->" + declarationName(decl) + ")";
        } else {
            return "&" + declarationName(decl);
        }
    }

    default String name(Variable var) {
        VarDecl decl = backend().names().declaration(var);
        IRNode parent = backend().tree().parent(decl);
        if (decl instanceof ClosureVarDecl) {
            return declarationName(decl);
        } else if (parent instanceof Scope || parent instanceof ActorMachine) {
            return declarationName(decl);
        } else {
            return declarationName(decl);
        }
    }
}
