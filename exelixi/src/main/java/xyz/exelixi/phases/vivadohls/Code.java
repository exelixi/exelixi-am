package xyz.exelixi.phases.vivadohls;

import org.multij.Binding;
import org.multij.Module;
import se.lth.cs.tycho.ir.Generator;
import se.lth.cs.tycho.ir.Port;
import se.lth.cs.tycho.ir.decl.ClosureVarDecl;
import se.lth.cs.tycho.ir.decl.GeneratorVarDecl;
import se.lth.cs.tycho.ir.decl.VarDecl;
import se.lth.cs.tycho.ir.entity.PortDecl;
import se.lth.cs.tycho.ir.entity.am.ActorMachine;
import se.lth.cs.tycho.ir.entity.am.Transition;
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
import se.lth.cs.tycho.types.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.multij.BindingKind.MODULE;

@Module
public interface Code {
    @Binding(MODULE)
    VivadoHLSBackend backend();

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

    default String outputPortTypeSize(Port port) {
        Connection.End source = new Connection.End(Optional.of(backend().instance().get().getInstanceName()), port.getName());
        return "";//backend().channels().sourceEndTypeSize(source);
    }

    default String inputPortTypeSize(Port port) {
        return "";//backend().channels().targetEndTypeSize(new Connection.End(Optional.of(backend().instance().get().getInstanceName()), port.getName()));
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
        assert !input.hasRepeat() : "Cannot assign a repeated input to a scalar.";
        Type portType = types().portType(input.getPort());
        String tmp = variables().generateTemp();
        if (input.getOffset() == 0) {
            emitter().emit("%s = channel_peek_first_%s(self->%s_channel);", lvalue, inputPortTypeSize(input.getPort()), input.getPort().getName());
        } else {
            emitter().emit("%s;", declaration(portType, tmp));
            emitter().emit("channel_peek_%s(self->%s_channel, %d, 1, &%s);", inputPortTypeSize(input.getPort()), input.getPort().getName(), input.getOffset(), tmp);
            emitter().emit("%s = %s;", lvalue, tmp); // should handle some discrepancies between port type and variable type.
        }
    }

    default void assignList(ListType type, String lvalue, Expression expr) {
        emitter().emit("%s = %s", lvalue, evaluate(expr));
    }

