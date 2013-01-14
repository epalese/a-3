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
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.util.Collection;

/**
 * The interface of the data-link layer of a REDS broker. Objects implementing this interface
 * manages the connections between the local node and its neighbors, offering methods to send and
 * receive data (i.e., any <code>Serializable</code> object). Each data item has an associated
 * subject (a <code>String</code>) that can be used by the upper layers to distinguish between
 * different kind of data. Classes that implement this interface differ for the type of protocol
 * they use to connect and exchange data items.
 */
public interface Transport {
  /**
   * Starts this <code>Transport</code>.
   * 
   * Transports must be started before performing every other operation.
   * 
   * @throws IOException if something goes wrong setting up the components to accept incoming
   *             connections.
   */
  public void start() throws IOException;

  /**
   * Stops this <code>Transport</code>.
   * 
   * @throws IOException if something goes wrong stopping the transport.
   */
  public void stop() throws IOException;

  /**
   * Checks whether this <code>Transport</code> is running (i.e., it has been started and not yet
   * stopped).
   * 
   * @return <code>true</code> if this transport is running, <code>false</code> otherwise.
   */
  public boolean isRunning();

  /**
   * Activates and deactivates the beaconing mechanism to check the status of links.
   * 
   * If beaconing is active the node requires data to arrive periodically to keep the connection
   * alive and periodically send data.
   * 
   * @param beaconing <code>true</code> to enable the beaconing mechanism, <code>false</code>
   *            otherwise.
   */
  public void setBeaconing(boolean beaconing);

  /**
   * Checks whether the beaconing mechanism is active.
   * 
   * @return <code>true</code> if the beaconing mechanism is active, <code>false</code>
   *         otherwise.
   * @see #setBeaconing(boolean)
   */
  public boolean isBeaconing();

  /**
   * Gets the URLs associated with this transport.
   * 
   * @return the URLs associated with this transport.
   */
  public String[] getUrls();

  /**
   * Checks whether this transport knows the given protocol.
   * 
   * This method can be used to decide if this transport is able to speak the given protocol, and
   * consequently it can be used to open links toward a node identified by an URL characterized by
   * such protocol.
   * 
   * @param protocol the protocol to check.
   * @return <code>true</code> if this transport knows the given protocol, <code>false</code>
   *         otherwise.
   */
  public boolean knowsProtocol(String protocol);

  /**
   * Opens a new link with the node located at the specified URL.
   * 
   * If everything goes right the new link is opened and the <code>OpenLinkListener</code>s
   * registered are notified of the new link, otherwise an exception is thrown.
   * 
   * @param url The URL of the node.
   * @return The new link.
   * @throws MalformedURLException if the <code>url</code> is not a valid REDS URL.
   * @throws ConnectException if the connection cannot be opened.
   * @throws NotRunningException if the transport is not running.
   */
  public Link openLink(String url) throws MalformedURLException, ConnectException,
      NotRunningException;

  /**
   * Gets the collection of all links opened (and not yet closed) so far.
   * 
   * @return the collection of all links opened (and not yet closed) so far.
   */
  public Collection<Link> getOpenLinks();

  /**
   * Registers a listener for the arrival of new data addressed to the given subject.
   * 
   * If the listener (i.e., a listener that <code>equals</code> this one) was added before this
   * operation has no effect.
   * 
   * @param listener the component to be notified when new data arrive.
   * @param subject the subject to listen for.
   */
  public void addDataListener(DataListener listener, String subject);

  /**
   * Registers a listener for connectivity changes.
   * 
   * Registers a listener for the events of opening/closing/crashing of links. If the listener
   * (i.e., a listener that <code>equals</code> this one) was added before this operation has no
   * effect.
   * 
   * @param listener the component to be notified when connectivity changes.
   */
  public void addConnectivityChangeListener(ConnectivityChangeListener listener);

  /**
   * Removes the given listener.
   * 
   * Removes the first <code>DataListener</code> (among those registered for the given subject)
   * that <code>equals</code> the one passed as a parameter.
   * 
   * @param listener the listener to be removed.
   * @param subject the subject the listener to be removed was registered to.
   */
  public void removeDataListener(DataListener listener, String subject);

  /**
   * Removes the given listener.
   * 
   * Removes the first registered <code>ConnectivityChangeListener</code> that <code>equals</code>
   * the one passed as a parameter.
   * 
   * @param listener the listener to be removed
   */
  public void removeConnectivityChangeListener(ConnectivityChangeListener listener);
}