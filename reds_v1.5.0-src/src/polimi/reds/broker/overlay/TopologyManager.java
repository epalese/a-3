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

import polimi.reds.NodeDescriptor;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.util.Collection;

/**
 * The component responsible of managing the topology of a network of REDS brokers.
 * 
 * It must enforce the expected constraints (e.g., the fact that the overlay is aciclic and that it
 * remains connected even in presence of crashing links/brokers). It uses the services provided by
 * one or more {@link Transport}s to manage the links among neighboring brokers.
 * 
 * @see polimi.reds.broker.overlay.GenericOverlay
 * @see polimi.reds.broker.overlay.Transport
 */
public interface TopologyManager extends ConnectivityChangeListener {
  /**
   * Starts this topology manager.
   */
  public void start();

  /**
   * Stops this topology manager.
   */
  public void stop();

  /**
   * Checks whether this topology manager is running (i.e., it has been started and not yet
   * stopped).
   * 
   * @return True if this topology manager is running.
   */
  public boolean isRunning();

  /**
   * Connects to the node located at the specified URL. If everything goes right the specified node
   * becomes a new neighbor and the <code>NeighborAddedListener</code>s registered are notified
   * of the new neighbor, otherwise an exception is thrown.
   * 
   * @see #tentativelyAddNeighbor(String)
   * 
   * @param url The URL of the node to connect.
   * @throws NotRunningException If the topology manager is not running.
   * @throws MalformedURLException If the given URL has a wrong format.
   * @throws ConnectException If the connection fails.
   * @return The <code>NodeDescriptor</code> of the new neighbor.
   */
  public NodeDescriptor addNeighbor(String url) throws NotRunningException, MalformedURLException,
      ConnectException;

  /**
   * Try connecting to the node located at the specified URL. If everything goes right the specified
   * node becomes a new neighbor and the <code>NeighborAddedListener</code>s registered are
   * notified of the new neighbor. If connecting with the specified node violates some constraints
   * among those enforced by this <code>TopologyManager</code>, this method does nothing (it does
   * not open the new connection and it does not notify the listeners).
   * 
   * @see #addNeighbor(String)
   * 
   * @param url The URL of the node to connect.
   * @throws NotRunningException If the topology manager is not running.
   * @throws MalformedURLException If the given URL has a wrong format or a transport knowing the
   *             corresponding protocol (see {@link Transport#knowsProtocol(String)}) was not
   *             added.
   */
  public void tentativelyAddNeighbor(String url) throws NotRunningException, MalformedURLException;

  /**
   * Disconnects from the specified neighbor. If everything goes right the connection is closed and
   * the <code>NeighborhoodChangeListener</code>s registered are notified. If the specified node
   * was not a neighbor of the local node, this method does nothing.
   * 
   * @param node The node to disconnect.
   */
  public void removeNeighbor(NodeDescriptor node);

  /**
   * Registers a listener for neighborhood changes.
   * 
   * Registers a listener for the events of adding/removing/crashing of neighbors. If the listener
   * (i.e., a listener that <code>equals</code> this one) was added before this operation has no
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
   * @param listener The listener to remove.
   */
  public void removeNeighborhoodChangeListener(NeighborhoodChangeListener listener);

  /**
   * Gets the current set of neighbors.
   * 
   * @return The current set of neighbors.
   */
  public Collection<NodeDescriptor> getNeighbors();

  /**
   * Gets the current set of neighbors excluding the specified one.
   * 
   * @param excludedNeighbor The excluded neighbor
   * @return The set of neighbors excluding the specified one.
   */
  public Collection<NodeDescriptor> getAllNeighborsExcept(NodeDescriptor excludedNeighbor);

  /**
   * Checks whether the givn node is a neighbor of the local node.
   * 
   * @return True if the given node is a neighbor, false otherwise.
   */
  public boolean isNeighborOf(NodeDescriptor neighbor);

  /**
   * Retrieves the link used to communicate with the given neighbor.
   * 
   * If the specified node is not a neighbor it returns null. If several links connect this node
   * with the specified neighbor, one of them is returned (non deterministically).
   * 
   * @param neighbor the neighbor to retrieve the link for.
   * @return the link connecting this node with the specified neighbor or null if the specified node
   *         is not a neighbor.
   */
  public Link getLinkFor(NodeDescriptor neighbor);

  /**
   * Retrieves the node descriptor of the neighbor connected through the given link.
   * 
   * If the specified link does not exist it returns null.
   * 
   * @param link the link to retrieve the neighbor for.
   * @return the node descriptor of the neighbor connected through the given link.
   */
  public NodeDescriptor getNeighborFor(Link link);

  /**
   * Gets the current number of neighbors of the local node.
   * 
   * @return The current number of neighbors of the local node.
   */
  public int getNumberOfNeighbors();

  /**
   * Gets the number of brokers that are directly connected with the local node.
   * 
   * @return The number of brokers that are directly connected with the local node.
   */
  public int getNumberOfBrokers();

  /**
   * Gets the number of clients that are directly connected with the local node.
   * 
   * @return The number of clients of the local node.
   */
  public int getNumberOfClients();

  /**
   * Associates a new transport to this topology manager.
   * 
   * @param tr The transport to add.
   */
  public void addTransport(Transport tr);

  /**
   * Removes the given transport from the collection of transports used by this topology manager.
   * 
   * @param tr The transport to remove.
   */
  public void removeTransport(Transport tr);

  /**
   * Gets the collection of transports used by this topology manager.
   * 
   * @return The collection of transports used by this topology manager.
   */
  public Collection<Transport> getTransports();

  /**
   * Sets the {@link NodeDescriptor} used by this topology manager.
   * 
   * @param nd The {@link NodeDescriptor} used by this topology manager.
   */
  public void setNodeDescriptor(NodeDescriptor nd);
}