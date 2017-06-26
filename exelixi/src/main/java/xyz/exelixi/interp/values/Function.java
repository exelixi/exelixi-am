package xyz.exelixi.interp.values;

import xyz.exelixi.interp.Interpreter;

public interface Function extends Value {
	
	/**
	 * Evaluate the body of the function and removes the argument from the stack.
	 * @param interpreter is used to evaluating the body.
	 * @return
	 */
	public RefView apply(Interpreter interpreter);
}
