package xyz.exelixi.interp.values.predef;

import xyz.exelixi.interp.Interpreter;
import xyz.exelixi.interp.Stack;
import xyz.exelixi.interp.TypeConverter;
import xyz.exelixi.interp.values.Function;
import xyz.exelixi.interp.values.RefView;
import xyz.exelixi.interp.values.Value;

public class BoolFunctions {

	private static abstract class LogicOp implements Function {

		private TypeConverter conv = TypeConverter.getInstance();

		@Override
		public final Value copy() {
			return this;
		}

		@Override
		public final RefView apply(Interpreter interpreter) {
			Stack stack = interpreter.getStack();
			boolean b = conv.getBoolean(stack.pop());
			boolean a = conv.getBoolean(stack.pop());
			conv.setBoolean(stack.push(), op(a, b));
			return stack.pop();
		}

		protected abstract boolean op(boolean a, boolean b);
	}

	public static class And extends LogicOp {
		protected final boolean op(boolean a, boolean b) {
			return a && b;
		}
	}

	public static class Or extends LogicOp {
		protected final boolean op(boolean a, boolean b) {
			return a || b;
		}
	}

	public static class Not implements Function {

		private TypeConverter conv = TypeConverter.getInstance();

		@Override
		public final Value copy() {
			return this;
		}

		@Override
		public final RefView apply(Interpreter interpreter) {
			Stack stack = interpreter.getStack();
			boolean b = conv.getBoolean(stack.pop());
			conv.setBoolean(stack.push(), !b);
			return stack.pop();
		}
	}

}
