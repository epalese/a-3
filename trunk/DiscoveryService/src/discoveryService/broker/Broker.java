package discoveryService.broker;

import polimi.reds.broker.overlay.Overlay;

/**
 * This interface define the methods that a broker in the Discovery Service must implement.
 * 
 * Discovery Service in terms of publish/subscribe paradigms has two different network components:
 * - broker: is in charge of storing subscriptions 
 * - dispatcher: is the "front-end" interface of the service and allow the components to  publish messages or to express interest in certain type of messages through a subscription
 * - broker: a dispatcher create a connection to the broker and rely on it to publish messages or to register subscription.
 * 	 The brokers are connected together in an overlay network (often using a tree structure) and are in charge of routing messages of publisher to the interested subscribers.
 * 
 * @author leleplx@gmail.com (emanuele)
 *
 */
public interface Broker {
	/**
	 * Start the broker. This method must be called before any activity of the broker.
	 */
	public void start();
	
	/**
	 * Stop the broker activities and close all network connections.
	 */
	public void stop();
	
	/**
	 * Connect the broker to another known broker addressed by url.
	 * If the connection is successful the two broker will be part of the same overlay network and will start to exchange information on overlay network topology taking part in the routing and delivery activities. 
	 * 
	 * @param url The URL of the broker to connect with (e.g. red-tcp:127.0.0.2:8100)
	 */
	public void addNeighbor(String url);
	
	/**
	 * Perform a discovery in the network to find all the brokers available and connect to the first num brokers.
	 * 
	 * @param num The max number of brokers to connect to. If num is equal to 0 thenthe broker will be connected to all the neighbors found.
	 */
	public void locateNeighbors(int num);
	
	/**
	 * Get access to the underlying overlay network.
	 * See REDS documentation.
	 * 
	 * @return An instance of polimi.reds.broker.overlay.Overlay to manage underlying broker overlay network.
	 */
	public Overlay getOverlay();
	
	/**
	 * Get the URL of the broker.
	 * 
	 * @return URL of the current broker.
	 */
	public String getURL();
}
