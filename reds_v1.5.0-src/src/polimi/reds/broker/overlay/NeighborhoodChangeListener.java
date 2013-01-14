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

import polimi.reds.NodeDescriptor;

/**
 * Components implementing this interface may listen for the event of removing existing neighbors of
 * the local node.
 * 
 * @see Overlay
 */
public interface NeighborhoodChangeListener {
  /**
   * This method is called whenever a new neighbor connects (or is connected).
   * 
   * @param addedNeighbor The <code>NodeDescriptor</code> of the added neighbor.
   * @param reconfInfo additional information about the topology change.<br>
   *            The <code>Reconfigurator</code> can use this information to optimize the
   *            reconfiguration process. The actual information contained in the parameter depends
   *            on the combination of topology manager and reconfigurator in use.<br>
   *            This parameter can be <code>null</code> if the topology manager does not support
   *            providing additional information, or there is no information to provide.
   */
  public void notifyNeighborAdded(NodeDescriptor addedNeighbor, Serializable reconfInfo);

  /**
   * This method is called whenever a neighbor of the local node disconnects (or is disconnected).
   * 
   * @param removedNeighbor The <code>NodeDescriptor</code> of the removed neighbor.
   */
  public void notifyNeighborRemoved(NodeDescriptor removedNeighbor);

  /**
   * This method is called whenever a neighbor of the local node dies.
   * 
   * @param deadNeighbor The <code>NodeDescriptor</code> of the no more reachable neighbor.
   * @param reconfInfo additional information about the topology change.<br>
   *            The <code>Reconfigurator</code> can use this information to optimize the
   *            reconfiguration process. The actual information contained in the parameter depends
   *            on the combination of topology manager and reconfigurator in use.<br>
   *            This parameter can be <code>null</code> if the topology manager does not support
   *            providing additional information, or there is no information to provide.
   */
  public void notifyNeighborDead(NodeDescriptor deadNeighbor, Serializable reconfInfo);
}