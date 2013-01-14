/***
 * * A-3 DiscoveryService
 * * <mailto: leleplx@gmail.com>
 * *
 * * This library is free software; you can redistribute it and/or modify it
 * * under the terms of the GNU Lesser General Public License as published by
 * * the Free Software Foundation; either version 2.1 of the License, or (at
 * * your option) any later version.
 * *
 * * This library is distributed in the hope that it will be useful, but
 * * WITHOUT ANY WARRANTY; without even the implied warranty of
 * * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * * General Public License for more details.
 * *
 * * You should have received a copy of the GNU Lesser General Public License
 * * along with this library; if not, write to the Free Software Foundation,
 * * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

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
