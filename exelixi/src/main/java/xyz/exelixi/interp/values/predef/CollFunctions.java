package xyz.exelixi.interp.values.predef;

import xyz.exelixi.interp.Interpreter;
import xyz.exelixi.interp.Stack;
import xyz.exelixi.interp.TypeConverter;
import xyz.exelixi.interp.values.Function;
import xyz.exelixi.interp.values.Iterator;
import xyz.exelixi.interp.values.List;
import xyz.exelixi.interp.values.Range;
import xyz.exelixi.interp.values.RefView;
import xyz.exelixi.interp.values.Value;

public class CollFunctions {
	public static class ListAccumulate implements Function {
		private TypeConverter conv = TypeConverter.getInstance();
		@Override
		public final Value copy() {
			return this;
		}

		@Override
		public final RefView apply(Interpreter interpreter) {
			Stack stack = interpreter.getStack();
			List list = conv.getList(stack.pop());
			RefView accValue = stack.pop();
			Function function = conv.getFunction(stack.pop());
			Iterator iter = list.iterator();
			while(!iter.finished()){
				accValue.assignTo(stack.push());
				iter.assignTo(stack.push());
				accValue = function.apply(interpreter);
				iter.advance();
			}
			return accValue;
		}
	}


	public static class IntegerRange implements Function {

		private TypeConverter conv = TypeConverter.getInstance();

		@Override
		public final Value copy() {
			return this;
		}

		@Override
		public final RefView apply(Interpreter interpreter) {
			Stack stack = interpreter.getStack();
			int to = conv.getInt(stack.pop());
			int from = conv.getInt(stack.pop());
			conv.setCollection(stack.push(), new Range(from, to));
			return stack.pop();
		}
	}

}
