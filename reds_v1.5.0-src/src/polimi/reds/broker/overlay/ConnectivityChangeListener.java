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

/**
 * Components implementing this interface may listen for changes in the connectivity managed by some
 * <code>Transport</code>, i.e., adding/removing/crashing of links.
 * 
 * @see polimi.reds.broker.overlay.Transport
 */
public interface ConnectivityChangeListener {
  /**
   * This method is called whenever a link is opened for a new neighbor of the local node.
   * 
   * @param link the new link.
   */
  public void notifyLinkOpened(Link link);

  /**
   * This method is called whenever a link is closed.
   * 
   * @param link the closed link.
   */
  public void notifyLinkClosed(Link link);
  
  /**
   * This method is called whenever a link crashes.
   * 
   * @param link the crashed link.
   */
  public void notifyLinkCrashed(Link link);
}
