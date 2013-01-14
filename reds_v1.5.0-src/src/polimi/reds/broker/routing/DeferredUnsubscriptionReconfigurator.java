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

import java.io.IOException;
import java.io.Serializable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import polimi.reds.Filter;
import polimi.reds.NodeDescriptor;
import polimi.reds.broker.overlay.NotConnectedException;
import polimi.reds.broker.overlay.NotRunningException;
import polimi.reds.broker.overlay.Overlay;
import polimi.reds.broker.overlay.PacketListener;
import polimi.reds.broker.overlay.ReconfigurationId;

/**
 * This class handles reconfigurations using the Timed Deferred Unsubscription or the 
 * Notified Deferred Unsubscription protocol. The protocol variant to use is chosen in the 
 * constructor.<br>
 * Note that if you create this class in the timed version and using a timeout of 0, the class
 * behaves as in the Strawman protocol.
 * <p>
 * The protocols are described in "Content-Based Routing for Publish-Subscribe on a Dynamic 
 * Topology: Concepts, Protocols and Evaluation", by G. Cugola, D. Frey, A.L. Murphy and G.P. Picco.
 * 
 * @author Andrea Milani
 */
public class DeferredUnsubscriptionReconfigurator implements Reconfigurator, PacketListener {
  
  // =============================================================================================
  //
  // IMPORTANT - SYNCHRONIZATION NOTE
  //
  // All the methods that handle reconfigurations must be synchronized so that they do not execute 
  // concurrently. Concurrent execution might cause wrong subscription propagation.
  // 
  // =============================================================================================
  
  /** Default timeout value (in milliseconds) for the deferral of unsubscriptions. */
  public static final long DEFAULT_DEFER_TIMEOUT = 3000;
  
  /** The subject used for FLUSH messages. */
  private static final String FLUSH_SBJ = "__DEFUNSUB_FLUSH";
  
  private Logger logger = Logger.getLogger(
      "polimi.reds.broker.routing.DeferredUnsubscriptionReconfigurator");
  
  private Router router = null;
  private Overlay overlay = null;
  
  // When true, the "notified" version of the protocol is used. When false, the "timed" version 
  // is used.
  private final boolean notified;
  
  // The timeout for the deferral of unsubscriptions.
  private long deferTimeout;
  // The timer used for the deferral of unsubscriptions.
  // The timer's thread is set to run as a daemon, so that it does not prevent
  // the VM from terminating when other threads have finished. Note that in this
  // way the program may terminate without having completed some deferred 
  // unsubscriptions, but in this case the neighbors of the local broker would 
  // notice that the broker has gone down, and consequently they would 
  // automatically remove all the subscriptions from the local broker.
  private Timer deferTimer = new Timer(true);
  
  // A map containing the tasks currently scheduled to execute the deferred unsubscriptions.
  // The map is used only with the "notified" protocol. Tasks are not inserted in the map when the 
  // "timed" protocol is used.
  // With the "notified" protocol, a task is inserted in the map after it has been scheduled; it is
  // removed after the unsubscriptions have been sent.
  private HashMap<ReconfigurationId, UnsubTask> deferredTasks = 
      new HashMap<ReconfigurationId, UnsubTask>();
  
  /**
   * Creates a new <code>DeferredUnsubscriptionReconfigurator</code> using the default timeout 
   * ({@value #DEFAULT_DEFER_TIMEOUT} ms) for the deferral of unsubscriptions.
   *
   * @param notified see the two parameter constructor.
   */
  public DeferredUnsubscriptionReconfigurator(boolean notified) {
    this(notified, DEFAULT_DEFER_TIMEOUT);
  }
  
  /**
   * Creates a new <code>DeferredUnsubscriptionReconfigurator</code> using the specified parameters.
   * 
   * @param notified <code>true</code> to use the "notified" version of the protocol, 
   *        <code>false</code> to use the "timed" version.
   * @param deferTimeout timeout (in milliseconds) to use for the deferral of unsubscriptions.
   */
  public DeferredUnsubscriptionReconfigurator(boolean notified, long deferTimeout) {
    this.notified = notified;
    this.deferTimeout = deferTimeout;
  }
  
