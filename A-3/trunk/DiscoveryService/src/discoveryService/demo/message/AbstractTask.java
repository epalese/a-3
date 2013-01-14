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

import polimi.reds.Repliable;
import discoveryService.core.DSMessage;

public abstract class AbstractTask extends DSMessage implements Repliable {
	private static final long serialVersionUID = 1595495262590364937L;
	protected String destination;
	protected TaskResult taskResult;
	
	public abstract void perform();
	
	public void setTaskResult(TaskResult result) {
		taskResult = result;
	}
	
	public TaskResult getTaskResult() {
		// Setta l'attributo destination di TaskResult al valore del nodeName
		// che ha mandato il task e che dovrà ricevere il risultato
		taskResult.setDestination(nodeName);
		return taskResult;
	}
	
	public String getDestination() {
		return destination;
	}
	
	public void setDestination(String destination) {
		this.destination = destination;
	}
}
