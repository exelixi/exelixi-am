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
import se.lth.cs.tycho.ir.Generator;
import se.lth.cs.tycho.ir.Port;
import se.lth.cs.tycho.ir.decl.GeneratorVarDecl;
import se.lth.cs.tycho.ir.decl.VarDecl;
import se.lth.cs.tycho.ir.entity.PortDecl;
import se.lth.cs.tycho.ir.expr.*;
import se.lth.cs.tycho.ir.network.Connection;
import se.lth.cs.tycho.ir.stmt.*;
import se.lth.cs.tycho.ir.stmt.lvalue.LValue;
import se.lth.cs.tycho.ir.stmt.lvalue.LValueDeref;
import se.lth.cs.tycho.ir.stmt.lvalue.LValueIndexer;
import se.lth.cs.tycho.ir.stmt.lvalue.LValueVariable;
import se.lth.cs.tycho.ir.util.ImmutableList;
import se.lth.cs.tycho.phases.attributes.Names;
import se.lth.cs.tycho.phases.attributes.Types;
import se.lth.cs.tycho.phases.cbackend.Emitter;
import se.lth.cs.tycho.settings.Configuration;
import se.lth.cs.tycho.types.*;
import xyz.exelixi.backend.opencl.aocl.AoclBackendCore;
import xyz.exelixi.utils.Resolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.multij.BindingKind.MODULE;
import static xyz.exelixi.backend.opencl.aocl.phases.AoclBackendPhase.usePipes;

/**
 * @author Simone Casale-Brunet
 */
@Module
public interface Code {
    @Binding(MODULE)
    AoclBackendCore backend();

    default Resolver resolver() {
        return backend().resolver().get();
    }

    default Emitter emitter() {
        return backend().emitter();
    }

    default Types types() {
        return backend().types();
    }

    default Variables variables() {
        return backend().variables();
    }

    default Names names() {
        return backend().names();
    }

    default Configuration configuration() {
        return backend().context().getConfiguration();
    }

    default String outputPortTypeSize(Port port) {
        Connection.End source = new Connection.End(Optional.of(backend().instance().get().getInstanceName()), port.getName());
        return "";//backend().channels().sourceEndTypeSize(source);
    }

    default String inputPortTypeSize(Port port) {
        return "";//backend().channels().targetEndTypeSize(new Connection.End(Optional.of(backend().instance().get().getInstanceName()), portDeclaration.getName()));
    }

    void assign(Type type, String lvalue, Expression expr);

    default void assign(RefType type, String lvalue, Expression expr) {
        assignScalar(type, lvalue, expr);
    }

    default void assign(LambdaType type, String lvalue, Expression expr) {
        assignScalar(type, lvalue, expr);
    }

    default void assign(ProcType type, String lvalue, Expression expr) {
        assignScalar(type, lvalue, expr);
    }

    default void assign(IntType type, String lvalue, Expression expr) {
        assignScalar(type, lvalue, expr);
    }

    default void assign(RealType type, String lvalue, Expression expr) {
        assignScalar(type, lvalue, expr);
    }

    default void assign(BoolType type, String lvalue, Expression expr) {
        assignScalar(type, lvalue, expr);
    }

    default void assign(UnitType type, String lvalue, Expression expr) {
        emitter().emit("%s = 0;", lvalue);
    }

    default void assign(ListType type, String lvalue, Expression expr) {
        assignList(type, lvalue, expr);
    }

    default void assignScalar(Type type, String lvalue, Expression expr) {
        emitter().emit("%s = %s;", lvalue, evaluate(expr));
    }

    default void assignScalar(Type type, String lvalue, ExprInput input) {
        throw new UnsupportedOperationException("channel peeks are not supported");
    }

    default void assignList(ListType type, String lvalue, Expression expr) {
        emitter().emit("%s = %s", lvalue, evaluate(expr));
    }

    default void assignList(ListType type, String lvalue, ExprInput input) {
        throw new UnsupportedOperationException("channel peeks are not supported");
    }

