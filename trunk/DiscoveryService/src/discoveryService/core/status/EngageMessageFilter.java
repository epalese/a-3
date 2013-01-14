package discoveryService.core.status;

import polimi.reds.Filter;
import polimi.reds.Message;

public class EngageMessageFilter implements Filter {
	private static final long serialVersionUID = -1845875821187009553L;
	private Status status;
	
	public EngageMessageFilter(String nodeName, String address, Object status) {
		this.status = new Status();
		this.status.setNodeName(nodeName);
		this.status.setAddress(address);
		this.status.setStatus(status);
	}
	
	public EngageMessageFilter(Status status) {
		this.status = status;
	}
	
	public String getNodeName() {
		return status.getNodeName();
	}
	
	public String getAddress() {
		return status.getAddress();
	}
	
	public Object getStatus() {
		return status.getStatus();
	}
	
	@Override
	public boolean matches(Message msg) {
		if (!(msg instanceof EngageMessage))
			return false;
		else {
			EngageMessage em = (EngageMessage)msg;
			return em.getStatusFilter().matches(status);
		}
	}
	
	public String toString() {
		return "EngageMessageFilter[" + status.toString() + "]";
	}

}
