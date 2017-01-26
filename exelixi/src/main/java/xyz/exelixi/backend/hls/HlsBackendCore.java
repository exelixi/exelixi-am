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

package xyz.exelixi.backend.hls;

import org.multij.Binding;
import org.multij.Module;
import se.lth.cs.tycho.comp.CompilationTask;
import se.lth.cs.tycho.comp.Context;
import se.lth.cs.tycho.comp.UniqueNumbers;
import se.lth.cs.tycho.ir.network.Instance;
import se.lth.cs.tycho.phases.TreeShadow;
import se.lth.cs.tycho.phases.attributes.*;
import se.lth.cs.tycho.phases.cbackend.Emitter;
import se.lth.cs.tycho.phases.cbackend.util.Box;
import xyz.exelixi.backend.hls.codegen.*;

import static org.multij.BindingKind.*;


/**
 * A Vivado HLS Backend
 *
 * @author Endri Bezati
 */
@Module
public interface HlsBackendCore {

    // -- Attributes
    @Binding(INJECTED)
    CompilationTask task();

    @Binding(INJECTED)
    Context context();

    @Binding(LAZY)
    default Box<Instance> instance() {
        return Box.empty();
    }

    @Binding(LAZY)
    default Emitter emitter() {
        return new Emitter();
    }

    @Binding(LAZY)
    default Types types() {
        return context().getAttributeManager().getAttributeModule(Types.key, task());
    }

    @Binding(LAZY)
    default ConstantEvaluator constants() {
        return context().getAttributeManager().getAttributeModule(ConstantEvaluator.key, task());
    }

    @Binding(LAZY)
    default Names names() {
        return context().getAttributeManager().getAttributeModule(Names.key, task());
    }

    @Binding(LAZY)
    default GlobalNames globalNames() {
        return context().getAttributeManager().getAttributeModule(GlobalNames.key, task());
    }

    @Binding(LAZY)
    default UniqueNumbers uniqueNumbers() {
        return context().getUniqueNumbers();
    }

    @Binding(LAZY)
    default TreeShadow tree() {
        return context().getAttributeManager().getAttributeModule(TreeShadow.key, task());
    }

    @Binding(LAZY)
    default ActorMachineScopes scopes() {
        return context().getAttributeManager().getAttributeModule(ActorMachineScopes.key, task());
    }

    // -- Code Genearation
    @Binding(MODULE)
    Preprocessor preprocessor();

    @Binding(MODULE)
    FileNotice fileNotice();

    @Binding(MODULE)
    Actor actor();

    @Binding(MODULE)
    Controllers controllers();

    @Binding(MODULE)
    DefaultValues defaultValues();

    @Binding(MODULE)
    Structure structure();

    @Binding(MODULE)
    Code code();

    @Binding(MODULE)
    Variables variables();

    @Binding(MODULE)
    Callables callables();



}
