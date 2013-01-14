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

/**
 * Components implementing this interface may listen for the arrival of new data (i.e., any
 * <code>Serializable</Code> object) through some <code>Transport</code>.
 * 
 * @see polimi.reds.broker.overlay.Transport
 */
public interface DataListener {
  /**
   * This method is called whenever a new data item arrives from a neighbor of the local node.
   * 
   * @param subject The subject the data was addressed to.
   * @param source The <code>Link</code> the data comes from.
   * @param data The received data item.
   */
  public void notifyDataArrived(String subject, Link source, Serializable data);
}
