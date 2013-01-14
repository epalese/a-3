package discoveryService.demo.message;

import discoveryService.core.DSMessage;

public class TaskResult extends DSMessage {
	private static final long serialVersionUID = -3109311544183568943L;
	private String destination;
	private Object result;
	
	public void setResult(Object result) {
		this.result = result;
	}
	
	public Object getResult() {
		return result;
	}
	
	public void setDestination(String node) {
		destination = node;
	}
	
	public String getDestination() {
		return destination;
	}
}