    default void assignList(ListType type, String lvalue, ExprVariable var) {
        assert type.getSize().isPresent();
        String tmp = variables().generateTemp();
        String rvalue = evaluate(var);
        emitter().emit("for (size_t %s=0; %1$s < %d; %1$s++) {", tmp, type.getSize().getAsInt());
        emitter().increaseIndentation();
        emitter().emit("%s[%s] = %s[%2$s];", lvalue, tmp, rvalue);
        emitter().decreaseIndentation();
        emitter().emit("}");
    }

    default void assignList(ListType type, String lvalue, ExprList list) {
        if (type.getSize().isPresent()) {
            int i = 0;
            for (Expression element : list.getElements()) {
                assign(type.getElementType(), String.format("%s[%d]", lvalue, i), element);
                i = i + 1;
            }
        } else {
            throw new UnsupportedOperationException("dynamic list assignment is not supported");
        }
    }

    default void assignList(ListType type, String lvalue, ExprComprehension list) {
        emitter().emit("{");
        emitter().increaseIndentation();
        String index = variables().generateTemp();
        emitter().emit("int %s = 0;", index);
        assignListComprehension(type, lvalue, index, list);
        emitter().decreaseIndentation();
        emitter().emit("}");
    }

    void assignListComprehension(ListType type, String lvalue, String index, Expression list);

    default void assignListComprehension(ListType type, String lvalue, String index, ExprComprehension list) {
        if (!list.getFilters().isEmpty()) {
            throw new UnsupportedOperationException();
        }
        Generator generator = list.getGenerator();
        withGenerator(generator.getCollection(), generator.getVarDecls(), () -> {
            assignListComprehension(type, lvalue, index, list.getCollection());
        });
    }

    default void assignListComprehension(ListType type, String lvalue, String index, ExprList list) {
        for (Expression element : list.getElements()) {
            assign(type.getElementType(), String.format("%s[%s]", lvalue, index), element);
            emitter().emit("%s++;", index);
        }
    }

    String declaration(Type type, String name);


    default String declaration(IntType type, String name) {
        return type(type) + " " + name;
    }

    default String declaration(UnitType type, String name) {
        return "char " + name;
    }

    default String declaration(RefType type, String name) {
        return declaration(type.getType(), String.format("(*%s)", name));
    }

    default String declaration(LambdaType type, String name) {
        String t = backend().callables().mangle(type).encode();
        return t + " " + name;
    }

    default String declaration(ProcType type, String name) {
        String t = backend().callables().mangle(type).encode();
        return t + " " + name;
    }

    default String declaration(ListType type, String name) {
        if (type.getSize().isPresent()) {
            return declaration(type.getElementType(), String.format("%s[%d]", name, type.getSize().getAsInt()));
        } else {
            //TODO to be implemented
            return String.format("%s %s[] /* TODO IMPLEMENT */ ", type(type.getElementType()), name);
        }
    }

    default String declaration(RealType type, String name) {
        return String.format("%s %s", type(type), name);
    }

    default String declaration(QueueType type, String name) {
        return String.format("/* delcaration of internal queue */");
    }

    default String declaration(BoolType type, String name) {
        return "bool " + name;
    }

    default String declaration(StringType type, String name) {
        return "char *" + name;
    }

    String type(Type type);

    default String inputPipeDeclaration(PortDecl portDecl, Connection connection) {
        int id = resolver().getConnectionId(connection);
        String type = type(types().declaredPortType(portDecl));
        String attributes = "__attribute__((blocking)) __attribute__((depth(FIFO_DEPTH)))";
        return "read_only" + " pipe " + type + " " + attributes + " " + " FIFO_" + id;
    }

    default String outputPipeDeclaration(PortDecl portDecl, Connection connection) {
        int id = resolver().getConnectionId(connection);
        String type = type(types().declaredPortType(portDecl));
        String attributes = "__attribute__((blocking)) __attribute__((depth(FIFO_DEPTH)))";
        return "write_only" + " pipe " + type + " " + attributes + " " + "FIFO_" + id;
    }

