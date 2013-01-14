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

package polimi.reds.broker.overlay;

import java.io.Serializable;
import polimi.reds.NodeDescriptor;

/**
 * Contains information about a new link that has replaced another link during a reconfiguration 
 * operation. In a tree overlay network, a link can replace another if they both connect the same 
 * sub-trees of the overlay.<br>
 * When using the Informed Link Activation reconfiguration protocol, the topology manager uses this
 * class in the NeighborDead events to inform the reconfigurator when a previously existing link 
 * between the local broker and the dead broker is replaced by a new link.<br>
 * The new link can be between two brokers different from the ones in the old link; between a 
 * broker that was in the old link and a broker that was not in the old link; or even between the 
 * same brokers as the ones in the old link.
 * 
 * @author Andrea Milani
 */
public class LinkSubstitutionInfo implements Serializable {

  private static final long serialVersionUID = 1L;
   
  private NodeDescriptor newLinkBrokerInSameSubTree;
  private NodeDescriptor newLinkBrokerInOtherSubTree;
  private ReconfigurationId reconfId;
  
  /**
   * Creates a new <code>LinkSubstitutionInfo</code>.
   * 
   * @param newLinkBrokerInSameSubTree see {@link #getNewLinkBrokerInSameSubTree()}
   * @param newLinkBrokerInOtherSubTree see {@link #getNewLinkBrokerInOtherSubTree()}
   * @param reconfId see {@link #getReconfId()}
   */
  public LinkSubstitutionInfo(NodeDescriptor newLinkBrokerInSameSubTree, 
      NodeDescriptor newLinkBrokerInOtherSubTree, ReconfigurationId reconfId) {
    this.newLinkBrokerInSameSubTree = newLinkBrokerInSameSubTree;
    this.newLinkBrokerInOtherSubTree = newLinkBrokerInOtherSubTree;
    this.reconfId = reconfId;
  }
  
  /**
   * Returns the broker which is the end of the new link in the sub-tree containing the local 
   * broker.
   * 
   * @return the broker of the new link in the same sub-tree as the local broker.
   */
  public NodeDescriptor getNewLinkBrokerInSameSubTree() {
    return newLinkBrokerInSameSubTree;
  }
  
  /**
   * Returns the broker which is the end of the new link in the sub-tree containing the old neighbor
   * of the local broker. The old neighbor is the broker which was linked to local broker in the old
   * link.
   * 
   * @return the broker of the new link in the same sub-tree as the old neighbor.
   */
  public NodeDescriptor getNewLinkBrokerInOtherSubTree() {
    return newLinkBrokerInOtherSubTree;
  }

  /**
   * Returns the <code>ReconfigurationId</code> of the reconfiguration operation.
   * 
   * @return the <code>ReconfigurationId</code> of the reconfiguration operation.
   */
  public ReconfigurationId getReconfId() {
    return reconfId;
  }
  
} // LinkSubstitutionInfo