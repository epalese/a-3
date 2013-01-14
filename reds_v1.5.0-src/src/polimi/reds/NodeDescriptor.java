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

package polimi.reds;

import java.io.Serializable;
import java.util.*;

/***************************************************************************************************
 * The descriptor of a REDS node (i.e., a broker or a client). It encapsulates the unique identifier
 * of the node and some other information about it (i.e., if it is a client or a broker and the URLs
 * that can be used to locate it).
 **************************************************************************************************/
public class NodeDescriptor implements Serializable, Comparable<NodeDescriptor> {
  private static final long serialVersionUID = 1972955749730791242L;
  /*
   * The identifier of the node.
   */
  private UUID id;
  /*
   * True if the node is a broker, false if it is a client.
   */
  private boolean broker;
  /**
   * The URLs that can be used to connect to the node.
   */
  protected Set<String> urls;

  /**
   * Creates a <code>NodeDescriptor</code> with a new identifier, which is guaranteed to be unique
   * in the network. Initially the set of URLs is empty.
   * 
   * @param isBroker <code>true</code> if the node is a broker, false otherwise (i.e., it is a
   *            client).
   */
  public NodeDescriptor(boolean isBroker) {
    id = UUID.randomUUID();
    broker = isBroker;
    urls = new HashSet<String>();
  }

  /**
   * Creates a <code>NodeDescriptor</code> for a node of the specified type (broker or not-broker,
   * i.e., client), with the specified URL, and with a new identifier, which is guaranteed to be
   * unique in the network.
   * 
   * @param mainURL The main (first) URL for connecting with the node.
   * @param isBroker True if the node is a broker, false otherwise (i.e., it is a client).
   */
  public NodeDescriptor(String mainURL, boolean isBroker) {
    this(isBroker);
    urls.add(mainURL);
  }

  /**
   * Gets the identifier of the node.
   * 
   * @return the identifier of the node.
   */
  public String getID() {
    return id.toString();
  }

  /**
   * Adds a new URL to this node descriptor.
   * 
   * @param url the URL to add.
   */
  public synchronized void addUrl(String url) {
    urls.add(url);
  }

  /**
   * Removes the given url from the set of URLs associated with this node descriptor.
   * 
   * @param url the URL to remove.
   */
  public synchronized void removeUrl(String url) {
    urls.remove(url);
  }

  /**
   * Get the node's URLs.
   * 
   * Note: these URLs depend on the <code>Transport</code>s available to the node at each
   * instant. In general, if this descriptor is serialized and sent to another node, no attempt is
   * made to mantain this information up-to-date and consistent (i.e., if the set of
   * <code>Transport</code>s changes, this information may become inaccurate).
   * 
   * @return the array of URLs that can be used to connect to this node.
   */
  public synchronized String[] getUrls() {
    return urls.toArray(new String[urls.size()]);
  }

  /**
   * Checks whether the node is a broker.
   * 
   * @return <code>true</code> if the node is a broker, <code>false</code> otherwise (it is a
   *         client).
   */
  public synchronized boolean isBroker() {
    return broker;
  }

  /**
   * Checks whether the node is a client.
   * 
   * @return <code>true</code> if the node is a client, <code>false</code> otherwise (it is a
   *         broker).
   */
  public synchronized boolean isClient() {
    return !broker;
  }

  public String toString() {
    return (broker ? "Broker:" : "Client:")+id;
  }

  public int hashCode() {
    return id.hashCode();
  }

  /**
   * Two <code>NodeDescriptor</code>s are equal if they have the same identifier.
   */
  public boolean equals(Object o) {
    if(o instanceof NodeDescriptor) { return id.equals(((NodeDescriptor) o).id); }
    return false;
  }

  /**
   * Compares two NodeDescriptors. Notice that clients' descriptors are always less than brokers'
   * one.
   * 
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(NodeDescriptor o) {
    if(this.isClient()&&o.isBroker()) return -1;
    if(this.isBroker()&&o.isClient()) return 1;
    return id.compareTo(o.id);
  }
}