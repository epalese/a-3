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

import polimi.reds.Filter;
import polimi.reds.Message;
import polimi.reds.NodeDescriptor;
import polimi.reds.Reply;
import polimi.reds.broker.overlay.PacketListener;
import polimi.reds.broker.overlay.Overlay;

/**
 * This interface is the "core" of a REDS broker. Classes that implement this interface constitutes
 * the main component of a REDS broker. It is registered to the <code>Overlay</code> to receive
 * messages coming from the other nodes of the network. In most cases it delegates its main
 * functionalities to the other components that compose a REDS broker: namely, the
 * <code>RoutingStrategy</code> and the <code>ReplyManager</code>. It holds the common data
 * structure managed by those components: the <code>SubscriptionTable</code> and the
 * <code>ReplyTable</code>.
 */
public interface Router extends PacketListener {
  /**
   * Subject for unsubscription messages.
   */
  public static final String UNSUBSCRIBE = "unsubscribe";
  /**
   * Subject for unsubscription messages for all filters.
   */
  public static final String UNSUBSCRIBEALL = "unsubscribeAll";
  /**
   * Subject for publication messages.
   */
  public static final String PUBLISH = "publish";
  /**
   * Subject for subscription messages.
   */
  public static final String SUBSCRIBE = "subscribe";
  /**
   * Subject for reply messages.
   */
  public static final String REPLY = "reply";
  /**
   * Recommended traffic class for <code>PUBLISH</code> and <code>REPLY</code> messages.<br>
   * The actual association between a subject and a traffic class must be performed by the router
   * implementation in use. The router can also choose to use classes different from the recommended
   * ones, or leave all messages in the default class.
   */
  public static final String MESSAGE_CLASS = "MessageClass";
  /**
   * Recommended traffic class for <code>SUBSCRIBE</code>, <code>UNSUBSCRIBE</code> and
   * <code>UNSUBSCRIBEALL</code> messages.<br>
   * The actual association between a subject and a traffic class must be performed by the router
   * implementation in use. The router can also choose to use classes different from the recommended
   * ones, or leave all messages in the default class. However, the <code>SUBSCRIBE</code>,
   * <code>UNSUBSCRIBE</code> and <code>UNSUBSCRIBEALL</code> subjects <b>MUST</b> belong to
   * the same traffic class; putting them in separate classes may lead to incorrect subscription
   * tables.
   */
  public static final String FILTER_CLASS = "FilterClass";

  /**
   * Subscribes the specified neighbor to the messages matching the given <code>Filter</code>.
   * <p>
   * Note that reconfigurators assume that the implementation of this method is synchronized on the
   * router object, so that the reconfigurator can acquire the lock when it needs to make sure that
   * no router operations are executed concurrently with reconfiguration actions.
   * 
   * @param neighborID the identifier of the neighbor which subscribed.
   * @param filter the <code>Filter</code> used to determine the messages the neighbor is
   *            interested in.
   */
  public void subscribe(NodeDescriptor neighborID, Filter filter);

  /**
   * Unsubscribes the specified neighbor from the messages matching the given <code>Filter</code>.
   * In general, it undoes a corresponding call to the <code>subscribe</code> method.
   * <p>
   * Note that reconfigurators assume that the implementation of this method is synchronized on the
   * router object, so that the reconfigurator can acquire the lock when it needs to make sure that
   * no router operations are executed concurrently with reconfiguration actions.
   * 
   * @param neighborID the identifier of the neighbor which wants to be unsubscribed.
   * @param filter the <code>Filter</code> used to determine the messages the neighbor is no more
   *            interested in.
   */
  public void unsubscribe(NodeDescriptor neighborID, Filter filter);

  /**
   * Removes all the subscriptions previously issued by the give neighbor.
   * <p>
   * Note that reconfigurators assume that the implementation of this method is synchronized on the
   * router object, so that the reconfigurator can acquire the lock when it needs to make sure that
   * no router operations are executed concurrently with reconfiguration actions.
   * 
   * @param neighborID the identifier of the neighbor which requested to be unsubscribed.
   */
  public void unsubscribeAll(NodeDescriptor neighborID);

  /**
   * Publish the given message coming from the specified neighbor. Depending on the routing policy
   * adopted, this requires to forward the given message to some or any of the neighbors of the
   * broker this router is part of.<br>
   * If <code>message</code> is instance of<code>Repliable</code>, the <code>Router</code>
   * needs to activate the <code>ReplyManager</code> to record the message in the
   * <code>ReplyTable</code>.
   * <p>
   * Note that reconfigurators assume that the implementation of this method is synchronized on the
   * router object, so that the reconfigurator can acquire the lock when it needs to make sure that
   * no router operations are executed concurrently with reconfiguration actions.
   * 
   * @param neighborID the identifier of the neighbor from which the message was received.
   * @param message the <code>Message</code> to be published.
   * 
   */
  public void publish(NodeDescriptor neighborID, Message message);

  /**
   * Returns the unique identifier of this broker within the REDS network.
   * 
   * @return the unique identifier of this broker within the REDS network.
   */
  public NodeDescriptor getID();

  /**
   * Returns the subscription table used by this broker.
   * 
   * @return the subscription table used by this broker.
   */
  public SubscriptionTable getSubscriptionTable();

  /**
   * Returns the reply table used by this broker.
   * 
   * @return the reply table used by this broker.
   */
  public ReplyTable getReplyTable();

  /**
   * Set the overlay.
   * 
   * @param overlay the overlay
   */
  public void setOverlay(Overlay o);

  /**
   * Get the overlay.
   * 
   * @return the overlay
   */
  public Overlay getOverlay();

  /**
   * Forwards the given reply to to its sender. According to the informations contained into the
   * local reply table the reply will be sent to a specific neighbor or dropped if no information
   * about its sender is contained in the reply table.
   * <p>
   * Note that reconfigurators assume that the implementation of this method is synchronized on the
   * router object, so that the reconfigurator can acquire the lock when it needs to make sure that
   * no router operations are executed concurrently with reconfiguration actions.
   * 
   * @param reply the reply to be sent.
   */
  public void forwardReply(Reply reply);
}
