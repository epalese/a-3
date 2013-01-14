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

import polimi.reds.Repliable;
import discoveryService.core.DSMessage;

public class EngageMessage extends DSMessage implements Repliable {
	private static final long serialVersionUID = 5360422068966862615L;
	private StatusFilter filter;
	
	public EngageMessage(StatusFilter filter) {
		super();
		this.filter = filter;
	}
	
	public void setStatusFilter(StatusFilter filter) {
		this.filter = filter;
	}
	
	public StatusFilter getStatusFilter() {
		return filter;
	}
	
	public String toString() {
		return nodeName + ": EngageMessage[" + filter.toString() + "]";
	}
}
