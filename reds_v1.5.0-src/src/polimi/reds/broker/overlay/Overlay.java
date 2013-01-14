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

package polimi.reds.broker.overlay;

import java.io.IOException;
import java.io.Serializable;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.util.Collection;
import polimi.reds.NodeDescriptor;

/**
 * As its name suggests, the <code>Overlay</code> is the component of a REDS broker in charge of
 * managing the overlay network of brokers on top of which CBR is performed. It offers methods to
 * add/remove neighbors and to send/receive packets (i.e., any <code>Serializable</code> object).
 * Each packet has an associated subject (a <code>String</code>) that can be used by the upper
 * layers to distinguish between different kind of packets (e.g., publish vs. subscribe).
 * <p>
 * An overlay can manage different queues for incoming packets, each associated with one or more
 * subjects. This implicitly defines different traffic classes. In general a single "default"
 * traffic class (i.e., queue) is used. The overlay offers methods to get the collection of known
 * traffic classes and to associate a subject with a new/existing traffic class.
 * <p>
 * Finally, each REDS broker has its own, unique {@link polimi.reds.NodeDescriptor}, which is
 * created and managed by the <code>Overlay</code> component.
 */
public interface Overlay {
  /**
   * Starts this overlay. An overlay must be started before it can send and receive packets.
   * 
   * @see #stop()
   */
  public void start();

  /**
   * Stops this overlay.
   * 
   * @see #start()
   */
  public void stop();

  /**
   * Checks weather this overlay is running (i.e., it has been started and not yet stopped).
   * 
   * @return True if this overlay is running.
   */
  public boolean isRunning();

  /**
   * Gets the node descriptor of the local node.
   * 
   * @return the node descriptor of the local node.
   */
  public NodeDescriptor getNodeDescriptor();

  /**
   * Connects to the node located at the specified URL. If everything goes right the specified node
   * becomes a new neighbor and the <code>NeighborAddedListener</code>s registered are notified
   * of the new neighbor, otherwise an exception is thrown.
   * 
   * @see #tentativelyAddNeighbor(String)
   * 
   * @param url the URL of the node to connect.
   * @throws NotRunningException if the topology manager is not running.
   * @throws MalformedURLException if the given URL has a wrong format.
   * @throws AlreadyNeighborException if the node to add is already a neighbor of the local node.
   * @throws ConnectException if the connection fails.
   * @return the <code>NodeDescriptor</code> of the new neighbor.
   */
  public NodeDescriptor addNeighbor(String url) throws NotRunningException, MalformedURLException,
      AlreadyNeighborException, ConnectException;

  /**
   * Try connecting to the node located at the specified URL. If everything goes right the specified
   * node becomes a new neighbor and the <code>NeighborAddedListener</code>s registered are
   * notified of the new neighbor. If connecting with the specified node violates some constraints
   * among those enforced by this <code>TopologyManager</code>, this method does nothing (it does
   * not open the new connection and it does not notify the listeners).
   * 
   * @see #addNeighbor(String)
   * 
   * @param url the URL of the node to connect.
   * @throws NotRunningException if the topology manager is not running.
   * @throws MalformedURLException if the given URL has a wrong format or a transport knowing the
   *             corresponding protocol (see {@link Transport#knowsProtocol(String)}) was not
   *             added.
   */
  public void tentativelyAddNeighbor(String url) throws NotRunningException, MalformedURLException;

  /**
   * Disconnects from the specified neighbor. If everything goes right the connection is closed and
   * the <code>NeighborhoodChangeListener</code>s registered are notified. If the specified node
   * was not a neighbor of the local node, this method does nothing.
   * 
   * @param node the node to disconnect.
   */
  public void removeNeighbor(NodeDescriptor node);

  /**
   * Registers a listener for neighborhood changes.
   * 
   * Registers a listener for the events of adding/removing/crashing of neighbors. If the listener
   * (i.e., a listener that <code>equals</code> this one) was added before, this operation has no
   * effect.
   * 
   * @param listener the listener to add.
   */
  public void addNeighborhoodChangeListener(NeighborhoodChangeListener listener);