  /**
   * Sets the timeout to use for the deferral of unsubscriptions. The new setting will take effect
   * starting from the next reconfiguration.
   * 
   * @param timeout the deferral timeout, in milliseconds.
   */
  public synchronized void setDeferTimeout(long timeout) {
    this.deferTimeout = timeout;
  }
  
  public synchronized void setRouter(Router r) {
    router = r;
    overlay = r.getOverlay();
    overlay.addNeighborhoodChangeListener(this);
    if (notified) {
      overlay.addPacketListener(this, FLUSH_SBJ);
      overlay.setTrafficClass(FLUSH_SBJ, Router.FILTER_CLASS);
    }
  }
    
  public synchronized void notifyNeighborAdded(NodeDescriptor addedNeighbor,
                                               Serializable reconfInfo) {
    
    if (addedNeighbor.isClient()) {
      // We must not propagate the local subscriptions to clients.
      return;
    }
    
    // The new neighbor is a broker. Propagate the local broker's subscriptions to it.
    // Synchronize on the router to make sure that no one unsubscribes from a filter after we have
    // retrieved it from the subscription table but before we have sent the subscription. If that
    // could happen, we could send a subscription for a no longer needed filter, causing useless
    // traffic in the future.
    synchronized (router) {
      SubscriptionTable subTbl = router.getSubscriptionTable();
      Collection<Filter> filters = subTbl.getAllFiltersExcept(false, addedNeighbor);
      for (Filter filter : filters) {
        try {
          overlay.send(Router.SUBSCRIBE, filter, addedNeighbor);
        } catch (NotConnectedException nce) {
          logger.warning(nce + " while sending subscriptions to " + addedNeighbor);
        } catch (IOException ioe) {
          logger.warning(ioe + " while sending subscriptions to " + addedNeighbor);
        } catch (NotRunningException nre) {
          logger.severe("Overlay not running while sending subscriptions to " + addedNeighbor);
          nre.printStackTrace();
        }
      }
    }
    
    if (notified) {
      // If the new node has been added as part of a reconfiguration, send it a FLUSH.
      if (reconfInfo == null) {
        logger.finer("reconfInfo == null in NeighborAdded: assuming the event is not " +
                     "part of a reconfiguration.");
      } else {
        try {
          // The cast of reconfInfo to ReconfigurationId is there to force a type-check on 
          // reconfInfo (otherwise any serializable parameter could be sent without causing 
          // exceptions).
          overlay.send(FLUSH_SBJ, (ReconfigurationId) reconfInfo, addedNeighbor);
        } catch (NotConnectedException nce) {
          logger.warning(nce + " while sending FLUSH to " + addedNeighbor);
        } catch (IOException ioe) {
          logger.warning(ioe + " while sending FLUSH to " + addedNeighbor);
        } catch (NotRunningException nre) {
          logger.severe("Overlay not running while sending FLUSH to " + addedNeighbor);
          nre.printStackTrace();
        }
      }
    }
    
  }
  
  public synchronized void notifyNeighborRemoved(NodeDescriptor removedNeighbor) {
    // Remove all the subscriptions of the neighbor.
    router.unsubscribeAll(removedNeighbor);
  }
  
  public synchronized void notifyNeighborDead(NodeDescriptor deadNeighbor, 
                                              Serializable reconfInfo) {
    
    if (deadNeighbor.isClient() || deferTimeout == 0) {
      router.unsubscribeAll(deadNeighbor);
      return;
    }
    
    SubscriptionTable subTbl = router.getSubscriptionTable();
    
    // Check whether the local broker was a leaf dispatcher, and the death of the neighbor has left
    // it isolated from other brokers. In this case, unsubscriptions are only local, and deferring 
    // them is not necessary. 
    if (overlay.getNumberOfBrokers() == 0) {
      synchronized (router) {
        subTbl.removeAllSubscriptions(deadNeighbor);
      }
      return;
    }
    
    // The local broker was not a leaf dispatcher. Execute local unsubscriptions immediately, but 
    // defer their propagation to other brokers.
    
    Collection<Filter> filtersToUnsub;
    synchronized (router) {
      // Save the list of filters to unsubscribe.
      filtersToUnsub = subTbl.getAllFilters(deadNeighbor);
      // If there are no filters to unsubscribe from, scheduling the deferred task is useless, 
      // unless we are in notified mode: in this case the task is necessary to recognize a FLUSH 
      // addressed to this node.
      if (filtersToUnsub.size() == 0 && !notified) {
        return;
      }
      subTbl.removeAllSubscriptions(deadNeighbor);
    }
    ReconfigurationId reconfId = null;
    if (reconfInfo != null) reconfId = (ReconfigurationId) reconfInfo;
    UnsubTask deferredTask = new UnsubTask(reconfId, deadNeighbor, filtersToUnsub);
    deferTimer.schedule(deferredTask, deferTimeout);
    // When in notified mode, save the task in deferredTasks so that it can be found when a FLUSH
    // is received.
    if (notified) {
      deferredTasks.put(reconfId, deferredTask);
    }
    
  }
  
