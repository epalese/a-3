package discoveryService.broker;

import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import polimi.reds.NodeDescriptor;
import polimi.reds.broker.overlay.AlreadyAddedNeighborException;
import polimi.reds.broker.overlay.GenericOverlay;
import polimi.reds.broker.overlay.Overlay;
import polimi.reds.broker.overlay.SimpleTopologyManager;
import polimi.reds.broker.overlay.TCPTransport;
import polimi.reds.broker.overlay.TopologyManager;
import polimi.reds.broker.overlay.Transport;
import polimi.reds.broker.routing.DeferredUnsubscriptionReconfigurator;
import polimi.reds.broker.routing.GenericRouter;
import polimi.reds.broker.routing.GenericTable;
import polimi.reds.broker.routing.HashReplyTable;
import polimi.reds.broker.routing.ImmediateForwardReplyManager;
import polimi.reds.broker.routing.Reconfigurator;
import polimi.reds.broker.routing.ReplyManager;
import polimi.reds.broker.routing.ReplyTable;
import polimi.reds.broker.routing.RoutingStrategy;
import polimi.reds.broker.routing.SubscriptionForwardingRoutingStrategy;
import polimi.reds.broker.routing.SubscriptionTable;
import polimi.util.Locator;

/**
 * Provide an implementation of {@link Broker} based on TCP connection-oriented protocol.
 * The broker is also able to manage replies.
 * For instance:
 * - node A connect to broker 1 and register a subscription to message of type A
 * - node B connect to broker N and publish a message of type A
 * - broker N dispatch the message to overlay network until it reaches broker 1
 * - broker 1 matches the message with the subscription of node A
 * - broker 1 deliver the message to node A
 * - node A send a reply to the message
 * - the message traverse the overlay network back until node B
 *  
 * @author leleplx@gmail.com (emanuele)
 * 
 *
 */
public class TCPReplyCapableBroker implements Broker {
	/**
	 * Time in milliseconds during which the Locator waits for detecting other brokers.
	 */
	public final static int LOCATOR_SEARCH_TIME = 1000;
	private static Logger logger = Logger.getLogger(TCPReplyCapableBroker.class);
	
	private Transport transport;
	private TopologyManager topManager;
	private Overlay overlay;
	private RoutingStrategy routStrategy;
	private Reconfigurator reconf;
	private GenericRouter router;
	private SubscriptionTable subTable;
	private ReplyManager replyManager;
	private ReplyTable replyTable;
	private Locator locator;
	private String myURL;
	
	/**
	 * Instantiates a broker relying on TCP protocol stack.
	 * This broker is to be used when connection-oriented behaviour is required.
	 *  
	 * @param url broker URL (must be specified without port number e.g.: 127.0.0.1)
	 * @param brokerPort broker port number e.g.: 1911
	 */
	public TCPReplyCapableBroker(String address, int brokerPort) {
		transport = new TCPTransport(brokerPort);
		topManager = new SimpleTopologyManager();
//		topManager = new LSTreeTopologyManager();
		Set<Transport> transports = new HashSet<Transport>();
		transports.add(transport);
		overlay = new GenericOverlay(topManager,transports);
		routStrategy = new SubscriptionForwardingRoutingStrategy();
		reconf = new DeferredUnsubscriptionReconfigurator();
		router = new GenericRouter(overlay);
		subTable = new GenericTable();
		routStrategy.setOverlay(overlay);
		reconf.setOverlay(overlay);
		replyManager = new ImmediateForwardReplyManager();
		replyTable = new HashReplyTable();
		replyManager.setOverlay(overlay);
		router.setOverlay(overlay);
		router.setSubscriptionTable(subTable);
		router.setRoutingStrategy(routStrategy);
		router.setReplyManager(replyManager);
		router.setReplyTable(replyTable);
		reconf.setRouter(router);
		replyManager.setReplyTable(replyTable);
		myURL = "reds-tcp:" + address + ":" + brokerPort;
		
		try {
			locator = new Locator(myURL);
		} catch(IOException e) { e.printStackTrace(); }
	}
	
	@Override
	public void addNeighbor(String url) {
		try {
			overlay.addNeighbor(url);
		} catch (AlreadyAddedNeighborException e) {
			e.printStackTrace(); 
		} catch (ConnectException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Overlay getOverlay() {
		return overlay;
	}
	
	@Override
	public void locateNeighbors(int num) {
		logger.info("Searching for other brokers...");
		String[] urls = null;
		try {
			urls = locator.locate(LOCATOR_SEARCH_TIME);
		} catch (Exception e) {
			e.printStackTrace();
			logger.info("Unable to use locator");
		}
		if (urls != null) {
			int count = 0;
			for (String url : urls) {
				logger.info("Connecting to " + url);
				try {
					overlay.addNeighbor(url);
					count++;
				} catch(AlreadyAddedNeighborException e) { 
					logger.info("Broker already added"); } 
				catch (ConnectException e) {
					logger.error("Connect exception");
					e.printStackTrace();
				} catch (MalformedURLException e) {
					logger.error("URL Malformed");
					e.printStackTrace();
				}
				// Se num = 0 allora significa che verranno aggiunti tutti i neighbors trovati
				if (count == num)
					break;
			}
		}
		else logger.info("No brokers found");
	}

	@Override
	public void start() {
		overlay.start();
		locator.startServer();
	}

	@Override
	public void stop() {
		locator.stopServer();
		Set<?> neighbors = topManager.getNeighbors();
		for (Object node : neighbors) {
			//topManager.removeNeighbor((NodeDescriptor)node);
			overlay.removeNeighbor((NodeDescriptor)node);
		}
		topManager.stop();
		overlay.stop();
		transport.stop();
	}
	
	@Override
	public String getURL() {
		return transport.getURL();
		//return myURL;
	}

}
