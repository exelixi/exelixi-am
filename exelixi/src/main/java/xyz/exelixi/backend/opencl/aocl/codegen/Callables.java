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
import org.multij.BindingKind;
import org.multij.Module;
import se.lth.cs.tycho.ir.IRNode;
import se.lth.cs.tycho.ir.decl.ClosureVarDecl;
import se.lth.cs.tycho.ir.decl.ParameterVarDecl;
import se.lth.cs.tycho.ir.decl.VarDecl;
import se.lth.cs.tycho.ir.entity.Entity;
import se.lth.cs.tycho.ir.entity.am.Scope;
import se.lth.cs.tycho.ir.expr.ExprLambda;
import se.lth.cs.tycho.ir.expr.ExprLet;
import se.lth.cs.tycho.ir.expr.ExprProc;
import se.lth.cs.tycho.ir.expr.Expression;
import se.lth.cs.tycho.ir.stmt.StmtBlock;
import se.lth.cs.tycho.ir.util.ImmutableList;
import se.lth.cs.tycho.phases.cbackend.util.NameExpression;
import se.lth.cs.tycho.phases.cbackend.util.NameExpression.Seq;
import se.lth.cs.tycho.types.*;
import xyz.exelixi.backend.opencl.aocl.AoclBackendCore;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static se.lth.cs.tycho.phases.cbackend.util.NameExpression.name;
import static se.lth.cs.tycho.phases.cbackend.util.NameExpression.seq;

/**
 * @author Simone Casale-Brunet
 */
@Module
public interface Callables {
    @Binding
    AoclBackendCore backend();
    /*
    Global scope:
	- typedef fat pointers for all different ExprLambda and ExprProc types
	- declare prototype for all ExprLambda and ExprProc
	- define function for all external VarDecls of callable type containing the extern function declaration.
	- typedef environment for all ExprLambda and ExprProc
	- define function for all ExprLambda and ExprProc

	Where declared:
	- create fat function pointer with null environment for all external declaration.
	- create fat function pointer with envorinment for all ExprLambda and ExprProc.
	 */

    default void declareCallables() {
        backend().emitter().emit("");
        backend().task().walk().forEach(this::callablePrototype);
        backend().emitter().emit("");

    }

    default void defineCallables() {
        backend().task().walk().forEach(this::callableDefinition);
        backend().emitter().emit("");
    }


    default void collectCallableTypes(IRNode node, Consumer<CallableType> collector) {
    }

    default void collectCallableTypes(ExprLambda lambda, Consumer<CallableType> collector) {
        collector.accept((LambdaType) backend().types().type(lambda));
    }

    default void collectCallableTypes(ExprProc proc, Consumer<CallableType> collector) {
        collector.accept((ProcType) backend().types().type(proc));
    }

    default void collectCallableTypes(VarDecl varDecl, Consumer<CallableType> collector) {
        if (varDecl.isExternal()) {
            Type type = backend().types().declaredType(varDecl);
            if (type instanceof CallableType) {
                collector.accept((CallableType) type);
            }
        }
    }

    default Set<Expression> callablesInScope(IRNode scope) {
        Set<Expression> result = new LinkedHashSet<>();
        collectCallablesInScope(scope, result::add);
        return result;
    }

    default void collectCallablesInScope(IRNode node, Consumer<Expression> collector) {
        node.forEachChild(child -> collectCallablesInScope(child, collector));
    }

    default void collectCallablesInScope(ExprLambda lambda, Consumer<Expression> collector) {
        collector.accept(lambda);
    }

    default void collectCallablesInScope(ExprProc proc, Consumer<Expression> collector) {
        collector.accept(proc);
    }

    default void collectCallablesInScope(ExprLet let, Consumer<Expression> collector) {
    }

    default void collectCallablesInScope(StmtBlock block, Consumer<Expression> collector) {
    }

    default void collectCallablesInScope(Entity entity, Consumer<Expression> collector) {
    }

    NameExpression mangle(Type t);

    default NameExpression mangle(CallableType type) {
        NameExpression kind = name("fn");
        NameExpression returnType = mangle(type.getReturnType());
        List<NameExpression> parameterTypes = type.getParameterTypes().stream().map(this::mangle).collect(Collectors.toList());
        return new Seq(ImmutableList.<NameExpression>builder().add(kind).add(returnType).addAll(parameterTypes).build());
    }

    default NameExpression mangle(UnitType type) {
        return name("void");
    }

    default NameExpression mangle(StringType type) {
        return name("str");
    }

