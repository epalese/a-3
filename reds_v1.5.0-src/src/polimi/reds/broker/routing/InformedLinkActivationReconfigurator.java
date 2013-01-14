/***
 * * REDS - REconfigurable Dispatching System
 * * Copyright (C) 2007 Politecnico di Milano
 * * <mailto: cugola@elet.polimi.it> <mailto: picco@elet.polimi.it>
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
 ***/

package polimi.reds.broker.routing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;
import java.util.logging.Logger;

import polimi.reds.Filter;
import polimi.reds.NodeDescriptor;
import polimi.reds.broker.overlay.LinkSubstitutionInfo;
import polimi.reds.broker.overlay.NotConnectedException;
import polimi.reds.broker.overlay.NotRunningException;
import polimi.reds.broker.overlay.Overlay;
import polimi.reds.broker.overlay.PacketListener;
import polimi.reds.broker.overlay.REDSMarshaller;
import polimi.reds.broker.overlay.REDSUnmarshaller;
import polimi.reds.broker.overlay.ReconfigurationId;

/**
 * This class handles reconfigurations using the Informed Link Activation protocol.
 * <p>
 * The protocol is described in "Content-Based Routing for Publish-Subscribe on a Dynamic Topology: 
 * Concepts, Protocols and Evaluation", by G. Cugola, D. Frey, A.L. Murphy and G.P. Picco.
 * 
 * @author Andrea Milani
 */
public class InformedLinkActivationReconfigurator implements Reconfigurator, PacketListener {
  
  // =============================================================================================
  //
  // IMPORTANT - SYNCHRONIZATION NOTE
  //
  // All the methods that handle reconfigurations must be synchronized so that they do not execute 
  // concurrently. Concurrent execution might cause wrong subscription propagation.
  // 
  // =============================================================================================
  
  /** Default timeout value (in milliseconds) for the deferral of unsubscriptions. */
  public static final long DEFAULT_UNSUBSCRIPTION_DEFER_TIMEOUT = 3000;
  /** Default timeout value (in milliseconds) for the deferral of subscriptions. */
  public static final long DEFAULT_SUBSCRIPTION_DEFER_TIMEOUT = 5000;
  
  /** The subject used for FLUSH messages. */
  private static final String FLUSH_SBJ = "__INFLINKACT_FLUSH";
  
  // The maximum size (in bytes) for a UDP packet sent by this class.
  private static final int MAX_UDP_SIZE = 4096;
  
  private Logger logger = Logger.getLogger(
      "polimi.reds.broker.routing.InformedLinkActivationReconfigurator");
  
  private Router router = null;
  private Overlay overlay = null;
  
  // Timeout values for the deferral of subscriptions and unsubscriptions.
  private long subTimeout;
  private long unsubTimeout;
  
  // The timer used to schedule future actions.
  // The timer's thread is set to run as a daemon, so that it does not prevent the VM from 
  // terminating when other threads have finished. Note that in this way the program may terminate
  // without having completed some deferred tasks. In the case of deferred subscriptions and 
  // unsubscriptions, this is not a problem, because neighbors would notice that the local node has
  // gone down, and consequently fix their subscription tables. In the case of ACTIVATE_ACK checks,
  // the termination of the program will disable ACTIVATE retransmissions.
  private Timer scheduler = new Timer(true);
  
  // The map containing the tasks currently scheduled to execute the deferred unsubscriptions.
  private HashMap<ReconfigurationId, UnsubTask> deferredUnsubs = 
          new HashMap<ReconfigurationId, UnsubTask>();
  
  // The UDP socket used for sending and receiving out-of-band messages (currently ACTIVATE and 
  // ACTIVATE_ACK). The socket receives data on the same port the overlay uses to accept incoming 
  // neighbor TCP connections.
  private DatagramSocket udpSocket;
  
  // Contains all the classes that are currently sending an ACTIVATE message.
  private HashMap<ReconfigurationId, ActivateSender> activateSenders = 
      new HashMap<ReconfigurationId, ActivateSender>();
  
  /**
   * Creates a new <code>InformedLinkActivationReconfigurator</code> using the default timeouts
   * for the deferrral of subscriptions and unsubscriptions.
   */
  public InformedLinkActivationReconfigurator() {
    this(DEFAULT_SUBSCRIPTION_DEFER_TIMEOUT, DEFAULT_UNSUBSCRIPTION_DEFER_TIMEOUT);
  }
  
