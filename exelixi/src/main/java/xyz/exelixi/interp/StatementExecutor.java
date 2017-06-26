package xyz.exelixi.interp;

import se.lth.cs.tycho.ir.stmt.lvalue.*;
import xyz.exelixi.interp.values.BasicRef;
import xyz.exelixi.interp.values.List;
import xyz.exelixi.interp.values.Procedure;
import xyz.exelixi.interp.values.Ref;
import xyz.exelixi.interp.values.RefView;
import se.lth.cs.tycho.ir.decl.VarDecl;
import se.lth.cs.tycho.ir.expr.Expression;
import se.lth.cs.tycho.ir.stmt.Statement;
import se.lth.cs.tycho.ir.stmt.StatementVisitor;
import se.lth.cs.tycho.ir.stmt.StmtAssignment;
import se.lth.cs.tycho.ir.stmt.StmtBlock;
import se.lth.cs.tycho.ir.stmt.StmtCall;
import se.lth.cs.tycho.ir.stmt.StmtConsume;
import se.lth.cs.tycho.ir.stmt.StmtForeach;
import se.lth.cs.tycho.ir.stmt.StmtIf;
import se.lth.cs.tycho.ir.stmt.StmtWhile;
import se.lth.cs.tycho.ir.util.ImmutableList;

public class StatementExecutor implements StatementVisitor<Void, Environment>, LValueVisitor<Ref, Environment> {

	private final Interpreter interpreter;
	private final TypeConverter conv;
	private final Stack stack;
	//private final GeneratorFilterHelper gen;
	private final TypeConverter converter;

	public StatementExecutor(Interpreter interpreter) {
		this.interpreter = interpreter;
		this.conv = TypeConverter.getInstance();
		this.stack = interpreter.getStack();
		//this.gen = new GeneratorFilterHelper(interpreter);
		this.converter = TypeConverter.getInstance();
	}

	private void execute(Statement stmt, Environment env) {
		stmt.accept(this, env);
	}

	
	@Override
	public Void visitStmtAssignment(StmtAssignment stmt, Environment env) {
		RefView value = interpreter.evaluate(stmt.getExpression(), env);
		Ref memCell = stmt.getLValue().accept(this, env);
		value.assignTo(memCell);
		return null;
	}

	/**
	 * Return the memory location of the lhs
	 */
	@Override
	public Ref visitLValueVariable(LValueVariable lvalue, Environment env) {
		/*
		VariableLocation var = (VariableLocation)lvalue.getVariable();
		if(var.isScopeVariable()){
			return env.getMemory().get(var);
		} else {
			return stack.peek(var.getOffset());
		}*/
		return null;
	}

	/**
	 * Return the memory location of the lhs
	 */
	@Override
	public Ref visitLValueIndexer(LValueIndexer lvalue, Environment env) {
		RefView index = interpreter.evaluate(lvalue.getIndex(), env);
		int i = converter.getInt(index);
		Ref structure = lvalue.getStructure().accept(this, env);
		List l = converter.getList(structure);
		return l.getRef(i);
	}

	/**
	 * Return the memory location of the lhs
	 */
	@Override
	public Ref visitLValueField(LValueField lvalue, Environment env) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("field at lhs of assignment");
	}

	@Override
	public Ref visitLValueDeref(LValueDeref lvalue, Environment parameter) {
		return null;
	}

	@Override
	public Void visitStmtConsume(StmtConsume s, Environment p) {
		/*
		Channel.OutputEnd source = p.getSourceChannelOutputEnd(s.getPort().getOffset());
		source.remove(s.getNumberOfTokens());
		*/
		return null;
	}

	@Override
	public Void visitStmtBlock(StmtBlock stmt, Environment env) {
		if(!stmt.getTypeDecls().isEmpty()) {
			throw new UnsupportedOperationException();
		}
		for (VarDecl d : stmt.getVarDecls()) {
			if(d.getValue() != null){
				stack.push(interpreter.evaluate(d.getValue(), env));				
			} else {
				stack.push();
			}
		}
		for (Statement s : stmt.getStatements()) {
			execute(s, env);
		}
		stack.remove(stmt.getVarDecls().size());
		return null;
	}

	@Override
	public Void visitStmtIf(StmtIf stmt, Environment env) {
		RefView condRef = interpreter.evaluate(stmt.getCondition(), env);
		boolean cond = conv.getBoolean(condRef);
		/*
		if (cond) {
			execute(stmt.getThenBranch(), env);
		} else {
			execute(stmt.getElseBranch(), env);
		}
		*/
		return null;
	}

	@Override
	public Void visitStmtCall(StmtCall stmt, Environment env) {
		RefView r = interpreter.evaluate(stmt.getProcedure(), env);
		Procedure p = conv.getProcedure(r);
		ImmutableList<Expression> argExprs = stmt.getArgs();
		for (Expression arg : argExprs) {
			stack.push(interpreter.evaluate(arg, env));
		}
		//TODO, closure
		p.exec(interpreter);
		stack.remove(stmt.getArgs().size());
		return null;
	}

	/*
	@Override
	public Void visitStmtOutput(StmtOutput stmt, Environment env) {
		Channel.InputEnd channel = env.getSinkChannelInputEnd(stmt.getPort().getOffset());
		if (stmt.hasRepeat()) {
			ImmutableList<Expression> exprList = stmt.getValues();
			BasicRef[] values = new BasicRef[exprList.size()];
			for (int i = 0; i < exprList.size(); i++) {
				values[i] = new BasicRef();
				interpreter.evaluate(exprList.get(i), env).assignTo(values[i]);
			}
			for (int r = 0; r < stmt.getRepeat(); r++) {
				for (BasicRef v : values)
					channel.write(v);
			}
		} else {
			ImmutableList<Expression> exprList = stmt.getValues();
			for (Expression expr : exprList) {
				channel.write(interpreter.evaluate(expr, env));
			}
		}
		return null;
	}
*/
	@Override
	public Void visitStmtWhile(StmtWhile stmt, Environment env) {
		/*
		while (conv.getBoolean(interpreter.evaluate(stmt.getCondition(), env))) {
			execute(stmt.getBody(), env);
		}
		*/
		return null;
	}

	@Override
	public Void visitStmtForeach(final StmtForeach stmt, final Environment env) {
		/*
		Runnable execStmt = new Runnable() {
			public void run() {
				execute(stmt.getBody(), env);
			}
		};
		gen.interpret(stmt.getGenerator(), execStmt, env);
		*/
		return null;

	}

}