    default String alteraChannelDefinition(Connection connection) {
        int id = resolver().getConnectionId(connection);

        PortDecl portDecl = resolver().getTargetPortDecl(connection);
        if (portDecl == null) {
            portDecl = resolver().getSourcePortDecl(connection);
        }
        if (portDecl == null) {
            throw new Error("Connection type can not be resolved");
        }

        String type = type(types().declaredPortType(portDecl));
        return "channel " + type + " FIFO_" + id;
    }


    default String type(IntType type) {
        //FIXME support type size
        // if (type.getSize().isPresent())
        return type.isSigned() ? "int" : "uint";

    }

    default String type(RealType type) {
        switch (type.getSize()) {
            case 32:
                return "float";
            case 64:
                return "double";
            default:
                throw new UnsupportedOperationException("Unknown real type.");
        }
    }

    default String type(UnitType type) {
        return "void";
    }

    default String type(ListType type) {
        if (type.getSize().isPresent()) {
            return String.format("%s[%s]", type(type.getElementType()), type.getSize().getAsInt());
        } else {
            return "void*";
        }
    }

    default String type(StringType type) {
        return "char*";
    }

    default String type(BoolType type) {
        return "bool";
    }

    String evaluate(Expression expr);

    default String evaluate(ExprVariable variable) {
        return variables().name(variable.getVariable());
    }

    default String evaluate(ExprRef ref) {
        return "(&" + variables().name(ref.getVariable()) + ")";
    }

    default String evaluate(ExprDeref deref) {
        return "(*" + evaluate(deref.getReference()) + ")";
    }

    default String evaluate(ExprGlobalVariable variable) {
        return variables().globalName(variable);
    }

    default String evaluate(ExprLiteral literal) {
        switch (literal.getKind()) {
            case Integer:
                return literal.getText();
            case True:
                return "true";
            case False:
                return "false";
            case Real:
                return literal.getText();
            case String:
                return literal.getText();
            default:
                throw new UnsupportedOperationException(literal.getText());
        }
    }

    default String evaluate(ExprBinaryOp binaryOp) {
        assert binaryOp.getOperations().size() == 1 && binaryOp.getOperands().size() == 2;
        String operation = binaryOp.getOperations().get(0);
        Expression left = binaryOp.getOperands().get(0);
        Expression right = binaryOp.getOperands().get(1);
        switch (operation) {
            case "+":
            case "-":
            case "*":
            case "/":
            case "<":
            case "<=":
            case ">":
            case ">=":
            case "==":
            case "!=":
            case "<<":
            case ">>":
            case "&":
            case "|":
            case "^":
                return String.format("(%s %s %s)", evaluate(left), operation, evaluate(right));
            case "=":
                return String.format("(%s == %s)", evaluate(left), evaluate(right));
            case "mod":
                return String.format("(%s %% %s)", evaluate(left), evaluate(right));
            case "and":
            case "&&":
                String andResult = variables().generateTemp();
                emitter().emit("bool %s;", andResult);
                emitter().emit("if (%s) {", evaluate(left));
                emitter().increaseIndentation();
                emitter().emit("%s = %s;", andResult, evaluate(right));
                emitter().decreaseIndentation();
                emitter().emit("} else {");
                emitter().increaseIndentation();
                emitter().emit("%s = false;", andResult);
                emitter().decreaseIndentation();
                emitter().emit("}");
                return andResult;
            case "||":
            case "or":
                String orResult = variables().generateTemp();
                emitter().emit("bool %s;", orResult);
                emitter().emit("if (%s) {", evaluate(left));
                emitter().increaseIndentation();
                emitter().emit("%s = true;", orResult);
                emitter().decreaseIndentation();
                emitter().emit("} else {");
                emitter().increaseIndentation();
                emitter().emit("%s = %s;", orResult, evaluate(right));
                emitter().decreaseIndentation();
                emitter().emit("}");
                return orResult;
            default:
                throw new UnsupportedOperationException(operation);
        }
    }

    default String evaluate(ExprUnaryOp unaryOp) {
        switch (unaryOp.getOperation()) {
            case "-":
            case "~":
                return String.format("%s(%s)", unaryOp.getOperation(), evaluate(unaryOp.getOperand()));
            case "not":
                return String.format("!%s", evaluate(unaryOp.getOperand()));
            default:
                throw new UnsupportedOperationException(unaryOp.getOperation());
        }
    }

