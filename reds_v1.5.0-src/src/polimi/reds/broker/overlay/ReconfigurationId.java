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

/**
 * The ID assigned by the topology manager to a reconfiguration operation.
 * The <code>ReconfigurationId</code> can be exploited by the reconfigurator to execute advanced 
 * reconfiguration protocols.
 * 
 * @author Andrea Milani
 */
public class ReconfigurationId implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  // The reconfiguration ID.
  private String id;
  
  /**
   * Creates a new <code>ReconfigurationId</code>.
   * 
   * @param id the reconfiguration ID.
   */
  public ReconfigurationId(String id) {
    this.id = id;
  }
  
  /**
   * Returns the reconfiguration ID.
   * 
   * @return the reconfiguration ID.
   */
  public String getId() {
    return id;
  }

  /**
   * Two <code>ReconfigurationId</code>s are equal when the value returned by their {@link #getId()}
   * method is the same.
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof ReconfigurationId)) return false;
    ReconfigurationId cmpTo = (ReconfigurationId) obj;
    return id.equals(cmpTo.getId());
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
  
  @Override
  public String toString()  {
    return "RecId: " + id;
  }
  
} // ReconfigurationId