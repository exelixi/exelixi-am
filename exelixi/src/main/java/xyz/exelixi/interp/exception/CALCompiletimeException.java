package xyz.exelixi.interp.exception;


public class CALCompiletimeException extends RuntimeException{
	// private ErrorModule errors;
	
	public CALCompiletimeException(String msg/*, ErrorModule errors*/) {
		super(msg);
		//this.errors = errors;
	}

	//public ErrorModule getErrorModule(){
	//	return errors;
	//}
	private static final long serialVersionUID = 1L;

}
