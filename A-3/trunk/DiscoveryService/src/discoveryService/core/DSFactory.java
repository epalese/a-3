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

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

import polimi.reds.DispatchingService;
import polimi.reds.TCPDispatchingService;
import discoveryService.broker.Broker;
import discoveryService.broker.TCPReplyCapableBroker;

/**
 * This class provides helper methods to create instances of the different
 * components that can take part in a node of Discovery Service: Dispatching
 * Service and Broker.
 * 
 * @author leleplx@gmail.com (emanuele)
 *
 */
public class DSFactory {
	private static Logger logger = Logger.getLogger(DSFactory.class);
	
	/**
	 * Create an instance of {@link DispatchingService} using the parameters 
	 * passed through an instance of {@link DSConfiguration}.
	 * 
	 * @param conf An instance of {@link DSConfiguration} with the parameters used to
	 * set up the broker.
	 * 
	 * @return The instance of {@link DSConfiguration} created.
	 * 
	 * TODO: implement UDP
	 */
	public static DispatchingService createDispatchingService(DSConfiguration conf) {
		// TCP Dispatcher
		if (conf.getProtocol().equals(DSConfiguration.TCP)) {
			String address = conf.getBrokerAddress();
			int port = conf.getBrokerPort();
			return new TCPDispatchingService(address, port);
		}
		/*
		 * TODO: implement UDP Dispatching Service
		 */
		return null;
	}
	
	/**
	 * Create an instance of {@link Broker} using the parameters passed through
	 * an instance of {@link DSConfiguration}.
	 * After creating the broker, the constructor start the broker and search for neighbor nodes.
	 * 
	 * @param conf An instance of {@link DSConfiguration} with the parameters used to
	 * set up the broker.
	 * 
	 * @return The instance of {@link Broker} created.
	 * 
	 * TODO: implement UDP.
	 */
	public static Broker createBroker(DSConfiguration conf) {
		Broker broker; 
		
		// TCP Reply Capable Broker
		if (conf.getProtocol().equals(DSConfiguration.TCP)) {
			String address = null;
			try {
				address = InetAddress.getLocalHost().getHostAddress();
			} catch(UnknownHostException e) { e.printStackTrace(); }
			
			if(address==null)
				return null;			
			
			broker = new TCPReplyCapableBroker(address, conf.getBrokerPort());
			broker.start();
			logger.info("Broker started!");
			
			if (conf.getBrokerNeighbors() != null) {
				for (String neighbor : conf.getBrokerNeighbors())
					broker.addNeighbor(neighbor);
			}
			else {
				broker.locateNeighbors(conf.getMaxNumOfNeighbors());
				logger.info("Neighbors added!");
			}
			return broker;
		}
		/*
		 * TODO: implement UDP Dispatching Service
		 */
		
		return null;
	}
}
