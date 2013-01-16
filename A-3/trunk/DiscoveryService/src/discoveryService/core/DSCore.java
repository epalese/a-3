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

import java.net.ConnectException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Set;

import org.apache.log4j.Logger;

import polimi.reds.DispatchingService;
import polimi.reds.Filter;
import polimi.reds.Message;
import polimi.reds.MessageID;
import polimi.reds.Replies;
import polimi.reds.TimeoutException;
import discoveryService.broker.Broker;
import discoveryService.core.status.EngageMessage;
import discoveryService.core.status.EngageMessageFilter;
import discoveryService.core.status.Status;
import discoveryService.core.status.StatusFilter;

/**
 * The core of a Discovery Service node. It can be composed of two parts: the broker and the dispatching service.
 * <p>
 * The dispatching service is the "front end" of the node and offers the methods to publish information in the
 * discovery service and to search for it.
 * </p>
 * <p>
 * The role of the broker is to receive publish or search requests and to deliver them to the correct nodes.
 * Discovery Service can be deployed on an overlay network having nodes ranging from one to a totally decentralised
 * topology where every node runs a broker instance.
 * </p>
 * <p>
 * The various parameters of the service must can be configured using an instance of {@link DSConfiguration}.
 * </p>
 * <p>
 * With the node can be associated a {@link Status} that represents some particular information about the state
 * of the node. Discovery Service node notifies status changes when triggered through the method <code>
 * DSCore.notifyStatusUpdate</code>. A node can notify to the service that it is interested in status changes
 * of a specific node through the method <code>DSCore.engage()</code>.
 * 
 * @author leleplx@gmail.com (emanuele)
 *
 */
public class DSCore {
	private static Logger logger = Logger.getLogger(DSCore.class);
	private String nodeName;
	private DispatchingService ds;
	private Broker broker;
	private MessageListener msgListener;
	private Object msgListenerLock;
	private StatusListener statusListener;
	private Object statusListenerLock;
	private MsgHandler msgHandler;
	private Status status;
	private EngageMessageFilter emFilter;
	private Hashtable<String, Status> engagedNodesTable;
	
	/**
	 * Create an instance of DSCore with the given node name and parameters passed in an instance of
	 * <code>DSConfiguration</code>.
	 * The node name is explicitly passed allow node name generated at running time.
	 * 
	 * @param nodeName A string identifier representing the name of the node.
	 * @param dsConf <code>DSConfiguration</code> instance encapsulating service (for both dispatching service and
	 * broker if required).
	 * 
	 * TODO: handle situations when a neighbor dies. Ref. Overlay.addNeighborDeadListener(NeighborDeadListener)
	 */
	public DSCore(String nodeName, DSConfiguration dsConf) {
		if ((this.nodeName = nodeName) == null)
			throw new IllegalConfigurationException("No node name defined");
		if (dsConf.getCreateBroker()) {
			logger.info("Creating broker");
			broker = DSFactory.createBroker(dsConf);
		}
		ds = DSFactory.createDispatchingService(dsConf);
		msgListener = null;
		msgListenerLock = new Object();
		statusListener = null;
		statusListenerLock = new Object();
		engagedNodesTable = new Hashtable<String, Status>();
	}
	
	/**
	 * Create an instance of DSCore with the parameters specified in an instance of <code>DSConfiguration</code>.
	 */
	public DSCore(DSConfiguration dsConf) {
		this(dsConf.getNodeName(), dsConf);
	}
	
	/**
	 * Start the activity of the node. After calling the method the node can publish or search for information
	 * in the discovery service.
	 * 
	 * @throws ConnectException
	 */
	public void start() throws ConnectException {
		ds.open();
		logger.info("Dispatching service opened: " + ds.isOpened());
		msgHandler = new CoreMsgHandler();
		msgHandler.start();
		
		// Create an instance of Status and then make a subscription to messages interested
		// in the status of the node.
		status = new Status();
		status.setNodeName(nodeName);
		try {
			status.setAddress(InetAddress.getLocalHost().getHostAddress());
		} catch (UnknownHostException e) { e.printStackTrace(); }
		emFilter = new EngageMessageFilter(status);
		ds.subscribe(emFilter);
		
	}

	/**
	 * Stop the activity of the node.
	 */
	public void stop() {
		msgHandler.stopProcess();
		ds.close();
		if (broker != null)
			broker.stop();
	}
	
	/**
	 * Return the node name specified when the instance of DSCore has been created.
	 * @return
	 */
	public String getNodeName() {
		return nodeName;
	}
	
	/**
	 * Get the instance of <code>polimi.reds.DispatchingService</code> associated with the <code>DSCore
	 * </code> instance. 
	 * 
	 * @return
	 */
	public DispatchingService getDispatchingService() {
		return ds;
	}
	