    default String evaluate(ExprComprehension comprehension) {
        return evaluateComprehension(comprehension, types().type(comprehension));
    }

    String evaluateComprehension(ExprComprehension comprehension, Type t);

    default String evaluateComprehension(ExprComprehension comprehension, ListType t) {
        String name = variables().generateTemp();
        String decl = declaration(t, name);
        emitter().emit("%s;", decl);
        String index = variables().generateTemp();
        emitter().emit("size_t %s = 0;", index);
        evaluateListComprehension(comprehension, name, index);
        return name;
    }

    void evaluateListComprehension(Expression comprehension, String result, String index);

    default void evaluateListComprehension(ExprComprehension comprehension, String result, String index) {
        if (!comprehension.getFilters().isEmpty()) {
            throw new UnsupportedOperationException("Filters in comprehensions not supported.");
        }
        withGenerator(comprehension.getGenerator().getCollection(), comprehension.getGenerator().getVarDecls(), () -> {
            evaluateListComprehension(comprehension.getCollection(), result, index);
        });
    }

    default void evaluateListComprehension(ExprList list, String result, String index) {
        list.getElements().forEach(element ->
                emitter().emit("%s[%s++] = %s;", result, index, evaluate(element))
        );
    }

    void withGenerator(Expression collection, ImmutableList<GeneratorVarDecl> varDecls, Runnable body);

    default void withGenerator(ExprBinaryOp binOp, ImmutableList<GeneratorVarDecl> varDecls, Runnable action) {
        if (binOp.getOperations().equals(Collections.singletonList(".."))) {
            String from = evaluate(binOp.getOperands().get(0));
            String to = evaluate(binOp.getOperands().get(1));
            for (VarDecl d : varDecls) {
                Type type = types().declaredType(d);
                String name = variables().declarationName(d);
                emitter().emit("%s = %s;", declaration(type, name), from);
                emitter().emit("while (%s <= %s) {", name, to);
                emitter().increaseIndentation();
            }
            action.run();
            List<VarDecl> reversed = new ArrayList<>(varDecls);
            Collections.reverse(reversed);
            for (VarDecl d : reversed) {
                emitter().emit("%s++;", variables().declarationName(d));
                emitter().decreaseIndentation();
                emitter().emit("}");
            }
        } else {
            throw new UnsupportedOperationException(binOp.getOperations().get(0));
        }
    }


    default String evaluate(ExprList list) {
        ListType t = (ListType) types().type(list);
        if (t.getSize().isPresent()) {
            String name = variables().generateTemp();
            String decl = declaration(t, name);
            String value = list.getElements().stream().sequential()
                    .map(this::evaluate)
                    .collect(Collectors.joining(", ", "{", "}"));
            emitter().emit("%s = %s;", decl, value);
            return name;
        } else {
            throw new Error("dynamically sized lists are not supported");
        }
    }

    void forEach(Expression collection, List<GeneratorVarDecl> varDecls, Runnable action);

    default void forEach(ExprBinaryOp binOp, List<GeneratorVarDecl> varDecls, Runnable action) {
        emitter().emit("{");
        emitter().increaseIndentation();
        if (binOp.getOperations().equals(Collections.singletonList(".."))) {
            Type type = types().declaredType(varDecls.get(0));
            for (VarDecl d : varDecls) {
                emitter().emit("%s;", declaration(type, variables().declarationName(d)));
            }
            String temp = variables().generateTemp();
            emitter().emit("%s = %s;", declaration(type, temp), evaluate(binOp.getOperands().get(0)));
            emitter().emit("while (%s <= %s) {", temp, evaluate(binOp.getOperands().get(1)));
            emitter().increaseIndentation();
            for (VarDecl d : varDecls) {
                emitter().emit("%s = %s++;", variables().declarationName(d), temp);
            }
            action.run();
            emitter().decreaseIndentation();
            emitter().emit("}");
        } else {
            throw new UnsupportedOperationException(binOp.getOperations().get(0));
        }
        emitter().decreaseIndentation();
        emitter().emit("}");
    }

