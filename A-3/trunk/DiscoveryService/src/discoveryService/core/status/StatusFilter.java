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

/**
 * It defines the matching criteria on the status of a node used during 
 * an engagement request.
 * These criteria are specified by the node that wants to perform the engagement
 * and are encapsulated in an instance of <code>StatusFilter</code> and
 * inserted in the instance of <code>EngageMessage</code>. The instance of
 * <code>EngageMessage</code> is then sent in the service node and delivered
 * to the required node.
 * 
 * <code>StatusFilter</code> implements the method <code>matches(Message msg)</code> 
 * that receive as argument an instance of Status and is called by the node 
 * that receive the <code>EngageMessage</code> that will pass as argument its own
 * <code>Status<code> instance. So the method <code>StatusFilter.mataches(Message msg)</code>
 * perform its operations on the Status of the receiving node.
 *  
 * @author leleplx@gmail.com (emanuele)
 *
 */
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
		
		/**
		 * TODO: at the moment the match is based only on nodeName.
		 * Extend the matching criteria allowing complex way to compare
		 * Status.
		 */
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