  /**
   * Removes the given listener.
   * 
   * Removes the first registered <code>NeighborhoodChangeListener</code> that <code>equals</code>
   * the one passed as a parameter.
   * 
   * @param listener the listener to remove.
   */
  public void removeNeighborhoodChangeListener(NeighborhoodChangeListener listener);

  /**
   * Gets the current set of neighbors.
   * 
   * @return the current set of neighbors.
   */
  public Collection<NodeDescriptor> getNeighbors();

  /**
   * Gets the current set of neighbors excluding the specified one.
   * 
   * @param excludedNeighbor the excluded neighbor
   * @return the set of neighbors excluding the specified one.
   */
  public Collection<NodeDescriptor> getAllNeighborsExcept(NodeDescriptor excludedNeighbor);

  /**
   * Checks whether the given node is a neighbor of the local node.
   * 
   * @return true if the given node is a neighbor, false otherwise.
   */
  public boolean isNeighborOf(NodeDescriptor neighbor);

  /**
   * Gets the current number of neighbors of the local node.
   * 
   * @return the current number of neighbors of the local node.
   */
  public int getNumberOfNeighbors();

  /**
   * Gets the number of brokers that are directly connected with the local node.
   * 
   * @return the number of brokers that are directly connected with the local node.
   */
  public int getNumberOfBrokers();

  /**
   * Gets the number of clients that are directly connected with the local node.
   * 
   * @return the number of clients of the local node.
   */
  public int getNumberOfClients();

  /**
   * The default traffic class used for packets addressed to subjects for which no different traffic
   * class was specified.
   * 
   * @see #setTrafficClass(String, String)
   */
  public final static String DEFAULT_TRAFFIC_CLASS = "MiscClass";

  /**
   * Gets the collection of traffic classes known to this overlay.
   * 
   * @return the collection of traffic classes known to this overlay.
   */
  public Collection<String> getTrafficClasses();

  /**
   * Sets the traffic class to be used for packets sent to the given subject. If this is the first
   * time the given traffic class is mentioned, a new (unbounded) queue is created. If the same
   * subject was previously associated to a different traffic class, then the association is
   * changed.
   * 
   * @param subject the subject of packets that will be associated with the given traffic class.
   * @param trafficClass the traffic class to be used for packets sent to the given subject.
   */
  public void setTrafficClass(String subject, String trafficClass);

  /**
   * Creates a new traffic class with a queue of the given size. If <code>queueSize</code> is -1
   * than a unbounded queue is used.
   * 
   * @param trafficClass
   * @param queueSize
   * @throws IllegalArgumentException if the traffic class has been already created before.
   */
  public void addTrafficClass(String trafficClass, int queueSize) throws IllegalArgumentException;

  /**
   * Registers a listener for the arrival of new packets addressed to the given subject.
   * 
   * If the listener (i.e., a listener that <code>equals</code> this one) was added before, this
   * operation has no effect.
   * 
   * @param listener the component to be notified when new packets arrive.
   * @param subject the subject to listen for.
   */
  public void addPacketListener(PacketListener listener, String subject);

  /**
   * Removes the given listener.
   * 
   * Removes the first <code>PacketListener</code> (among those registered for the given subject)
   * that <code>equals</code> the one passed as a parameter.
   * 
   * @param listener the listener to be removed.
   * @param subject the subject the listener to be removed was registered to.
   */
  public void removePacketListener(PacketListener listener, String subject);

  /**
   * Sends a packet with the specified subject to the given recipient (which must be a neighbor of
   * the local node).
   * 
   * @param subject the subject to address the packet to.
   * @param packet the packet to send.
   * @param recipient the receiver of the packet.
   * @throws IOException if an I/O error occurs.
   * @throws NotConnectedException if the recipient is not a neighbor of the local node.
   * @throws NotRunningException if the overlay is not running.
   */
  public void send(String subject, Serializable packet, NodeDescriptor recipient)
      throws IOException, NotConnectedException, NotRunningException;
}
