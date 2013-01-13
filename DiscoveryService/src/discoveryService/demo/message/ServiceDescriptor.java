package discoveryService.demo.message;

import discoveryService.core.DSMessage;

public class ServiceDescriptor extends DSMessage {
	private static final long serialVersionUID = -5327453957551157713L;
	private String serviceID;
	private String qos;
	
	public ServiceDescriptor() {
		nodeName = null;
		serviceID = null;
		qos = null;
	}
	
	public void setServiceID(String serviceID) {
		this.serviceID = serviceID;
	}
	
	public String getServiceID() {
		return serviceID;
	}
	
	public void setQos(String qos) {
		this.qos = qos;
	}
	
	public String getQos() {
		return qos;
	}
	
	public String toString() {
		return "ServiceDescriptor{ nodeName: " + nodeName + ", serviceID: " + serviceID + ", qos: " + qos + "}";
	}
}
