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

import discoveryService.core.DSMessage;

/**
 * Provide an abstraction for the internal status of the node.
 * It provides three basic attributes for the status:
 * - nodeName: a string identifying the name of the node
 * - address: the IP address of the node
 * - status: a reference to a generic object that can be used by the
 *           user to store other relevant information.
 *            
 * @author leleplx@gmail.com (emanuele)
 *
 */
public class Status extends DSMessage {
	private static final long serialVersionUID = 3675942301627310972L;
	private String address;
	private Object status;
	
	public Status(String nodeName, String address, Object status) {
		this.nodeName = nodeName;
		this.address = address;
		this.status = status;
	}
	
	public Status() {
		nodeName = null;
		address = null;
		status = null;
	}
	
	public void setAddress(String address) {
		this.address = address;
	}
	
	public String getAddress() {
		return address;
	}
	
	public void setStatus(Object status) {
		this.status = status;
	}
	
	public Object getStatus() {
		return status;
	}
	
	public String toString() {
		return "Status:["+nodeName+"; " + address + "; " + status + "]";
	}
	
}
