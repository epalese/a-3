package discoveryService.core;

import polimi.reds.Message;

public class DSMessage extends Message {
	private static final long serialVersionUID = -7684705933007103294L;
	protected String nodeName;
	
	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}
	
	public String getNodeName() {
		return nodeName;
	}
}
