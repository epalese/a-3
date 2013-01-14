package discoveryService.demo.message;

import polimi.reds.Repliable;
import discoveryService.core.DSMessage;

public abstract class AbstractTask extends DSMessage implements Repliable {
	private static final long serialVersionUID = 1595495262590364937L;
	protected String destination;
	protected TaskResult taskResult;
	
	public abstract void perform();
	
	public void setTaskResult(TaskResult result) {
		taskResult = result;
	}
	
	public TaskResult getTaskResult() {
		// Setta l'attributo destination di TaskResult al valore del nodeName
		// che ha mandato il task e che dovrà ricevere il risultato
		taskResult.setDestination(nodeName);
		return taskResult;
	}
	
	public String getDestination() {
		return destination;
	}
	
	public void setDestination(String destination) {
		this.destination = destination;
	}
}