    default String evaluate(ExprIndexer indexer) {
        return String.format("%s[%s]", evaluate(indexer.getStructure()), evaluate(indexer.getIndex()));
    }

    default String evaluate(ExprIf expr) {
        Type type = types().type(expr);
        String temp = variables().generateTemp();
        String decl = declaration(type, temp);
        emitter().emit("%s;", decl);
        emitter().emit("if (%s) {", evaluate(expr.getCondition()));
        emitter().increaseIndentation();
        assign(type, temp, expr.getThenExpr()); // this kind of assignment?
        emitter().decreaseIndentation();
        emitter().emit("} else {");
        emitter().increaseIndentation();
        assign(type, temp, expr.getElseExpr()); // this kind of assignment?
        emitter().decreaseIndentation();
        emitter().emit("}");
        return temp;
    }

    default String evaluate(ExprApplication apply) {
        String fn = evaluate(apply.getFunction());
        List<String> parameters = new ArrayList<>();
        for (Expression parameter : apply.getArgs()) {
            parameters.add(evaluate(parameter));
        }
        String result = variables().generateTemp();
        String decl = declaration(types().type(apply), result);
        emitter().emit("%s = %s(%s);", decl, fn, String.join(", ", parameters));
        return result;
    }

    default String evaluate(ExprLambda lambda) {
        String functionName = backend().callables().functionName(lambda);
        return functionName;
    }

    default String evaluate(ExprProc proc) {
        String functionName = backend().callables().functionName(proc);
        return functionName;
    }

    default String evaluate(ExprLet let) {
        for (VarDecl decl : let.getVarDecls()) {
            Type type = types().declaredType(decl);
            String name = variables().declarationName(decl);
            emitter().emit("%s;", declaration(type, name));
            assign(type, name, decl.getValue());
        }
        return evaluate(let.getBody());
    }

    void execute(Statement stmt);

    default void execute(StmtConsume consume) {
        if (consume.getNumberOfTokens() == 1) {
            emitter().emit("%s.read();", consume.getPort().getName());
        } else {
            emitter().emit("for(int i = 0; i <= %d; i++){", consume.getNumberOfTokens());
            emitter().increaseIndentation();
            emitter().emit("%s.read();", consume.getPort().getName());
            emitter().decreaseIndentation();
            emitter().emit("}");
        }
    }

    default void execute(StmtRead read) {
        String portName = read.getPort().getName();
        Connection connection = resolver().getIncoming(backend().instance().get().getInstanceName(), portName);
        int id = resolver().getConnectionId(connection);
        String fifoName = "FIFO_" + id;

        if (read.getRepeatExpression() == null) {
            String portType = type(types().portType(read.getPort()));
            for (LValue lvalue : read.getLValues()) {
                String value = lvalue(lvalue);
                if (configuration().get(usePipes).booleanValue()) {
                    emitter().emit("read_pipe(%s, &%s);", fifoName, value);
                } else {
                    emitter().emit("%s = read_channel_altera(%s);", value, fifoName);
                }
            }
        } else if (read.getLValues().size() == 1) {
            String portType = type(types().portType(read.getPort()));
            String value = lvalue(read.getLValues().get(0));
            String repeat = evaluate(read.getRepeatExpression());
            String temp = variables().generateTemp();
            emitter().emit("for (int %1$s = 0; %1$s < %2$s; %1$s++) {", temp, repeat);
            emitter().increaseIndentation();
            if (configuration().get(usePipes).booleanValue()) {
                emitter().emit("read_pipe(%s, &%s[%s]);", fifoName, value, temp);
            } else {
                emitter().emit("%s[%s] = read_channel_altera(%s);", value, temp, fifoName);
            }
            emitter().decreaseIndentation();
            emitter().emit("}");
        } else {
            throw new Error("not implemented");
        }
    }

