package xyz.exelixi.interp.values;

import xyz.exelixi.interp.Environment;
import xyz.exelixi.interp.Interpreter;
import se.lth.cs.tycho.ir.expr.ExprLambda;

public class LambdaFunction implements Function {

	public final ExprLambda lambda;
	public final Environment closure;

	public LambdaFunction(ExprLambda lambda, Environment closure) {
		this.lambda = lambda;
		this.closure = closure;
	}

	@Override
	public Value copy() {
		return this;
	}

	@Override
	public RefView apply(Interpreter interpreter) {
		RefView result = interpreter.evaluate(lambda.getBody(), closure);
		interpreter.getStack().remove(lambda.getValueParameters().size());
		return result;
	}

}
