package discoveryService.demo.filter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import polimi.reds.Filter;
import polimi.reds.Message;
import discoveryService.demo.message.ServiceDescriptor;

public class ServiceDescriptorFilter implements Filter {
	private static final long serialVersionUID = 5838338101131219756L;
	private ServiceDescriptor sdTemplate;
	
	public ServiceDescriptorFilter(ServiceDescriptor template) {
		super();
		sdTemplate = template;
	}
	
	public void setServiceDescriptorTemplate(ServiceDescriptor template) {
		sdTemplate = template;
	}
	
	public ServiceDescriptor getServiceDescriptorTemplate() {
		return sdTemplate;
	}
	
	
	@Override
	public boolean matches(Message arg0) {
		boolean nodeNameCond = false, 
				serviceIDCond = false, 
				qosCond = false;
		
		if (!(arg0 instanceof ServiceDescriptor))
			return false;
		
		ServiceDescriptor sd = (ServiceDescriptor)arg0;
		// Il valore null di un attributo nel template viene considerato come la wildcard *
		if (sdTemplate.getNodeName() == null)
			nodeNameCond = true;
		else {
			Pattern p = Pattern.compile(sdTemplate.getNodeName());
			Matcher m = p.matcher(sd.getNodeName());
			nodeNameCond = m.find();
		}
		
		if (sdTemplate.getServiceID() == null)
			serviceIDCond = true;
		else {
			Pattern p = Pattern.compile(sdTemplate.getServiceID());
			Matcher m = p.matcher(sd.getServiceID());
			serviceIDCond = m.find();
		}
		
		if (sdTemplate.getQos() == null)
			qosCond = true;
		else {
			Pattern p = Pattern.compile(sdTemplate.getQos());
			Matcher m = p.matcher(sd.getQos());
			qosCond = m.find();
		}
		
		return nodeNameCond && serviceIDCond && qosCond;
	}
	
	public String toString() {
		return "ServiceDescriptorFilter[ nodeName: " + sdTemplate.getNodeName() + 
										", serviceID: " + sdTemplate.getServiceID() + 
										", qos: " + sdTemplate.getQos() + "]";
	}

}
