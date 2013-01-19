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
 * Subscription filter used by a node to notify to Discovery Service that it 
 * is available for engagement by other nodes. The filter contains the status
 * of the node that perform the subscription. The node sends the filter to 
 * the broker in the Discovery Service through subscription.
 * 
 * When the broker receive a message it calls the methods <code>matches(Message msg)</code>
 * on the instance of <code>EngageMessageFilter</code>. So the implementation
 * of the method is in charge of verifying that the message is an instance
 * of {@link EngageMessage} and if so it extracts the {@link StatusFilter}
 * instance to perform an invocation of the method 
 * <code>StatusFilter.matches(Message msg). The argument passed to the method is
 * the instance of {@link Status} registered with the <code>EngageMessageFilter</code>.
 *  
 * @author leleplx@gmail.com (emanuele)
 *
 */
public class EngageMessageFilter implements Filter {
	private static final long serialVersionUID = -1845875821187009553L;
	private Status status;
	
	public EngageMessageFilter(String nodeName, String address, Object status) {
		this.status = new Status();
		this.status.setNodeName(nodeName);
		this.status.setAddress(address);
		this.status.setStatus(status);
	}
	
	public EngageMessageFilter(Status status) {
		this.status = status;
	}
	
	public String getNodeName() {
		return status.getNodeName();
	}
	
	public String getAddress() {
		return status.getAddress();
	}
	
	public Object getStatus() {
		return status.getStatus();
	}
	
	@Override
	public boolean matches(Message msg) {
		if (!(msg instanceof EngageMessage))
			return false;
		else {
			EngageMessage em = (EngageMessage)msg;
			return em.getStatusFilter().matches(status);
		}
	}
	
	public String toString() {
		return "EngageMessageFilter[" + status.toString() + "]";
	}

}
