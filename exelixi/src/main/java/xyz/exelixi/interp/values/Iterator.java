package xyz.exelixi.interp.values;

public interface Iterator extends RefView {
	public boolean finished();
	public void advance();
}