  /**
   * Creates a new <code>InformedLinkActivationReconfigurator</code> using the specified deferral
   * timeouts.
   * 
   * @param subTimeout timeout (in milliseconds) for the deferral of subscriptions. 
   * @param unsubTimeout timeout (in milliseconds) for the deferral of unsubscriptions.
   */
  public InformedLinkActivationReconfigurator(long subTimeout, long unsubTimeout) {
    this.subTimeout = subTimeout;
    this.unsubTimeout = unsubTimeout;
  }
  
  /**
   * Sets the timeout used for the deferral of subscriptions. The new setting will take effect
   * starting from the next reconfiguration.
   * 
   * @param timeout the subscription timeout, in milliseconds.
   */
  public synchronized void setSubscriptionTimeout(long timeout) {
    this.subTimeout = timeout;
  }
  
  /**
   * Sets the timeout used for the deferral of unsubscriptions. The new setting will take effect
   * starting from the next reconfiguration.
   * 
   * @param timeout the unsubscription timeout, in milliseconds.
   */
  public synchronized void setUnsubscriptionTimeout(long timeout) {
    this.unsubTimeout = timeout;
  }
  
  public synchronized void setRouter(Router r) {
    router = r;
    overlay = r.getOverlay();
    overlay.addNeighborhoodChangeListener(this);
    overlay.addPacketListener(this, FLUSH_SBJ);
    overlay.setTrafficClass(FLUSH_SBJ, Router.FILTER_CLASS);
    // Extract the UDP port for receiving out-of-band messages from the local node descriptor, then
    // open the UDP socket.
    try {
      NodeDescriptor self = overlay.getNodeDescriptor();
      String[] urlParts = self.getUrls()[0].split(":");
      if (urlParts.length != 3 || !urlParts[0].equals("reds-tcp")) {
        throw new MalformedURLException("Unsupported URL while determining the UDP port to use.");
      }
      int port = Integer.parseInt(urlParts[2]);
      udpSocket = new DatagramSocket(port);
      // Start the UDP receiving thread.
      Thread udpThread = new Thread(new OutOfBandReceiver());
      udpThread.setDaemon(true);
      udpThread.start();
    } catch (IOException ioe) {
      // The Reconfigurator interface does not allow us to throw a checked exception from setRouter,
      // but we cannot stay silent about an UDP socket creation error (e.g. BindException: address
      // already in use), because without the UDP socket the reconfigurator does not work as 
      // intended. So throw an unchecked exception.
      throw new RuntimeException(ioe);
    }
  }
  
  public synchronized void notifyNeighborAdded(NodeDescriptor addedNeighbor,
                                               Serializable reconfInfo) {
    
    // We must not propagate the local subscriptions to clients.
    if (addedNeighbor.isClient()) return;
    
    // Synchronize on the router to make sure that no one unsubscribes from a filter after we have
    // retrieved it from the subscription table but before we have sent the subscription. If that
    // could happen, we could send a subscription for a no longer needed filter, causing useless
    // traffic in the future.
    synchronized (router) {
      SubscriptionTable subTbl = router.getSubscriptionTable();
      Collection<Filter> filters = subTbl.getAllFiltersExcept(false, addedNeighbor);
      for (Filter filter : filters) {
        sendMessage(Router.SUBSCRIBE, filter, addedNeighbor, "notifyNeighborAdded");
      }
    }
    
  }
  
  public synchronized void notifyNeighborRemoved(NodeDescriptor removedNeighbor) {
    router.unsubscribeAll(removedNeighbor);
  }
    
