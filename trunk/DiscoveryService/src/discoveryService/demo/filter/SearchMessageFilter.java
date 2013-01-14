package discoveryService.demo.filter;

import polimi.reds.Filter;
import polimi.reds.Message;
import discoveryService.demo.message.SearchMessage;
import discoveryService.demo.message.ServiceDescriptor;

public class SearchMessageFilter implements Filter {
	private static final long serialVersionUID = 4751645322673605758L;
	private ServiceDescriptor mySD;
	
	public SearchMessageFilter(ServiceDescriptor myServiceDescriptor) {
		mySD = myServiceDescriptor;
	}
	
	public ServiceDescriptor getServiceDescriptor() {
		return mySD;
	}
	
	public int hashCode() {
		return "SearchMessageFilter".concat(mySD.getNodeName().concat(mySD.getServiceID().concat(mySD.getQos()))).hashCode();
	}

	@Override
	public boolean matches(Message arg0) {
		if (!(arg0 instanceof SearchMessage))
			return false;
		else {
			SearchMessage sm = (SearchMessage)arg0;
			return sm.getServiceDescriptorFilter().matches(mySD);
		}
	}

}
