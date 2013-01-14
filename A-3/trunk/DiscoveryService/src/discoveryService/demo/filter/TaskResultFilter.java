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

import polimi.reds.Filter;
import polimi.reds.Message;
import discoveryService.demo.message.TaskResult;

public class TaskResultFilter implements Filter {
	private static final long serialVersionUID = 6166633499257509103L;
	private String destNode;
	
	public TaskResultFilter(String nodeName) {
		destNode = nodeName;
	}
	
	public String getDestinationNode() {
		return destNode;
	}
	
	public void setDestinationNode(String nodeName) {
		destNode = nodeName;
	}
	
	@Override
	public boolean matches(Message arg0) {
		if (!(arg0 instanceof TaskResult))
			return false;
		TaskResult result = (TaskResult)arg0;
		if (result.getDestination().equals(destNode))
			return true;
		else
			return false;
		
	}
}
