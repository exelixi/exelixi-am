package xyz.exelixi.interp.values;

public class BasicRef implements Ref {

	private static enum Type {
		VALUE, LONG, DOUBLE, STRING
	}

	private Type type;
	private Value value;
	private long long_;
	private double double_;
	private String string_;
	
	private void assertType(Type t) {
		if (type != t) {
			throw new IllegalStateException("Wrong type");
		}
	}

	@Override
	public Value getValue() {
		assertType(Type.VALUE);
		return value;
	}

	@Override
	public long getLong() {
		assertType(Type.LONG);
		return long_;
	}

	@Override
	public double getDouble() {
		assertType(Type.DOUBLE);
		return double_;
	}


	@Override
	public String getString() {
		assertType(Type.STRING);
		return string_;
	}

	@Override
	public void setValue(Value v) {
		type = Type.VALUE;
		value = v;
	}

	@Override
	public void setLong(long v) {
		type = Type.LONG;
		long_ = v;
	}

	@Override
	public void setDouble(double v) {
		type = Type.DOUBLE;
		double_ = v;
	}

	@Override
	public void setString(String v) {
		type = Type.STRING;
		string_ = v;
	}

	@Override
	public void assignTo(Ref r) {
		if (type == null) {
			r.clear();
			return;
		}
		switch (type) {
		case LONG:
			r.setLong(long_);
			return;
		case VALUE:
			r.setValue(value.copy());
			return;
		case DOUBLE:
			r.setDouble(double_);
			return;
		case STRING:
			r.setString(string_);
			return;
		}
	}

	@Override
	public void clear() {
		type = null;
	}

	@Override
	public String toString(){
		if(type==null){return "null"; }
		switch (type) {
		case LONG:
			return Long.toString(long_);
		case VALUE:
			return value.toString();
		case DOUBLE:
			return Double.toString(double_);
		default:
			return "unknown";	
		}
	}
}
