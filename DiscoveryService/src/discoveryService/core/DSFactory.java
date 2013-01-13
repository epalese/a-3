package discoveryService.core;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

import polimi.reds.DispatchingService;
import polimi.reds.TCPDispatchingService;
import discoveryService.broker.Broker;
import discoveryService.broker.TCPReplyCapableBroker;

public class DSFactory {
	private static Logger logger = Logger.getLogger(DSFactory.class);
	public static DispatchingService createDispatchingService(DSConfiguration conf) {
		// TCP Dispatcher
		if (conf.getProtocol().equals(DSConfiguration.TCP)) {
			String address = conf.getBrokerAddress();
			int port = conf.getBrokerPort();
			return new TCPDispatchingService(address, port);
		}
		
		return null;
	}
	
	/**
	 * Istanzia un broker REDS in base ai parametri di configurazione passati in un'istanza di DSConfiguration.
	 * Dopo aver creato un'istanza del broker che soddisfi la configurazione richiesta, lo avvia e aggiunge o cerca i broker
	 * vicini 
	 * @param conf istanza di DSConfiguration che definisce alcuni parametri di configurazione del broker (ad es. protocollo
	 * TCP o UDP)
	 * @return un'istanza di broker REDS che rispetta i parametri passati come configurazione 
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
		
		return null;
	}
}
