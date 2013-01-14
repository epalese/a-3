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
