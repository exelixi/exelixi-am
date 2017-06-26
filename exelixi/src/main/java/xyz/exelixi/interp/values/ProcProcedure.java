package xyz.exelixi.interp.values;

import xyz.exelixi.interp.Environment;
import xyz.exelixi.interp.Interpreter;
import se.lth.cs.tycho.ir.expr.ExprProc;

public class ProcProcedure implements Procedure {

	public final ExprProc proc;
	public final Environment closure;

	public ProcProcedure(ExprProc proc, Environment closure) {
		this.proc = proc;
		this.closure = closure;
	}

	@Override
	public Value copy() {
		return this;
	}

	@Override
	public void exec(Interpreter interpreter) {
		// FIXME interpreter.execute(proc.getBody(), closure);
	}

}
