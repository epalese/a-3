package discoveryService.demo.filter;

import polimi.reds.Filter;
import polimi.reds.Message;
import discoveryService.demo.message.TaskResult;

public class TaskResultFilter implements Filter {
	private static final long serialVersionUID = 6166633499257509103L;
	private String destNode;
	
	public TaskResultFilter(String nodeName) {
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
		if (!(arg0 instanceof TaskResult))
			return false;
		TaskResult result = (TaskResult)arg0;
		if (result.getDestination().equals(destNode))
			return true;
		else
			return false;
		
	}
}
