package xyz.exelixi.interp;

import xyz.exelixi.interp.values.Ref;
import xyz.exelixi.interp.values.RefView;

public interface Stack {
	public Ref pop();

	public void remove(int n);

	public void push(RefView r);
	
	public Ref push();
	
	public void alloca(int n);

/**
 * @param i , 0 = top of stack
 */
	public Ref peek(int i);

	public Ref closure(int select);

	public boolean isEmpty();

}
