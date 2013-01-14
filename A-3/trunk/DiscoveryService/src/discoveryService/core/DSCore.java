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
	 * Istanzia il core del discovery service prendendo come parametri il nome del nodo e un oggetto DSConfiguration
	 * che specifica la configurazione di dispatching service e broker.
	 * Questo costrutture è utilizzato quando si vuole dare al nodo un nome generato automaticamente o utlizzando particolari
	 * algoritmi (ad es. per garantirne l'univocità)
	 * 
	 * @param nodeName nome del nodo
	 * @param dsConf istanza di DSConfiguration contenente la configurazione del discovery service (dispatching service e broker)
	 * 
	 * TODO: gestire situazione quando il broker a cui si è associati cade.
	 * Guardare Overlay.addNeighborDeadListener(NeighborDeadListener)
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
	
	public DSCore(DSConfiguration dsConf) {
		this(dsConf.getNodeName(), dsConf);
	}
	
	public void start() throws ConnectException {
		ds.open();
		logger.info("Dispatching service opened: " + ds.isOpened());
		msgHandler = new CoreMsgHandler();
		msgHandler.start();
		
		// Crea lo stato e si sottoscrive a messaggi di engage per il mio stato
		status = new Status();
		status.setNodeName(nodeName);
		try {
			status.setAddress(InetAddress.getLocalHost().getHostAddress());
		} catch (UnknownHostException e) { e.printStackTrace(); }
		emFilter = new EngageMessageFilter(status);
		ds.subscribe(emFilter);
		
	}

	public void stop() {
		msgHandler.stopProcess();
		ds.close();
		if (broker != null)
			broker.stop();
	}
	
	public String getNodeName() {
		return nodeName;
	}
	
	public DispatchingService getDispatchingService() {
		return ds;
	}
	
	public Broker getBroker() {
		return broker;
	}
	
	public void setStatus(Status status) {
		this.status = status;
	}
	
	public Status getStatus() {
		return status;
	}
	
	public void notifyStatusUpdate() {
		ds.unsubscribe(emFilter);
		emFilter = new EngageMessageFilter(status);
		ds.publish(status);
		
	}
	
	public StatusFilter engage(String nodeName) {
		// search status + subscribe
		StatusFilter sf = new StatusFilter(nodeName, null, null);
		EngageMessage em = new EngageMessage(sf);
		search(em);
		Message reply = null;
		try {
			reply = this.getNextReply(em.getID());
		} catch (TimeoutException e) {
			System.err.println("[" + this.nodeName + "-CORE] Engagement with " + nodeName + " failed!");
			e.printStackTrace();
		}
		System.out.println("[" + this.nodeName + "-CORE] engaged with " + nodeName + "; " + reply);
		subscribe(sf);
		synchronized(engagedNodesTable) {
			engagedNodesTable.put(nodeName, (Status)reply);
		}
		return sf;
	}
	
	public void disEngage(StatusFilter sf) {
		ds.unsubscribe(sf);
	}
	
	public Status getEngagedNodeStatus(String nodeName) {
		synchronized(engagedNodesTable) {
			return engagedNodesTable.get(nodeName);
		}
	}
	
	public Set<String> getEngagedNodes() {
		synchronized(engagedNodesTable) {
			return engagedNodesTable.keySet();
		}
	}

	public void publish(DSMessage item, Filter template) {
		item.setNodeName(nodeName);
		ds.publish(item);
		logger.info("[" + nodeName + "-CORE]: Published " + item);
		ds.subscribe(template);
		logger.info("[" + nodeName + "-CORE]: Subscribed to interest in " + template);
	}
	
	public void search(DSMessage template) {
		template.setNodeName(nodeName);
		ds.publish(template);
	}
	
	public void subscribe(Filter template) {
		ds.subscribe(template);
		logger.info("[" + nodeName + "-CORE]: Subscribed to " + template);
	}
	
	public void reply(DSMessage replyMsg, MessageID id) {
		ds.reply(replyMsg, id);
	}
	
	public Message getNextReply(int timeout) {
		if (timeout == 0)
			return ds.getNextReply();
		else if (timeout > 0)
			return ds.getNextReply(timeout);
		else
			return null;
	}
	
	public Message getNextReply(MessageID msgID) throws NullPointerException, TimeoutException {
		return ds.getNextReply(msgID);
	}
	
	public Message[] getAllReplies(MessageID msgID) {
		Replies replies = ds.getAllReplies(msgID);
		Message[] replyMsgs = replies.getReplies();
		return replyMsgs;
	}
	
	public boolean hasMoreReplies() {
		return ds.hasMoreReplies();
	}
	
	public boolean hasMoreReplies(MessageID msgID) {
		return ds.hasMoreReplies(msgID);
	}

	public void registerMessageListener(MessageListener listener) {
		synchronized(msgListenerLock) {
			msgListener = listener;
		}
	}
	
	public void removeMessageListener() {
		synchronized(msgListenerLock) {
			msgListener = null;
		}
	}
	
	public void registerStatusListener(StatusListener listener) {
		synchronized(statusListenerLock) {
			statusListener = listener;
		}
	}
	
	public void removeStatusListener() {
		synchronized(msgListenerLock) {
			statusListener = null;
		}
	}
	
	// Questa classe si occupa di gestire i msg ricevuti;
	// Nel caso tali msg servano al core li processa; in caso contrario li inoltra al listener registrato (se presente)
	private class CoreMsgHandler extends MsgHandler {
		private volatile boolean stopped = false;
		private MessageListener mListener;
		private StatusListener sListener;
		
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
				
				synchronized(statusListenerLock) {
					sListener = statusListener;
				}
				if (sListener != null)
					sListener.notify((Status)msg);
			}
			
			else {
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
