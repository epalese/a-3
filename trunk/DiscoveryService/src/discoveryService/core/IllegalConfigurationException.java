package discoveryService.core;

public class IllegalConfigurationException extends IllegalArgumentException {
	private static final long serialVersionUID = -8283189470624318525L;
	private String error;
	
	public IllegalConfigurationException(String msg) {
		super();
		error = msg;
	}
	
	public String getError() {
		return error;
	}

}