  public synchronized void notifyNeighborDead(NodeDescriptor deadNeighbor, 
                                              Serializable reconfInfo) {
    
    if (deadNeighbor.isClient() || reconfInfo == null) {
      // If the dead neighbor was a client there are no optimizations to use.
      // reconfInfo == null means that the topology manager has not been able to replace the link to
      // deadNeighbor's subtree, so just remove deadNeighbor's subscriptions, without optimizations.
      router.unsubscribeAll(deadNeighbor);
      return;
    }
    
    // Note that the reconfiguration must be executed even if the new link has been created between
    // the same nodes as the old link. In fact the subscriptions of the two nodes may have changed 
    // while the two nodes were disconnected and they could not exchange (un)subscription messages.
    
    // Note that since the topology manager has been able to replace the link to the sub-tree of the
    // dead neighbor, this node is not a leaf broker (else it could not be connected to the 
    // other sub-tree).
    
    Collection<Filter> deadFilters, deadOnlyFilters;
    synchronized (router) {
      SubscriptionTable subTbl = router.getSubscriptionTable();
      deadFilters = subTbl.getAllFilters(deadNeighbor);
      // Create a deadOnlyFilters list with the filters only deadNeighbor was subscribed to.
      deadOnlyFilters = new ArrayList<Filter>();
      for (Filter f : deadFilters) {
        NodeDescriptor singleBroker = subTbl.getSingleSubscribedBroker(f);
        if (singleBroker != null && singleBroker.equals(deadNeighbor)) {
          deadOnlyFilters.add(f);
        }
      }
      // Execute the unsubscriptions locally, but defer their propagation to the neighbors.
      subTbl.removeAllSubscriptions(deadNeighbor);
    }
    // Create the deferred task even if deadFilters is empty, because we use it to determine 
    // if a received FLUSH is addressed to the local node.
    LinkSubstitutionInfo substitution = (LinkSubstitutionInfo) reconfInfo;
    ReconfigurationId reconfId = null;
    if (substitution != null) reconfId = substitution.getReconfId();
    UnsubTask deferredTask = new UnsubTask(reconfId, deadNeighbor, deadFilters);
    scheduler.schedule(deferredTask, unsubTimeout);
    deferredUnsubs.put(reconfId, deferredTask);
    
    // Send the necessary information to the endpoint of the new link in our sub-tree.
    NodeDescriptor actDest = substitution.getNewLinkBrokerInSameSubTree();
    NodeDescriptor actDestNeighbor = substitution.getNewLinkBrokerInOtherSubTree();
    ActivateMsg act = new ActivateMsg(reconfId, deadOnlyFilters, actDestNeighbor);
    if (actDest.equals(overlay.getNodeDescriptor())) {
      performActivate(act);
    } else {
      ActivateSender sender = null;
      try {
        sender = new ActivateSender(act, actDest);
      } catch (Exception ex) {
        logger.warning(ex + " while creating ACTIVATE sender.");
      }
      if (sender != null) {
        synchronized (activateSenders) {
          activateSenders.put(reconfId, sender);
        }
        sender.send();
      }
    }
    
  }
  
  public synchronized void notifyPacketArrived(String subject, NodeDescriptor source,
                                               Serializable packet) {
    
    if (subject.equals(FLUSH_SBJ)) {
      
      ReconfigurationId reconfId = (ReconfigurationId) packet;
      UnsubTask unsubTask = deferredUnsubs.get(reconfId);
      if (unsubTask != null) {
        // This broker is the addressee of the flush message: complete the reconfiguration 
        // identified by reconfId.
        unsubTask.cancel();
        unsubTask.run();
      } else {
        // The ID in the flush message is unknown. Send the packet to the neighbors, so that it can 
        // reach its addressee.
        // Note that it might happen that the flush message for a certain reconfiguration arrives 
        // after the timeout for the deferred unsubscriptions has expired. In this case, the above 
        // check on deferredUnsubs will not find the ID and the broker will enter this code section
        // even if it was the intended addressee. Anyway this should happen rarely, because the 
        // timeout should be tuned so that the flush message can be received on time in most cases 
        // (otherwise the flush message would be useless). Furthermore, this problem is harmless for
        // the correctness of the protocol, it just increases the overhead a little.
        for (NodeDescriptor node : overlay.getAllNeighborsExcept(source)) {
          if (node.isBroker()) {
            sendMessage(FLUSH_SBJ, reconfId, node, "notifyPacketArrived");
          }
        }
      }
      
    } else {
      logger.severe("Unexpected packet subject: '" + subject + "'.");
    }
    
  }
  
  private void performActivate(ActivateMsg activate) {
    
    NodeDescriptor newNeighbor = activate.getNewNeighbor();
    // The protocol requires us to schedule the deferred task before performing the other 
    // operations. Note that since the task's run method is synchronized on the reconfigurator,
    // it is guaranteed not to execute until the caller of this method (notifyNeighborAdded or
    // notifyPacketArrived, which are synchronized) returns, so we have the possibility to call 
    // setParameters on the task.
    SubTask deferredTask = new SubTask();
    scheduler.schedule(deferredTask, subTimeout);
    // Determine which filters can be propagated immediately and which must be deferred.
    // See notifyNeighborAdded for the reason why synchronizing on the router is required.
    LinkedList<Filter> filtersToDefer = new LinkedList<Filter>();
    synchronized (router) {
      Collection<Filter> allFilters = router.getSubscriptionTable().getAllFilters(false);
      Collection<Filter> unconfirmedFilters = activate.getUnconfirmedFilters();
      for (Filter f : allFilters) {
        if (unconfirmedFilters.contains(f)) {
          filtersToDefer.add(f);
        } else {
          sendMessage(Router.SUBSCRIBE, f, newNeighbor, "performActivate");
        }
      }
    }
    deferredTask.setParameters(newNeighbor, filtersToDefer);
    sendMessage(FLUSH_SBJ, activate.getReconfId(), newNeighbor, "performActivate");
    
  }
  
