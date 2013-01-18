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

/**
 * This interface allow users to define a method in reaction to the reception of a 
 * {@link DSMessage}
 * The listener should be registered trough <code>DSCore.registerMessageListener</code>
 *  
 * @author leleplx@gmail.com (emanuele)
 *
 */
public interface MessageListener {
	/**
	 * After registering the listener, this method will be called when
	 * the instance of {@link DSCore} receive a message.
	 * 
	 * @param msg Instance of <code>DSMessage</code> riceved by <code>DSCore</code>
	 * and passed to the method.
	 *  
	 */
	public void notify(DSMessage msg);
}
