package discoveryService.demo.message;

import polimi.reds.Repliable;
import discoveryService.core.DSMessage;
import discoveryService.demo.filter.ServiceDescriptorFilter;

public class SearchMessage extends DSMessage implements Repliable {
	private static final long serialVersionUID = 3533971069026465720L;
	private ServiceDescriptorFilter filter;
	
	public SearchMessage(ServiceDescriptorFilter filter) {
		super();
		this.filter = filter;
	}
	
	public void setServiceDescriptorFilter(ServiceDescriptorFilter filter) {
		this.filter = filter;
	}
	
	public ServiceDescriptorFilter getServiceDescriptorFilter() {
		return filter;
	}
	
	public String toString() {
		return "SearchMessage {" + filter + "}";
	}
	
}
