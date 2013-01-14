/***
 * * REDS - REconfigurable Dispatching System
 * * Copyright (C) 2003 Politecnico di Milano
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
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Logger;
import polimi.reds.Filter;
import polimi.reds.Message;
import polimi.reds.NodeDescriptor;
import polimi.reds.broker.overlay.NotConnectedException;
import polimi.reds.broker.overlay.NotRunningException;
import polimi.reds.broker.overlay.Overlay;

/**
 * This class implements a subscription forwarding routing strategy among a set
 * of brokers connected in an unrooted tree.
 */
public class SubscriptionForwardingRoutingStrategy implements RoutingStrategy {
  /**
   * Reference to the router. Must be set before starting the broker (use the
   * <code>setRouter</code> method.
   */
  protected Router router = null;
  private Logger logger;
  private Overlay overlay = null;

  public SubscriptionForwardingRoutingStrategy() {
    logger = Logger.getLogger("polimi.reds.Router");
  }

  public void subscribe(NodeDescriptor neighbor, Filter filter) {
    // Due to the message queuing performed by GenericOverlay, a subscription can be processed after
    // the sender has actually been removed from the neighbors. In that case we must not accept the
    // subscription, else it will never be removed from the table unless the same neighbor connects
    // again to the local broker.
    if (!router.getOverlay().isNeighborOf(neighbor)) {
      return;
    }
    SubscriptionTable subscriptionTable = router.getSubscriptionTable();
    // If we already have this subscription there is nothing to do.
    if(subscriptionTable.isSubscribed(neighbor, filter)) {
      return;
    }
    logger.finest("Subscribing "+neighbor+" to "+filter);
    NodeDescriptor d;
    // Forward the subscription.
    if(!subscriptionTable.isFilterInTable(filter)) {
      // This is a new filter, all neighbors except the sender should receive it
      Collection<NodeDescriptor> l = overlay.getAllNeighborsExcept(neighbor);
      if(l != null){
        Iterator it = l.iterator();
        while(it.hasNext()) {
          d = (NodeDescriptor) it.next();
          if(d.isBroker())
            try {
              overlay.send(Router.SUBSCRIBE, filter, d);
            } catch (NotConnectedException e) {
              logger.fine("Error while forwarding subscription: " + d + " is now disconnected.");
            } catch (IOException e) {
              logger.warning("I/O error (" + e.getMessage() + ") while forwarding subscription " +
                             "to " + d);
            } catch (NotRunningException e) {
              logger.severe("Error while forwarding subscription: overlay not running.");
              e.printStackTrace();
            }
        }
      }
    } else {
      // There is another subscriber; is it the only one?
      d = (NodeDescriptor) subscriptionTable.getSingleSubscribedBroker(filter);
      if(d!=null&&!d.equals(neighbor))
        try {
          overlay.send(Router.SUBSCRIBE, filter, d);
        } catch (NotConnectedException e) {
          logger.fine("Error while forwarding subscription: " + d + " is now disconnected.");
        } catch (IOException e) {
          logger.warning("I/O error (" + e.getMessage() + ") while forwarding subscription " +
                         "to " + d);
        } catch (NotRunningException e) {
          logger.severe("Error while forwarding subscription: overlay not running.");
          e.printStackTrace();
        }
    }
    // Update the local subscription table
    subscriptionTable.addSubscription(neighbor, filter);
  }
  
  public void unsubscribe(NodeDescriptor neighbor, Filter filter) {
    SubscriptionTable subscriptionTable = router.getSubscriptionTable();
    // If the neighbor was not subscribed to the filter we have nothing to do.
    if (!subscriptionTable.isSubscribed(neighbor, filter)) {
      return;
    }
    logger.finest("Unsubscribing "+neighbor.getID()+" from "+filter);
    NodeDescriptor d;
    // Locally unsubscribe
    subscriptionTable.removeSubscription(neighbor, filter);
    // Forward the unsubscription
    if(!subscriptionTable.isFilterInTable(filter)) {
      // No more subscribers: forward the unsubscription to all other brokers
    	Collection<NodeDescriptor> l = overlay.getAllNeighborsExcept(neighbor);
    	if(l != null){
	      Iterator it = l.iterator();
	      while(it.hasNext()) {
	        d = (NodeDescriptor) it.next();
	        if(d.isBroker())
				try {
					overlay.send(Router.UNSUBSCRIBE, filter, d);
				} catch (NotConnectedException e) {
					logger.fine("Error while forwarding unsubscription: " + d + " is now " +
                                "disconnected.");
                } catch (IOException e) {
                    logger.warning("I/O error ("+e.getMessage()+") while forwarding unsubscription to "
                            +d+".");
                } catch (NotRunningException e) {
                    logger.severe("Error while forwarding unsubscription: overlay not running.");
                    e.printStackTrace();
                }
	      }
    	}
    } else {
      // There is another subscriber; is this the only one?
      d = (NodeDescriptor) subscriptionTable.getSingleSubscribedBroker(filter);
      if(d!=null)
		try {
			overlay.send(Router.UNSUBSCRIBE, filter, d);
		} catch (NotConnectedException e) {
			logger.fine("Error while forwarding unsubscription: " + d + " is now disconnected.");
        } catch (IOException e) {
            logger.warning("I/O error ("+e.getMessage()+") while forwarding unsubscription to "
                    +d+".");
        } catch (NotRunningException e) {
            logger.severe("Error while forwarding unsubscription: overlay not running.");
            e.printStackTrace();
        }
    }
  }

  public void unsubscribeAll(NodeDescriptor neighbor) {
    logger.finest("Unsubscribing "+neighbor.getID()+" from all filters");
    SubscriptionTable subscriptionTable = router.getSubscriptionTable();
    // The getAllFilters method returns a copy of the list of filters, so it is
    // safe to use the list even if the subscription table changes during 
    // unsubscription.
    Collection c = subscriptionTable.getAllFilters(neighbor);
    if(!c.isEmpty()) {
      Iterator it = c.iterator();
    	while(it.hasNext())
        unsubscribe(neighbor, (Filter) it.next());
    }
  }

  /**
   * @see RoutingStrategy#publish(NodeDescriptor, Message)
   */
  public FutureInt publish(NodeDescriptor sourceID, Message message) {
    logger.finest("Publishing "+message+" coming from "+sourceID);
    SubscriptionTable subscriptionTable = router.getSubscriptionTable();
    NodeDescriptor d;
    // Iterate over the collection of subscribed neighbors, forwarding them the message
    Iterator it = subscriptionTable.matches(message, sourceID).iterator();
    // Counts the number of neighbors that receive the message.
    int numNeighbor = 0;
    while(it.hasNext()) {
      d = (NodeDescriptor) it.next();
      if(d.equals(sourceID)) continue;
      try {
        overlay.send(Router.PUBLISH, message, d);
        numNeighbor++;
      } catch (NotConnectedException e) {
          logger.fine("Error while forwarding message: " + d + " is now disconnected.");
      } catch (IOException e) {
          logger.warning("I/O error ("+e.getMessage()+") while forwarding message to " + d);
      } catch (NotRunningException e) {
          logger.severe("Error while forwarding message: overlay not running.");
          e.printStackTrace();
      }
    }
    return new FutureInt(numNeighbor);
  }

  public void setRouter(Router router) {
    this.router = router;
    this.overlay = this.router.getOverlay();
  }

}