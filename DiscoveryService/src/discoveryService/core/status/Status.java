package discoveryService.core.status;

import discoveryService.core.DSMessage;

public class Status extends DSMessage {
	private static final long serialVersionUID = 3675942301627310972L;
	private String address;
	private Object status;
	
	public Status(String nodeName, String address, Object status) {
		this.nodeName = nodeName;
		this.address = address;
		this.status = status;
	}
	
	public Status() {
		nodeName = null;
		address = null;
		status = null;
	}
	
	public void setAddress(String address) {
		this.address = address;
	}
	
	public String getAddress() {
		return address;
	}
	
	public void setStatus(Object status) {
		this.status = status;
	}
	
	public Object getStatus() {
		return status;
	}
	
	public String toString() {
		return "Status:["+nodeName+"; " + address + "; " + status + "]";
	}
	
}
