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

package discoveryService.core.status;

import polimi.reds.Filter;
import polimi.reds.Message;

public class StatusFilter implements Filter {
	private static final long serialVersionUID = -7392801870949341993L;
	private Status status;
	
	public StatusFilter(String nodeName, String address, Object status) {
		this.status = new Status();
		this.status.setNodeName(nodeName);
		this.status.setAddress(address);
		this.status.setStatus(status);
	}
	
	public StatusFilter(Status status) {
		this.status = status;
	}
	
	public void setStatus(Status status) {
		this.status = status;
	}
	
	public Status getStatus() {
		return status;
	}
	
	@Override
	public boolean matches(Message msg) {
		boolean nodeNameCondition = false;
		boolean addressCondition = false;
		//boolean statusCondition = false;
		
		if (!(msg instanceof Status))
			return false;
		
		Status statusMsg = (Status)msg;
		
		if (status.getNodeName() == null)
			nodeNameCondition = true;
		else
			nodeNameCondition = statusMsg.getNodeName().equals(status.getNodeName());
		
		if (status.getAddress() == null)
			addressCondition = true;
		else
			addressCondition = statusMsg.getAddress().equals(status.getAddress());
		
		// Disabilitato perché per ora l'engage viene fatto solo su nodeName
		// guardare DSCore.engage(String nodeName)
		// Cmq rimane la possibilità di inserire nel messaggio di Status un qualsiasi oggetto (di tipo Matchable)
		/*if (status.getStatus() == null)
			statusCondition = true;
		else
			statusCondition = status.getStatus().matches(statusMsg.getStatus());*/
		
		return nodeNameCondition && addressCondition;	// && statusCondition;
	}
	
	public String toString() {
		return "StatusFilter["+status+"]";
	}

}