    default void assignList(ListType type, String lvalue, ExprInput input) {
        assert input.hasRepeat(); // only repeat assignments to lists are supported
        assert input.getPatternLength() == 1; // only with one variable
        assert input.getOffset() == 0; // and that variable is therefore the first
        Type portType = types().portType(input.getPort());
        emitter().emit("channel_peek_%s(self->%s_channel, 0, %d, (%s*) &%s);", inputPortTypeSize(input.getPort()), input.getPort().getName(), input.getRepeat(), type(portType), lvalue);
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
            emitter().emit("// dynamic list assignment is not implemented");
        }
    }

    default void assignList(ListType type, String lvalue, ExprComprehension list) {
        emitter().emit("{");
        emitter().increaseIndentation();
        String index = variables().generateTemp();
        emitter().emit("size_t %s = 0;", index);
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
        return "_Bool " + name;
    }

    default String declaration(StringType type, String name) {
        return "char *" + name;
    }

    String type(Type type);


    default List<String> actorMachineIO(ActorMachine actorMachine) {
        List<String> parameters = new ArrayList<String>();

        // -- IO
        List<PortDecl> inputs = actorMachine.getInputPorts();
        List<PortDecl> outputs = actorMachine.getOutputPorts();

        // -- Treat Inputs
        for (PortDecl portDecl : inputs) {
            parameters.add(backend().code().type(portDecl));
        }

        // -- Treat outputs
        for (PortDecl portDecl : outputs) {
            parameters.add(backend().code().type(portDecl));
        }

        return parameters;
    }

    default List<String> actorMachineIOName(ActorMachine actorMachine) {
        List<String> names = new ArrayList<String>();

        // -- IO
        List<PortDecl> inputs = actorMachine.getInputPorts();
        List<PortDecl> outputs = actorMachine.getOutputPorts();

        // -- Treat Inputs
        for (PortDecl portDecl : inputs) {
            names.add(portDecl.getName());
        }

        // -- Treat outputs
        for (PortDecl portDecl : outputs) {
            names.add(portDecl.getName());
        }

        return names;
    }

    default List<String> transitionIOName(Transition transition) {
        List<String> names = new ArrayList<>();

        for (Port port : transition.getInputRates().keySet()) {
            names.add(port.getName());
        }

        for (Port port : transition.getOutputRates().keySet()) {
            names.add(port.getName());
        }

        return names;
    }

    default String type(PortDecl portDecl) {
        String parentType = "hls::stream";
        String childType = type(types().declaredPortType(portDecl));
        String finalType = parentType + "< " + childType + " >";
        String portName = portDecl.getName();
        return finalType + " &" + portName;
    }

    default String type(Port port) {
        String parentType = "hls::stream";
        String childType = type(types().portType(port));
        String finalType = parentType + "< " + childType + " >";
        String portName = port.getName();
        return finalType + " &" + portName;
    }


    default String type(IntType type) {
        if (type.getSize().isPresent()) {
            int originalSize = type.getSize().getAsInt();
            int targetSize = 8;
            while (originalSize > targetSize) {
                targetSize = targetSize * 2;
            }
            return String.format(type.isSigned() ? "int%d_t" : "uint%d_t", targetSize);
        } else {
            return type.isSigned() ? "int32_t" : "uint32_t";
        }
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
        return "_Bool";
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
                emitter().emit("_Bool %s;", andResult);
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
                emitter().emit("_Bool %s;", orResult);
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
            return "NULL /* TODO: implement dynamically sized lists */";
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
        parameters.add(fn + ".env");
        for (Expression parameter : apply.getArgs()) {
            parameters.add(evaluate(parameter));
        }
        String result = variables().generateTemp();
        String decl = declaration(types().type(apply), result);
        emitter().emit("%s = %s.f(%s);", decl, fn, String.join(", ", parameters));
        return result;
    }

    default String evaluate(ExprLambda lambda) {
        backend().emitter().emit("// begin evaluate(ExprLambda)");
        String functionName = backend().callables().functionName(lambda);
        String env = backend().callables().environmentName(lambda);
        for (ClosureVarDecl var : lambda.getClosure()) {
            assign(types().declaredType(var), env + "." + variables().declarationName(var), var.getValue());
        }

        Type type = backend().types().type(lambda);
        String typeName = backend().callables().mangle(type).encode();
        String funPtr = backend().variables().generateTemp();
        backend().emitter().emit("%s %s = { &%s, &%s };", typeName, funPtr, functionName, env);

        backend().emitter().emit("// end evaluate(ExprLambda)");
        return funPtr;
    }

    default String evaluate(ExprProc proc) {
        backend().emitter().emit("// begin evaluate(ExprProc)");
        String functionName = backend().callables().functionName(proc);
        String env = backend().callables().environmentName(proc);
        for (ClosureVarDecl var : proc.getClosure()) {
            assign(types().declaredType(var), env + "." + variables().declarationName(var), var.getValue());
        }

        Type type = backend().types().type(proc);
        String typeName = backend().callables().mangle(type).encode();
        String funPtr = backend().variables().generateTemp();
        backend().emitter().emit("%s %s = { &%s, &%s };", typeName, funPtr, functionName, env);

        backend().emitter().emit("// end evaluate(ExprProc)");
        return funPtr;
    }

    default String evaluate(ExprLet let) {
        let.forEachChild(backend().callables()::declareEnvironmentForCallablesInScope);
        for (VarDecl decl : let.getVarDecls()) {
            emitter().emit("%s = %s;", declaration(types().declaredType(decl), variables().declarationName(decl)), evaluate(decl.getValue()));
        }
        return evaluate(let.getBody());
    }

    void execute(Statement stmt);

    default void execute(StmtConsume consume) {
        if(consume.getNumberOfTokens() == 1){
            emitter().emit("%s.read();",consume.getPort().getName());
        }else{
            emitter().emit("for(int i = 0; i <= %d; i++){",consume.getNumberOfTokens());
            emitter().increaseIndentation();
            emitter().emit("%s.read();",consume.getPort().getName());
            emitter().decreaseIndentation();
            emitter().emit("}");
        }
    }

    default void execute(StmtWrite write) {
        String portName = write.getPort().getName();
        if (write.getRepeatExpression() == null) {
            String portType = type(types().portType(write.getPort()));
            String tmp = variables().generateTemp();
            emitter().emit("%s;", declaration(types().portType(write.getPort()), tmp));
            for (Expression expr : write.getValues()) {
                emitter().emit("%s = %s;", tmp, evaluate(expr));
                emitter().emit("%s.write(%s);", portName, tmp);
            }
        } else if (write.getValues().size() == 1) {
            String portType = type(types().portType(write.getPort()));
            String value = evaluate(write.getValues().get(0));
            String repeat = evaluate(write.getRepeatExpression());
            String temp = variables().generateTemp();
            emitter().emit("for (size_t %1$s = 0; %1$s < %2$s; %1$s++) {", temp, repeat);
            emitter().increaseIndentation();
            emitter().emit("%s.write(%s[%s]);",portName, value, temp);
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
        backend().callables().declareEnvironmentForCallablesInScope(block);
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
        emitter().emit("while (true) {");
        emitter().increaseIndentation();
        emitter().emit("if (!%s) break;", evaluate(stmt.getCondition()));
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