    default void execute(StmtWrite write) {
        String portName = write.getPort().getName();
        // of list of fifos to be written
        List<String> fifoNames = new ArrayList<>();
        List<Connection> connections = resolver().getOutgoings(backend().instance().get().getInstanceName(), portName);
        connections.forEach(connection -> {
            int id = resolver().getConnectionId(connection);
            fifoNames.add("FIFO_" + id);
        });

        if (write.getRepeatExpression() == null) {
            String portType = type(types().portType(write.getPort()));
            for (Expression expr : write.getValues()) {
                fifoNames.forEach(fifo -> {
                    if (configuration().get(usePipes).booleanValue()) {
                        emitter().emit("write_pipe(%s, &%s);", fifo, evaluate(expr));
                    } else {
                        emitter().emit("write_channel_altera(%s, %s);", fifo, evaluate(expr));
                    }
                });

            }
        } else if (write.getValues().size() == 1) {
            String portType = type(types().portType(write.getPort()));
            String value = evaluate(write.getValues().get(0));
            String repeat = evaluate(write.getRepeatExpression());
            String temp = variables().generateTemp();

            emitter().emit("for (int %1$s = 0; %1$s < %2$s; %1$s++) {", temp, repeat);
            emitter().increaseIndentation();
            for (String fifo : fifoNames) {
                if (configuration().get(usePipes).booleanValue()) {
                    emitter().emit("write_pipe(%s, &%s[%s]);", fifo, value, temp);
                } else {
                    emitter().emit("write_channel_altera(%s, %s[%s]);", fifo, value, temp);
                }
            }
            emitter().decreaseIndentation();
            emitter().emit("}");


        } else {
            throw new Error("not implemented");
        }
    }

    default void execute(StmtAssignment assign) {
        Type type = types().lvalueType(assign.getLValue());
        String lvalue = lvalue(assign.getLValue());
        assign(type, lvalue, assign.getExpression());
    }

    default void execute(StmtBlock block) {
        emitter().emit("{");
        emitter().increaseIndentation();
        for (VarDecl decl : block.getVarDecls()) {
            Type t = types().declaredType(decl);
            String declarationName = variables().declarationName(decl);
            String d = declaration(t, declarationName);
            emitter().emit("%s;", d);
            if (decl.getValue() != null) {
                assign(t, declarationName, decl.getValue());
            }
        }
        block.getStatements().forEach(this::execute);
        emitter().decreaseIndentation();
        emitter().emit("}");
    }

    default void execute(StmtIf stmt) {
        emitter().emit("if (%s) {", evaluate(stmt.getCondition()));
        emitter().increaseIndentation();
        stmt.getThenBranch().forEach(this::execute);
        emitter().decreaseIndentation();
        if (stmt.getElseBranch() != null) {
            emitter().emit("} else {");
            emitter().increaseIndentation();
            stmt.getElseBranch().forEach(this::execute);
            emitter().decreaseIndentation();
        }
        emitter().emit("}");
    }

    default void execute(StmtForeach foreach) {
        forEach(foreach.getGenerator().getCollection(), foreach.getGenerator().getVarDecls(), () -> {
            for (Expression filter : foreach.getFilters()) {
                emitter().emit("if (%s) {", evaluate(filter));
                emitter().increaseIndentation();
            }
            foreach.getBody().forEach(this::execute);
            for (Expression filter : foreach.getFilters()) {
                emitter().decreaseIndentation();
                emitter().emit("}");
            }
        });
    }

    default void execute(StmtCall call) {
        String proc = evaluate(call.getProcedure());
        List<String> parameters = new ArrayList<>();
        parameters.add(proc + ".env");
        for (Expression parameter : call.getArgs()) {
            parameters.add(evaluate(parameter));
        }
        emitter().emit("%s.f(%s);", proc, String.join(", ", parameters));
    }

    default void execute(StmtWhile stmt) {
        emitter().emit("while (%s) {", evaluate(stmt.getCondition()));
        emitter().increaseIndentation();
        stmt.getBody().forEach(this::execute);
        emitter().decreaseIndentation();
        emitter().emit("}");
    }

    String lvalue(LValue lvalue);

    default String lvalue(LValueVariable var) {
        return variables().name(var.getVariable());
    }

    default String lvalue(LValueDeref deref) {
        return "*" + lvalue(deref.getVariable());
    }

    default String lvalue(LValueIndexer indexer) {
        return String.format("%s[%s]", lvalue(indexer.getStructure()), evaluate(indexer.getIndex()));
    }
}
