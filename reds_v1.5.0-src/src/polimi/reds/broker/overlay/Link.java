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

import java.io.Serializable;
import java.io.IOException;

/**
 * A data link between two REDS brokers. Each <code>Link</code> is associated with a
 * <code>Transport</code>. It hides the protocol to send and receive data (i.e., any
 * <code>Serializable</code> object) between neighboring brokers. Each data item has an associated
 * subject (a <code>String</code>) that can be used by the upper layers to distinguish between
 * different types of data.
 * 
 * @see polimi.reds.broker.overlay.Transport
 */
public interface Link {
  /**
   * Closes the link.
   * 
   * After a link is closed it is removed from the transport and cannot be reopened.
   */
  public void close();

  /**
   * Checks if the link is connected.
   * 
   * When created through the {@link Transport.openLink} method the link is connected. To disconnect
   * it the {@link close} must be called.
   * 
   * @return true if the link has not been closed, false otherwise.
   */
  public boolean isConnected();

  /**
   * Sends a data item with the specific subject to the neighbor connected through this link.
   * 
   * @param subject the subject of the data.
   * @param data the data to send.
   * @throws IOException if an I/O error occurs.
   * @throws NotConnectedException if the link is not connected (i.e., it has been closed).
   * @throws NotRunningException if the transport is not running.
   */
  public void send(String subject, Serializable data) throws IOException, NotConnectedException,
      NotRunningException;

  /**
   * Gets the <code>Transport</code> this link is associated to.
   * 
   * @return the <code>Transport</code> this link is associated to.
   */
  public Transport getTransport();
}
