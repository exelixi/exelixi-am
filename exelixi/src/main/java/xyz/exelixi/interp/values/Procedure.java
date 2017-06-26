package xyz.exelixi.interp.values;

import xyz.exelixi.interp.Interpreter;

public interface Procedure extends Value {
	public void exec(Interpreter interpreter);
}