    default NameExpression mangle(BoolType type) {
        return name("bool");
    }

    default NameExpression mangle(ListType type) {
        String size = type.getSize().isPresent() ? Integer.toString(type.getSize().getAsInt()) : "X";
        return seq(name("list"), mangle(type.getElementType()), name(size));
    }

    default NameExpression mangle(IntType type) {
        String kind;
        if (type.isSigned()) {
            kind = "i";
        } else {
            kind = "u";
        }
        String size;
        if (type.getSize().isPresent()) {
            size = Integer.toString(type.getSize().getAsInt());
        } else {
            size = "X";
        }
        return name(kind + size);
    }

    // function prototype
    default void callablePrototype(IRNode callable) {
    }

    default void callablePrototype(ExprLambda lambda) {
        String name = functionName(lambda);
        // closureTypedef(lambda.getClosure(), name);
        backend().emitter().emit("%s;", lambdaHeader(lambda));
    }

    default void callablePrototype(ExprProc lambda) {
        String name = functionName(lambda);
        //closureTypedef(lambda.getClosure(), name);
        backend().emitter().emit("%s;", procHeader(lambda));
    }

    default void closureTypedef(ImmutableList<ClosureVarDecl> closure, String name) {
        backend().emitter().emit("typedef struct {");
        backend().emitter().increaseIndentation();
        for (ClosureVarDecl var : closure) {
            Type type = backend().types().declaredType(var);
            String varName = backend().variables().declarationName(var);
            backend().emitter().emit("%s;", backend().code().declaration(type, varName));
        }
        backend().emitter().decreaseIndentation();
        backend().emitter().emit("} envt_%s;", name);
    }


    // function definition (matching function prototype)
    // with environment typedef
    default void callableDefinition(IRNode callable) {
    }

    default void callableDefinition(ExprLambda lambda) {
        String name = functionName(lambda);
        backend().emitter().emit("%s {", lambdaHeader(lambda));
        backend().emitter().increaseIndentation();
        backend().emitter().emit("return %s;", backend().code().evaluate(lambda.getBody()));
        backend().emitter().decreaseIndentation();
        backend().emitter().emit("}");
        backend().emitter().emit("");
    }

    default void callableDefinition(ExprProc proc) {
        String name = functionName(proc);
        backend().emitter().emit("%s {", procHeader(proc));
        backend().emitter().increaseIndentation();
        proc.getBody().forEach(backend().code()::execute);
        backend().emitter().decreaseIndentation();
        backend().emitter().emit("}");
        backend().emitter().emit("");
    }

    default String envField(VarDecl decl) {
        String name = backend().variables().declarationName(decl);
        Type originalType = new RefType(backend().types().declaredType(decl));
        return backend().code().declaration(originalType, name) + ";";
    }

    // closure creation
    String evaluate(Expression callable);

    @Binding(BindingKind.LAZY)
    default Map<Expression, String> callablesNames() {
        return new HashMap<>();
    }

    @Binding(BindingKind.LAZY)
    default Map<VarDecl, String> externalNames() {
        return new HashMap<>();
    } // TODO: persist between backends?

    @Binding(BindingKind.LAZY)
    default Set<String> usedNames() {
        return new HashSet<>();
    }

    /**
     * The name of the C-function
     */
    default String functionName(Expression callable) {
        assert callable instanceof ExprLambda || callable instanceof ExprProc;
        if (!callablesNames().containsKey(callable)) {
            IRNode parent = backend().tree().parent(callable);
            String candidate;
            if (parent instanceof VarDecl) {
                VarDecl decl = (VarDecl) parent;
                candidate = "f_" + decl.getName();
            } else {
                candidate = "f_anon";
            }
            int i = 0;
            while (usedNames().contains(candidate + "_" + i)) {
                i++;
            }

            candidate = candidate + (i > 0 ? "_" + i : "");
            usedNames().add(candidate);
            callablesNames().put(callable, candidate);
        }

        return callablesNames().get(callable);
    }

    default IRNode environmentScope(IRNode callable) {
        return backend().tree().parent(callable);
    }

    default IRNode environmentScope(ExprLambda lambda) {
        return lambda;
    }

    default IRNode environmentScope(ExprProc proc) {
        return proc;
    }

    default IRNode environmentScope(Scope scope) {
        return scope;
    }

    default IRNode environmentScope(ExprLet let) {
        return let;
    }

    default IRNode environmentScope(StmtBlock block) {
        return block;
    }

