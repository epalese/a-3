package discoveryService.demo.filter;

import polimi.reds.Filter;
import polimi.reds.Message;
import discoveryService.demo.message.AbstractTask;

public class TaskFilter implements Filter {
	private static final long serialVersionUID = -7673304404110438522L;
	private String destNode;
	
	public TaskFilter(String nodeName) {
		destNode = nodeName;
	}
	
	public String getDestinationNode() {
		return destNode;
	}
	
	public void setDestinationNode(String nodeName) {
		destNode = nodeName;
	}
	@Override
	public boolean matches(Message arg0) {
		if (!(arg0 instanceof AbstractTask))
			return false;
		AbstractTask task = (AbstractTask)arg0;
		if (task.getDestination().equals(destNode))
			return true;
		else
			return false;
		
	}

}
