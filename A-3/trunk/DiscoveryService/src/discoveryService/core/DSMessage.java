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

package discoveryService.core;

import polimi.reds.Message;

/**
 * The base class for messages exchanged between different nodes of the Discovery
 * Service. This class extends <code>polimi.reds.Message</code> provided by
 * REDS middleware with the attribute <code>nodeName</code> used to identify
 * a node in the service.
 * 
 * @author leleplx@gmail.com (emanuele)
 *
 */
public class DSMessage extends Message {
	private static final long serialVersionUID = -7684705933007103294L;
	protected String nodeName;
	
	/**
	 * Set a string identifier used to identify the node in the service.
	 * 
	 * @param nodeName The name of the node.
	 */
	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}
	
	public String getNodeName() {
		return nodeName;
	}
}