    default String environmentName(Expression callable) {
        IRNode scope = environmentScope(backend().tree().parent(callable));
        String functionName = functionName(callable);
        if (scope instanceof Scope) {
            return "self->env_" + functionName;
        } else {
            return "env_" + functionName;
        }
    }

    default String externalWrapperFunctionName(VarDecl external) {
        assert external.isExternal();
        if (!externalNames().containsKey(external)) {
            int i = 0;
            String name;
            do {
                name = "external_" + i;
                i++;
            } while (usedNames().contains(name));
            externalNames().put(external, name);
            usedNames().add(name);
        }
        return externalNames().get(external);
    }

    default void externalCallableDeclaration(IRNode varDecl) {
    }

    default void externalCallableDeclaration(VarDecl varDecl) {
        if (varDecl.isExternal()) {
            Type type = backend().types().declaredType(varDecl);
            assert type instanceof CallableType : "External declaration must be function or procedure";
            CallableType callable = (CallableType) type;
            List<String> parameterNames = new ArrayList<>();
            for (int i = 0; i < callable.getParameterTypes().size(); i++) {
                parameterNames.add("p_" + i);
            }
            backend().emitter().emit("%s;", callableHeader(varDecl.getOriginalName(), callable, parameterNames, false));
            String name = externalWrapperFunctionName(varDecl);
            backend().emitter().emit("%s;", callableHeader(name, callable, parameterNames, false));
        }
    }

    default void externalCallableDefinition(IRNode node) {
    }

    default void externalCallableDefinition(VarDecl varDecl) {
        if (varDecl.isExternal()) {
            Type type = backend().types().declaredType(varDecl);
            assert type instanceof CallableType : "External declaration must be function or procedure";
            CallableType callable = (CallableType) type;
            List<String> parameterNames = new ArrayList<>();
            for (int i = 0; i < callable.getParameterTypes().size(); i++) {
                parameterNames.add("p_" + i);
            }
            String name = externalWrapperFunctionName(varDecl);
            backend().emitter().emit("%s {", callableHeader(name, callable, parameterNames, false));
            backend().emitter().increaseIndentation();
            String call = varDecl.getOriginalName() + "(" + String.join(", ", parameterNames) + ")";
            if (callable.getReturnType().equals(UnitType.INSTANCE)) {
                backend().emitter().emit("%s;", call);
            } else {
                backend().emitter().emit("return %s;", call);
            }
            backend().emitter().decreaseIndentation();
            backend().emitter().emit("}");
        }
    }

    default String lambdaHeader(ExprLambda lambda) {
        String name = functionName(lambda);
        LambdaType type = (LambdaType) backend().types().type(lambda);
        ImmutableList<String> parameterNames = lambda.getValueParameters().map(backend().variables()::declarationName);
        return callableHeader(name, type, parameterNames, false);
    }

    default String procHeader(ExprProc proc) {
        String name = functionName(proc);
        ProcType type = (ProcType) backend().types().type(proc);
        ImmutableList<String> parameterNames = proc.getValueParameters().map(backend().variables()::declarationName);
        return callableHeader(name, type, parameterNames, false);
    }

    default String callableHeader(String name, CallableType type, List<String> parameterNames, boolean withEnv) {
        List<String> parameters = new ArrayList<>();
        if (withEnv) {
            parameters.add("void *e");
        }
        assert parameterNames.size() == type.getParameterTypes().size();
        for (int i = 0; i < parameterNames.size(); i++) {
            parameters.add(backend().code().declaration(type.getParameterTypes().get(i), parameterNames.get(i)));
        }
        String result = backend().code().type(type.getReturnType());
        result += " ";
        result += name;
        result += "(";
        result += String.join(", ", parameters);
        result += ")";
        return result;
    }

    String resultFromType(Type type);

    default String resultFromType(CallableType type) {
        return backend().code().type(type.getReturnType());
    }

    Stream<String> parametersFromType(Type type);

    default Stream<String> parametersFromType(CallableType type) {
        return type.getParameterTypes().stream().map(backend().code()::type);
    }

    Stream<String> parametersFromValue(Expression expr);

    default Stream<String> parametersFromValue(ExprProc proc) {
        return proc.getValueParameters().stream()
                .map(this::parameterDeclaration);
    }

    default Stream<String> parametersFromValue(ExprLambda lambda) {
        return lambda.getValueParameters().stream()
                .map(this::parameterDeclaration);
    }

    default String parameterDeclaration(ParameterVarDecl decl) {
        Type type = backend().types().declaredType(decl);
        String name = backend().variables().declarationName(decl);
        return backend().code().declaration(type, name);
    }
}