	/**
	 * Get the instance of {@link Broker} associated with the <code>DSCore
	 * </code> instance. 
	 * 
	 * 	@return
	 */
	public Broker getBroker() {
		return broker;
	}
	
	/**
	 * Set the instance attribute status to the one passed as parameter.
	 * 
	 * @param status
	 */
	public void setStatus(Status status) {
		this.status = status;
	}
	
	/**
	 * Return the current status.
	 * 
	 * @return
	 */
	public Status getStatus() {
		return status;
	}
	
	/**
	 * Notify to nodes a change in the status.
	 */
	public void notifyStatusUpdate() {
		ds.unsubscribe(emFilter);
		emFilter = new EngageMessageFilter(status);
		ds.publish(status);
		
	}
	
	/**
	 * Perform the engagement with the node specified by the node name passed as parameter.
	 * The engagement is a subscription to changes in status of the respective nodes.
	 * 
	 * @param nodeName The node to engage with.
	 * @return A {@link StatusFilter} instance used to match the notification of status changes sent by the node
	 * with whom the engagement has been performed.
	 */
	public StatusFilter engage(String nodeName) {
		// search status + subscribe
		StatusFilter sf = new StatusFilter(nodeName, null, null);
		EngageMessage em = new EngageMessage(sf);
		search(em);
		Message reply = null;
		try {
			reply = this.getNextReply(em.getID());
		} catch (TimeoutException e) {
			logger.info("[" + this.nodeName + "-CORE] Engagement with " + nodeName + " failed!");
			e.printStackTrace();
		}
		logger.info("[" + this.nodeName + "-CORE] engaged with " + nodeName + "; " + reply);
		subscribe(sf);
		synchronized(engagedNodesTable) {
			engagedNodesTable.put(nodeName, (Status)reply);
		}
		return sf;
	}
	
	/**
	 * Terminates the engage relationship.
	 *  
	 * @param sf The StatusFilter instance return at the method <code>DSCore.engage()</code> in invocated.
	 */
	public void disEngage(StatusFilter sf) {
		ds.unsubscribe(sf);
	}
	
	/**
	 * Get the last status received of the node identified by nodeName.
	 *  
	 * @param nodeName The name of the node whose status is required.
	 * 
	 * @return The status of the specified node.
	 */
	public Status getEngagedNodeStatus(String nodeName) {
		synchronized(engagedNodesTable) {
			return engagedNodesTable.get(nodeName);
		}
	}
	
	/**
	 * Return a list of the engaged nodes.
	 * 
	 * @return
	 */
	public Set<String> getEngagedNodes() {
		synchronized(engagedNodesTable) {
			return engagedNodesTable.keySet();
		}
	}
	
	/**
	 * Publish information on the discovery service and makes it available for searching.
	 * The information is in the form of {@link DSMessage}. An instance of {@link Filter} is
	 * required since it is used to match query by other nodes.
	 * 
	 * @param item An instance of DSMessage describing the information to be published.
	 * @param template An instance of Filter built to allow the matching of search messages 
	 * addressed to the information published by the node.
	 */
	public void publish(DSMessage item, Filter template) {
		item.setNodeName(nodeName);
		ds.publish(item);
		logger.info("[" + nodeName + "-CORE]: Published " + item);
		ds.subscribe(template);
		logger.info("[" + nodeName + "-CORE]: Subscribed to interest in " + template);
	}
	
	/**
	 * Send a search message with the template describing the information to be searched for.
	 * 
	 * @param template An instance of DSMessage containing structured in a way that expresses
	 * the information to be searched for.
	 */
	public void search(DSMessage template) {
		template.setNodeName(nodeName);
		ds.publish(template);
	}
	
	/**
	 * Make a subscription to the kind of messages described through the filter passed as
	 * parameter.
	 * 
	 * @param template Defines the matching criteria of the subscription.
	 */
	public void subscribe(Filter template) {
		ds.subscribe(template);
		logger.info("[" + nodeName + "-CORE]: Subscribed to " + template);
	}
	/**
	 * Send replyMsg to reply to a message previously received.
	 *  
	 * @param replyMsg The reply message to be sent.
	 * @param id The id of the message that generated the reply.
	 */
	public void reply(DSMessage replyMsg, MessageID id) {
		ds.reply(replyMsg, id);
	}
	
	/**
	 * Waits for a reply message for <code>timeout</code> milliseconds.
	 * 
	 * @param timeout Time in milliseconds that the node will wait before returning 
	 * control to the caller. 
	 * 
	 * @return An instance of {@link Message} containing a reply.
	 */
	public Message getNextReply(int timeout) {
		if (timeout == 0)
			return ds.getNextReply();
		else if (timeout > 0)
			return ds.getNextReply(timeout);
		else
			return null;
	}
	
