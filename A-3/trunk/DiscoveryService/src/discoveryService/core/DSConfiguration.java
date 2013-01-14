package discoveryService.core;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

/**
 * DSConfiguration encapsulates all the parameters required to set up and start 
 * @author leleplx@gmail.com (emanuele)
 *
 */
public class DSConfiguration {
	public static final String TCP = "tcp";
	public static final String UDP = "udp";

	// nome del nodo
	private String nodeName;
	// es: tcp|udp|tcpl
	private String protocol;
	// es: yes|no
	private boolean createBroker = true;
	// es: 1911
	private int brokerPort;
	// es: 127.0.0.1
	private String brokerAddress;
	// es: 1181
	//private int localPort;
	// es: reds-tcp:127.0.0.1:1911
	private String[] brokerNeighbors;
	// usato nel caso in cui nn vengono indicati i neighbors ed è necessario trovarli
	// un valore di 0 indica nessun limite
	private int maxNumOfNeighbors;
	
	public DSConfiguration() {
		nodeName = null;
		protocol = null;
		brokerPort = 0;
		brokerAddress = null;
		//localPort = 0;
		brokerNeighbors = null;
		maxNumOfNeighbors = 0;
	}
	 
	public DSConfiguration(String fileName) throws FileNotFoundException, IOException {
		BufferedReader input = new BufferedReader(new FileReader(fileName));
		String line = null;
		while ((line = input.readLine()) != null)
			parser(line);
		input.close();
	}
	
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
	
	public void setNodeName(String name) {
		nodeName = name;
	}
	
	public String getNodeName() {
		return nodeName;
	}
	
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	
	public String getProtocol() {
		return protocol;
	}
	
	public void setBrokerAddress(String address) {
		brokerAddress = address;
	}
	
	public String getBrokerAddress() {
		return brokerAddress;	
	}
	
	public void setBrokerPort(int port) {
		brokerPort = port;
	}
	
	public int getBrokerPort() {
		return brokerPort;
	}
	
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
