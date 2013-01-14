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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

/**
 * <code>DSConfiguration</code> encapsulates all the parameters required to set up and start a node in the
 * Discovery Service. 
 * @author leleplx@gmail.com (emanuele)
 *
 */
public class DSConfiguration {
	public static final String TCP = "tcp";
	public static final String UDP = "udp";

	private String nodeName;						// node name
	private String protocol;						// tcp|udp|tcpl
	private boolean createBroker = true;			// yes|no	
	private int brokerPort;							// TCP port number
	private String brokerAddress;					// IP address
	private String[] brokerNeighbors;				// neighbors IP address
	
	/*
	 * Used when there is no neighbor to conect to and the broker node is in charge to locate 
	 * neighbors through network scanning. A value equals to 0 means that there is no limit
	 * to the numbers of neighbors that can be added.
	 */
	private int maxNumOfNeighbors;		
	
	/**
	 * Create a dummy instance of <code>DSConfiguration</code>.
	 * Use respective methods to set the values of the attributes.
	 */
	public DSConfiguration() {
		nodeName = null;
		protocol = null;
		brokerPort = 0;
		brokerAddress = null;
		//localPort = 0;
		brokerNeighbors = null;
		maxNumOfNeighbors = 0;
	}
	
	/**
	 * Create an instance of <code>DSConfiguration</code> fetching the value of parameters from a 
	 * configuration file.
	 *  
	 * @param fileName the name of the configuration file (with path). The configuration file is a normal txt.
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public DSConfiguration(String fileName) throws FileNotFoundException, IOException {
		BufferedReader input = new BufferedReader(new FileReader(fileName));
		String line = null;
		while ((line = input.readLine()) != null)
			parser(line);
		input.close();
	}
	
	/**
	 * Parse a single line of the configuration file fetching the value for the corresponding parameter.
	 * 
	 * @param line
	 */
	private void parser(String line) {
		if ((line.length() > 0) && (line.charAt(0) == '#'))
			return;

		String[] element = line.trim().split("=");
		String declaration = element[0].trim();		
		String value = null;
		if (element.length > 1)
			value = element[1].trim();
		
		if (declaration.equalsIgnoreCase("nodeName")) {
			setNodeName(value);
		}
		else if (declaration.equalsIgnoreCase("protocol")) {
			setProtocol(value);
		}
		else if (declaration.equalsIgnoreCase("brokerPort")) {
			int intValue = Integer.valueOf(value).intValue();
			setBrokerPort(intValue);
		}
		else if (declaration.equalsIgnoreCase("brokerAddress")) {
			setBrokerAddress(value);
		}
		else if (declaration.equalsIgnoreCase("brokerNeighbors")) {
			String[] neighs = value.split("[\\p{Space}]+");
			setBrokerNeighbors(neighs);
		}
		else if (declaration.equalsIgnoreCase("maxNumOfNeighbors")) {
			int intValue = Integer.valueOf(value).intValue();
			setMaxNumOfNeighbors(intValue);
		}
		else if (declaration.equalsIgnoreCase("createBroker")) {
			if (value.equalsIgnoreCase("no"))
				createBroker = false;
		}
	}
	
	/**
	 * Set the name of the node. Every node has a string identifier.
	 * 
	 * @param name
	 */
	public void setNodeName(String name) {
		nodeName = name;
	}
	
	public String getNodeName() {
		return nodeName;
	}
	
	/**
	 * Set the protocol used by the Discovery Service node.
	 * 
	 * @param protocol Protocol values: tcp|udp
	 */
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	
	public String getProtocol() {
		return protocol;
	}
	
	/**
	 * Set the address of the node
	 * 
	 * @param address E.g. 127.0.0.1
	 */
	public void setBrokerAddress(String address) {
		brokerAddress = address;
	}
	
	public String getBrokerAddress() {
		return brokerAddress;	
	}
	
	/**
	 * Set the port number of the node used to connect to other brokers.
	 * 
	 * @param port
	 */
	public void setBrokerPort(int port) {
		brokerPort = port;
	}
	
	public int getBrokerPort() {
		return brokerPort;
	}
	
	/**
	 * Set a list of neighbors to connect to.
	 * 
	 * @param neighbors The parameters is an array of addresses ( e.g. ["reds-tcp:127.0.0.1:1911", "...", ""] ).
	 */
	public void setBrokerNeighbors(String[] neighbors) {
		brokerNeighbors = neighbors;
	}
	
	public String[] getBrokerNeighbors() {
		return brokerNeighbors;
	}
	
	public void setMaxNumOfNeighbors(int num) {
		maxNumOfNeighbors = num;
	}
	
	public int getMaxNumOfNeighbors() {
		return maxNumOfNeighbors;
	}
	
	/**
	 * A Discovery Service node can be composed only by the Dispatching Service (that is like an interface used
	 * to connect the node to the dispatching network made up by all the brokers that are in charge of delivery 
	 * messages and subscriptions) or it can run also an instance of a broker.
	 * 
	 * @param cb If true the Discovery Service create a node composed by Dispatching Service and Broker.
	 */
	public void setCreateBroker(boolean cb) {
		createBroker = cb;
	}
	
	public boolean getCreateBroker() {
		return createBroker;
	}
	
	public String toString() {
		return super.toString() + "\n" + 
					"nodeName= " + nodeName + "\n" +
					"protocol= " + protocol + "\n" +
					"brokerPort= " + brokerPort + "\n" +
					"brokerAddress= " + brokerAddress + "\n" +
					(String)(((brokerNeighbors == null) || (brokerNeighbors.length == 0))? "" : Arrays.asList(brokerNeighbors)) +
					"maxNumOfNeighbors= " + maxNumOfNeighbors;				
	}
}
