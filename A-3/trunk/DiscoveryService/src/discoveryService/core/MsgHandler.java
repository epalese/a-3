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
 * Core class used by DSCore to process messages received from the neighbor nodes in
 * the overlay network of the service. 
 * DSCore instantiates a new thread that executes the <code>run()</code> methods. When a message
 * arrives at the node the method <code>process(DSMessage msg)</code> should be called
 * to process the message.
 * 
 * You never are supposed to extend this class for a normal use of Discovery Service. 
 * DSCore internally provides an implementation of it.
 *  
 * @author leleplx@gmail.com (emanuele)
 *
 */
public abstract class MsgHandler extends Thread {
	abstract public void process(DSMessage msg);
	abstract public void run();
	abstract public void stopProcess();
}