	/**
	 * Wait for a reply to message with id <code>msgID</code>
	 * 
	 * @param msgID The id of the message for which the node waits for a reply.
	 * 
	 * @return A Message instance containing the reply.
	 * 
	 * @throws NullPointerException
	 * @throws TimeoutException
	 */
	public Message getNextReply(MessageID msgID) throws NullPointerException, TimeoutException {
		return ds.getNextReply(msgID);
	}
	
	/**
	 * Get all replies for the message with the specified message ID.
	 * 
	 * @param msgID The ID of the message
	 * 
	 * @return An array of {@link Message} containing all the replies.
	 */
	public Message[] getAllReplies(MessageID msgID) {
		Replies replies = ds.getAllReplies(msgID);
		Message[] replyMsgs = replies.getReplies();
		return replyMsgs;
	}
	
	/**
	 * Check if the service has more replies.
	 * 
	 * @return True if there are more replies to be fetched.
	 */
	public boolean hasMoreReplies() {
		return ds.hasMoreReplies();
	}
	
	/**
	 * Check if the service has more replies for a particular message.
	 * 
	 * @return True if there are more replies to be fetched for the specified message.
	 */
	public boolean hasMoreReplies(MessageID msgID) {
		return ds.hasMoreReplies(msgID);
	}

	/**
	 * Register an instance of @{link MessageListener} that will be notified when
	 * a message arrives at the node.
	 * 
	 * @param listener The instance of MessageListener to be registered.
	 */
	public void registerMessageListener(MessageListener listener) {
		synchronized(msgListenerLock) {
			msgListener = listener;
		}
	}
	
	/**
	 * Remove the message listener.
	 * 
	 */
	public void removeMessageListener() {
		synchronized(msgListenerLock) {
			msgListener = null;
		}
	}
	
	/**
	 * Register an instance of {@link StatusListener} that will be notified when
	 * an engaged node changes its status.
	 * 
	 * @param listener The instance of StatusListener to be registered.
	 */
	public void registerStatusListener(StatusListener listener) {
		synchronized(statusListenerLock) {
			statusListener = listener;
		}
	}
	
	/**
	 * Remove the listener.
	 */
	public void removeStatusListener() {
		synchronized(msgListenerLock) {
			statusListener = null;
		}
	}
	
	// Questa classe si occupa di gestire i msg ricevuti;
	// Nel caso tali msg servano al core li processa; in caso contrario li inoltra al listener registrato (se presente)
	/**
	 * Implements the {@link MsgHandler} providing the required methods.
	 * The CoreMsgHandler is in charge of managing messages used by Discovery Service
	 * to implement the functionalities provided (that is publishing ans searching for
	 * information).
	 * 
	 * @author leleplx@gmail.com (emanuele)
	 *
	 */
	private class CoreMsgHandler extends MsgHandler {
		private volatile boolean stopped = false;
		private MessageListener mListener;
		private StatusListener sListener;
		
		/**
		 * Every message received by the node passes through this method.
		 * If the message in an instance of <code>EngageMessage</code> or <code>Status</code>
		 * the method processes it. Otherwise the messages is delivered to the application
		 * listeners (if any).
		 * 
		 */
		public void process(DSMessage msg) {
			if (msg instanceof EngageMessage) {
				logger.info("[" + nodeName + "-CORE]: received from " + msg.getNodeName() + " EngageMessage --> " + ((EngageMessage)msg).toString());
				ds.reply(status, msg.getID());
			}
			
			else if (msg instanceof Status) {
				logger.info("[" + nodeName + "-CORE]: received from " + msg.getNodeName() + " Status --> " + ((Status)msg).toString());
				synchronized(engagedNodesTable) {
					engagedNodesTable.put(msg.getNodeName(), (Status)msg);
				}
				
				// Save a copy of DSCore.statusListener in order to let
				// the node to avoid to lock the listeners registered.
				synchronized(statusListenerLock) {
					sListener = statusListener;
				}
				if (sListener != null)
					sListener.notify((Status)msg);
			}
			
			else {
				// Save a copy of DSCore.statusListener in order to let
				// the node to avoid to lock the listeners registered.
				synchronized(msgListenerLock) {
					mListener = msgListener;
				}
				if (mListener != null)
					mListener.notify(msg);
			}
		}
		
		public void run() {
			while(!stopped) {
				Message msg = ds.getNextMessage(1000);
				if ((msg != null) && (msg instanceof DSMessage)) {
					process(((DSMessage)msg));
				}
			}
		}
		
		public void stopProcess() {
			stopped = true;
		}
	}
}
