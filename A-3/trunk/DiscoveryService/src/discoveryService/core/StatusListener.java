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

import discoveryService.core.status.Status;

/**
 * This interface define a method called when the node receive a status notification
 * from one of the node with whom is in an engagement relationship.
 * The listener should be registered trough <code>DSCore.registerStatusListener</code>
 * 
 * @author leleplx@gmail.com (emanuele)
 *
 */
public interface StatusListener {
	/**
	 * After registering the listener, this method will be called when
	 * the instance of {@link DSCore} receive a status notification.
	 * 
	 * @param msg Instance of <code>Status</code> riceved by <code>DSCore</code>
	 * and passed to the method.
	 *  
	 */
	public void notify(Status status);
}