  // Utility method to send a message without writing exception handling code every time.
  private void sendMessage(String subject, Serializable payload, NodeDescriptor destination, 
      String sourceMethod) {
    try {
      overlay.send(subject, payload, destination);
    } catch (NotConnectedException nce) {
      // Logged as FINE because when many reconfigurations are running in parallel the SubTask will
      // often try to send subscription to neighbors that get disconnected in the meanwhile, so this
      // exception is normal.
      logger.fine(sourceMethod + ": " + nce + " while sending " + subject + " to " + destination);
    } catch (IOException ioe) {
      logger.warning(sourceMethod + ": " + ioe + " while sending " + subject + " to " +
                     destination);
    } catch (NotRunningException nre) {
      logger.severe(sourceMethod + ": overlay not running while sending " +  subject + " to " + 
                    destination);
      nre.printStackTrace();
    }
  }
  
  // Sends an out-of-band message.
  private void sendOobMsg(Serializable msg, InetAddress destAddr, int destPort) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    REDSMarshaller marshaller = new REDSMarshaller(baos);
    marshaller.writeObject(msg);
    marshaller.close();
    byte[] data = baos.toByteArray();
    if (data.length > MAX_UDP_SIZE) {
      throw new IOException("Cannot send a message " + data.length + " bytes long. " + 
                            "OutOfBandReceiver can handle at most " + MAX_UDP_SIZE + " bytes.");
    }
    DatagramPacket packet = new DatagramPacket(data, data.length, destAddr, destPort);
    udpSocket.send(packet);
  }
  
  // Class used to send an ACTIVATE message.
  private static class ActivateMsg implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private ReconfigurationId reconfId;
    private Collection<Filter> unconfirmedFilters;
    private NodeDescriptor newNeighbor;
    
    /**
     * Creates a new ACTIVATE message.
     * 
     * @param reconfId the ID of the reconfiguration this message is part of.
     * @param unconfirmedFilters the filters that the recipient of this message must wait to 
     *        propagate to the new neighbor.
     * @param newNeighbor the new neighbor of the recipient on the new link.
     */
    public ActivateMsg(ReconfigurationId reconfId, Collection<Filter> unconfirmedFilters, 
        NodeDescriptor newNeighbor) {
      this.reconfId = reconfId;
      this.unconfirmedFilters = unconfirmedFilters;
      this.newNeighbor = newNeighbor;
    }
      
    public ReconfigurationId getReconfId() {
      return reconfId;
    }
    
    public Collection<Filter> getUnconfirmedFilters() {
      return unconfirmedFilters;
    }
    
    public NodeDescriptor getNewNeighbor() {
      return newNeighbor;
    }
    
  } // ActivateMsg
  
  // Class used to send an ACTIVATE_ACK message.
  private static class ActivateAck implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private ReconfigurationId reconfId;
    
    public ActivateAck(ReconfigurationId id) {
      this.reconfId = id;
    }
    
    public ReconfigurationId getReconfId() {
      return reconfId;
    }
    
  } // ActivateAck
  
  // Class used to schedule the deferred propagation of unsubscriptions.
  private class UnsubTask extends TimerTask {
    
    private ReconfigurationId reconfId;
    // The neighbor the filters to unsubscribe belong to.
    private NodeDescriptor neighbor;
    // The list of filters to unsubscribe when the task is executed.
    private Collection<Filter> filters;
    // Flag used to avoid executing the task twice in case notifyPacketArrived and the scheduler 
    // launch it concurrently.
    private boolean executed = false;
    
    public UnsubTask(ReconfigurationId reconfId, NodeDescriptor neighbor, 
                     Collection<Filter> filters) {
      this.reconfId = reconfId;
      this.neighbor = neighbor;
      this.filters = filters;
    }
    
    @Override
    public void run() {
      synchronized (InformedLinkActivationReconfigurator.this) {
        
        if (executed) return;
        
        if (filters.size() > 0) {
          // Send the deferred unsubscriptions, unless the old neighbor has reconnected and
          // re-subscribed in the meanwhile.
          // It is necessary to synchronize on the router to make sure that no one subscribes to 
          // a filter just before we remove it, resulting in the filter to be erroneously removed.
          synchronized (router) {
            SubscriptionTable subTbl = router.getSubscriptionTable();
            for (Filter filter : filters) {
              if (!subTbl.isSubscribed(neighbor, filter)) {
                // The filter must be temporarily reinserted into the table because the unsubscribe
                // operation has no effect if the filter is not in the table.
                subTbl.addSubscription(neighbor, filter);
                router.unsubscribe(neighbor, filter);
              }
            }
          }
        }
        
        deferredUnsubs.remove(reconfId);
        executed = true;
        
      }
    }
    
  } // UnsubTask  
  
  // Class used to schedule the deferred propagation of subscriptions.
  private class SubTask extends TimerTask {

    // The neighbor to send the subscriptions to.
    private NodeDescriptor neighbor;
    // The list of filters to subscribe to when the task is executed.
    private Collection<Filter> filters;
    
    // This method must be invoked before run() is executed. The parameters are not set in the
    // constructor because the object is created before the parameters are known (see 
    // notifyPacketArrived).
    public synchronized void setParameters(NodeDescriptor neighbor, Collection<Filter> filters) {
      this.neighbor = neighbor;
      this.filters = filters;
    }
    
    @Override
    public void run() {
      synchronized (InformedLinkActivationReconfigurator.this) {
        SubscriptionTable subTbl = router.getSubscriptionTable();
        // See notifyNeighborAdded for the reason why synchronizing on the router is required.
        if (filters.size() > 0) {
          synchronized (router) {
            for (Filter filter : filters) {
              // Send the subscription to the new neighbor only if at least another neighbor is 
              // interested in the filter.
              Collection<NodeDescriptor> subscribers = subTbl.getSubscribedNeighbors(filter);
              if (subscribers.size() > 1 
                  || (subscribers.size() == 1 && !subscribers.contains(neighbor))) {
                sendMessage(Router.SUBSCRIBE, filter, neighbor, "SubTask.run");
              }
            }
          }
        }
      }
    }
    
  } // SubTask
  
  // The thread that receives out-of-band messages.
  private class OutOfBandReceiver implements Runnable {
    
    // If a node sends us an ACTIVATE but our ACK is lost or does not arrive on time, the other node
    // resends us the ACTIVATE. Reprocessing the ACTIVATE does not cause correctness problems,
    // but it generates unnecessary overhead. So we maintain a list with the most recent 
    // reconfiguration IDs we have processed, and do not process an ACTIVATE again if its 
    // reconfiguration ID is in the list.
    // LAST_RIDS_SIZE is currently set to a very high value to work well in PlanetLab tests with an
    // extremely high reconfiguration frequency; it can be lowered to spare memory if less frequent
    // reconfigurations are expected.
    private static final int LAST_RIDS_SIZE = 100;
    private RecentRecIdHolder lastRIds;
    
    public void run() {
      lastRIds = new RecentRecIdHolder(LAST_RIDS_SIZE);
      DatagramPacket buffer = new DatagramPacket(new byte[MAX_UDP_SIZE], MAX_UDP_SIZE);
      while (true) {
        try {
          udpSocket.receive(buffer);
        } catch (Exception ex) {
          logger.warning(ex + " while receiving UDP data.");
          ex.printStackTrace();
          udpSocket.close();
          break;
        }
        try {
          ByteArrayInputStream bais = new ByteArrayInputStream(buffer.getData());
          REDSUnmarshaller unmarshaller = new REDSUnmarshaller(bais);
          Object msg = unmarshaller.readObject();
          unmarshaller.close();
          if (msg instanceof ActivateMsg) {
            ActivateMsg activate = (ActivateMsg) msg;
            ReconfigurationId rId = activate.getReconfId();
            // Always send the ACK, but process the ACTIVATE only if we do not have already 
            // processed it. 
            sendOobMsg(new ActivateAck(rId), buffer.getAddress(), buffer.getPort());
            if (!lastRIds.isIn(rId)) {
              synchronized (InformedLinkActivationReconfigurator.this) {
                performActivate(activate);
              }
              lastRIds.add(rId);
            }
          } else if (msg instanceof ActivateAck) {
            ActivateAck ack = (ActivateAck) msg;
            ActivateSender sender;
            synchronized (activateSenders) {
              sender = activateSenders.get(ack.getReconfId());
            }
            // sender may be null if we did not receive the first ACK in time, so we sent another
            // ACTIVATE, then the first ACK was received (so the sender removed itself from the
            // activateSenders map) and the receiver also sent another ACK in reply to the second 
            // ACTIVATE.
            if (sender != null) sender.setAckReceived();
          } else {
            logger.warning("Unsupported message received: " + msg);
          }
        } catch (Exception ex) {
          logger.warning(ex + "while processing received UDP data.");
          // Stay in the loop and try to receive new data.
        }
      }
    }
    
  } // OutOfBandReceiver
  
  // Class used by OutOfBandReceiver to keep track of recently processed reconfiguration IDs.
  private static class RecentRecIdHolder {
    
    private LruCache<ReconfigurationId, Object> cache;
    // The LruCache updates the access time of an element only when it is retrieved by a get call,
    // so we must use get to verify if an element is in the cache. This means we cannot use null
    // values, so this dummy object is used as the value.
    private Object dummy = new Object();
    
    public RecentRecIdHolder(int cacheSize) {
      cache = new LruCache<ReconfigurationId, Object>(cacheSize);
    }
    
    public void add(ReconfigurationId rId) {
      cache.put(rId, dummy);
    }
    
    public boolean isIn(ReconfigurationId rId) {
      return cache.get(rId) != null;
    }
    
    // A map that behaves like an LRU cache.
    private static class LruCache<K,V> extends LinkedHashMap<K,V> {
      
      private static final long serialVersionUID = 1L;
      private int cacheSize;
      
      public LruCache(int cacheSize) {
        super(cacheSize + 1, (float) 0.75, true);
        this.cacheSize = cacheSize;
      }
      
      @Override
      protected boolean removeEldestEntry(Entry<K, V> eldest) {
        return size() > cacheSize;
      }
      
    } // LruCache
    
  } // RecentRecIdHolder
  
  // Class used to manage the sending of an ACTIVATE message. The class sends the message and waits
  // for the acknowledgement from the receiving node. If an acknowledgement is not received, up to
  // MAX_ATTEMPTS - 1 retransmissions are performed. If an ack is not received after the last 
  // transmission attempt, the class gives up and logs an error.
  private class ActivateSender extends TimerTask {
    
    private static final int ACK_TIMEOUT = 3000;
    private static final int MAX_ATTEMPTS = 5;
    
    private ActivateMsg activate;
    private InetAddress destHost;
    private int destPort;
    private int attempts = 0;
    private boolean ackReceived = false;
    
    public ActivateSender(ActivateMsg activate, NodeDescriptor destination) 
        throws MalformedURLException, UnknownHostException {
      this.activate = activate;
      String[] urlParts = destination.getUrls()[0].split(":");
      if (urlParts.length != 3 || !urlParts[0].equals("reds-tcp")) {
        throw new MalformedURLException("Could not determine destination UDP host and port.");
      }
      destHost = InetAddress.getByName(urlParts[1]);
      destPort = Integer.parseInt(urlParts[2]);
    }

    public synchronized void send() {
      scheduler.schedule(this, 0, ACK_TIMEOUT);
    }
    
    public synchronized void setAckReceived() {
      ackReceived = true;
    }
    
    @Override
    public synchronized void run() {
      // If we have received the ack, the ACTIVATE has been sent successfully and this class has
      // nothing more to do.
      // If we have not received the ack and we still have attempts left, (re)transmit the ACTIVATE
      // and (re)schedule this task to check the ack.
      // If we have not received the ack and all transmission attempts have been used, give up and
      // log an error.
      if (ackReceived) {
        // ACTIVATE sent successfully.
        cancelTask();
      } else {
        if (attempts < MAX_ATTEMPTS) {
          try {
            sendOobMsg(activate, destHost, destPort);
          } catch (IOException ioe) {
            logger.warning(ioe + "while sending ACTIVATE. Will retry later.");
          }
          attempts++;
          // Do not cancel the task so the scheduler will execute it again after ACK_TIMEOUT.
        } else {
          logger.severe("All attempts to send ACTIVATE to " + destHost + ":" + destPort + " " +
                        "have failed. Giving up.");
          cancelTask();
        }
      }
    }
    
    private void cancelTask() {
      this.cancel();
      synchronized (activateSenders) {
        activateSenders.remove(activate.getReconfId());
      }
    }
    
  } // ActivateSender
  
} // InformedLinkActivationReconfigurator