  public synchronized void notifyPacketArrived(String subject, NodeDescriptor source,
                                               Serializable packet) {
     
    // Note that this method can be executed only when using the "notified" protocol. With the 
    // "timed" protocol this class does not register as a packet listener.
    
    if (!subject.equals(FLUSH_SBJ)) {
      logger.severe("Unexpected packet subject: '" + subject + "'.");
      return;
    }
    
    ReconfigurationId reconfId = (ReconfigurationId) packet;
    UnsubTask unsubTask = deferredTasks.get(reconfId);
    if (unsubTask != null) {
      // This broker is the addressee of the flush message: complete the reconfiguration identified
      // by reconfId.
      unsubTask.cancel();
      unsubTask.run();
    } else {
      // The ID in the flush message is unknown. Send the packet to the neighbors, so that it can 
      // reach its addressee.
      // Note that it might happen that the flush message for a certain reconfiguration arrives 
      // after the timeout for the deferred task has expired. In this case, the above check on 
      // deferredTasks will not recognize the ID in the flush message (because the task is no more 
      // in the deferredTasks map) and the broker will enter this code section even if it was the 
      // intended addressee. Anyway this should happen rarely, because the timeout should be tuned 
      // so that the flush message can be received on time in most cases (otherwise the "notified" 
      // protocol would be equivalent to the "timed" protocol, hence useless). Furthermore, this 
      // problem is harmless for the correctness of the protocol, it just increases the overhead a 
      // little.
      Collection<NodeDescriptor> neighbors = overlay.getAllNeighborsExcept(source);
      if (neighbors != null) {
        for (NodeDescriptor node : neighbors) {
          if (node.isBroker()) {
            try {
              overlay.send(FLUSH_SBJ, reconfId, node);
            } catch (NotConnectedException nce) {
              logger.fine(nce + " while sending FLUSH to " + node);
            } catch (IOException ioe) {
              logger.warning(ioe + " while sending FLUSH to " + node);
            } catch (NotRunningException nre) {
              logger.severe("Overlay not running while sending FLUSH to " + node);
              nre.printStackTrace();
            }
          }
        }
      }
    }
    
  }
  
  // Class used to schedule the deferred propagation of unsubscriptions.
  private class UnsubTask extends TimerTask {
    
    // The ID of the reconfiguration this task is part of.
    // This is used only in the "notified" protocol. It is null in the "timed" protocol.
    private ReconfigurationId reconfId;
    // The neighbor the filters to unsubscribe belong to.
    private NodeDescriptor neighbor;
    // The list of filters to unsubscribe when the task is executed.
    private Collection<Filter> filters;
    // This variable is set to true after the task has been executed.
    // It is used to avoid the task being executed twice. In fact the deferTimer runs in its own 
    // thread, and it may launch the run method while notifyPacketArrived is already running 
    // (but before the task is canceled on the timer). This would cause the call by the timer to 
    // block on the synchronized instruction, but the method would be executed (after 
    // notifyPacketArrived has already executed it) when notifyPacketArrived exits and releases the
    // synchronization lock.
    private boolean executed = false;
    
    public UnsubTask(ReconfigurationId reconfId, NodeDescriptor neighbor, 
                     Collection<Filter> filters) {
      this.reconfId = reconfId;
      this.neighbor = neighbor;
      this.filters = filters;
    }
    
    @Override
    public void run() {
      synchronized (DeferredUnsubscriptionReconfigurator.this) {
        
        // Check if the task has already been run.
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
        
        // The task has been executed.
        if (notified) deferredTasks.remove(reconfId);
        executed = true;
        
      }
    }
    
  } // UnsubTask
  
} // DeferredUnsubscriptionReconfigurator