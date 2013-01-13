package discoveryService.core.status;

import polimi.reds.Repliable;
import discoveryService.core.DSMessage;

public class EngageMessage extends DSMessage implements Repliable {
	private static final long serialVersionUID = 5360422068966862615L;
	private StatusFilter filter;
	
	public EngageMessage(StatusFilter filter) {
		super();
		this.filter = filter;
	}
	
	public void setStatusFilter(StatusFilter filter) {
		this.filter = filter;
	}
	
	public StatusFilter getStatusFilter() {
		return filter;
	}
	
	public String toString() {
		return nodeName + ": EngageMessage[" + filter.toString() + "]";
	}
}
