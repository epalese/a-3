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
import java.util.*;
import java.util.logging.Logger;
import polimi.reds.NodeDescriptor;

/**
 * An abstract topology manager providing the methods common to all topology managers. When a new
 * transport is added, this abstract topology manager adds itself as a listener for all the events
 * (adding/removing/crashing of links + new data available) notified by the tranport. Similarly,
 * when the transport is removed, this abstract topology manager unregisters.
 */
public abstract class AbstractTopologyManager implements TopologyManager, DataListener {
  private volatile boolean running;
  private Set<NeighborhoodChangeListener> neighborhoodChangeListeners;
  private List<Transport> transports;
  private NodeDescriptor myND;
  /**
   * The <code>Logger</code> used by this transport.
   * 
   * Subclasses are supposed to use this logger to log messages (e.g., warning and errors).
   */
  protected Logger logger;

  protected AbstractTopologyManager() {
    logger = Logger.getLogger("polimi.reds.broker.overlay.TopologyManager");
    running = false;
    neighborhoodChangeListeners = new LinkedHashSet<NeighborhoodChangeListener>();
    transports = new ArrayList<Transport>();
    myND = null;
  }

  public void setNodeDescriptor(NodeDescriptor nd) {
    myND = nd;
  }

  public void start() {
    if(running) return;
    logger.fine("Starting "+this);
    running = true;
    synchronized(transports) {
      for(Transport tr : transports) {
        tr.addConnectivityChangeListener(this);
        for(String sbj : this.getSubjectsToListenFor()) {
          tr.addDataListener(this, sbj);
        }
      }
    }
  }

  public void stop() {
    if(!running) return;
    logger.fine("Stopping "+this);
    synchronized(transports) {
      for(Transport tr : transports) {
        tr.removeConnectivityChangeListener(this);
        for(String sbj : this.getSubjectsToListenFor()) {
          tr.removeDataListener(this, sbj);
        }
      }
    }
    running = false;
  }

  public boolean isRunning() {
    return running;
  }

  public void addNeighborhoodChangeListener(NeighborhoodChangeListener listener) {
    synchronized(neighborhoodChangeListeners) {
      neighborhoodChangeListeners.add(listener);
    }
  }

  public void removeNeighborhoodChangeListener(NeighborhoodChangeListener listener) {
    synchronized(neighborhoodChangeListeners) {
      neighborhoodChangeListeners.remove(listener);
    }
  }

  public void addTransport(Transport tr) {
    synchronized(transports) {
      transports.add(tr);
      if(running) {
        tr.addConnectivityChangeListener(this);
        for(String sbj : this.getSubjectsToListenFor()) {
          tr.addDataListener(this, sbj);
        }
      }
    }
  }

  public void removeTransport(Transport tr) {
    synchronized(transports) {
      if(running) {
        tr.removeConnectivityChangeListener(this);
        for(String sbj : this.getSubjectsToListenFor()) {
          tr.removeDataListener(this, sbj);
        }
      }
      transports.remove(tr);
    }
  }

  public Collection<Transport> getTransports() {
    Collection<Transport> res;
    synchronized(transports) {
      res = new ArrayList<Transport>(transports);
    }
    return res;
  }

  protected NodeDescriptor getNodeDescriptor() {
    return myND;
  }

  protected void notifyNeighborAdded(NodeDescriptor node, Serializable reconfInfo) {
    List<NeighborhoodChangeListener> copiedListeners = null;
    synchronized(neighborhoodChangeListeners) {
      copiedListeners = new ArrayList<NeighborhoodChangeListener>(neighborhoodChangeListeners);
    }
    for(NeighborhoodChangeListener l : copiedListeners) {
      l.notifyNeighborAdded(node, reconfInfo);
    }
  }

  protected void notifyNeighborRemoved(NodeDescriptor node) {
    List<NeighborhoodChangeListener> copiedListeners = null;
    synchronized(neighborhoodChangeListeners) {
      copiedListeners = new ArrayList<NeighborhoodChangeListener>(neighborhoodChangeListeners);
    }
    for(NeighborhoodChangeListener l : copiedListeners) {
      l.notifyNeighborRemoved(node);
    }
  }

  protected void notifyNeighborDead(NodeDescriptor node, Serializable reconfInfo) {
    List<NeighborhoodChangeListener> copiedListeners = null;
    synchronized(neighborhoodChangeListeners) {
      copiedListeners = new ArrayList<NeighborhoodChangeListener>(neighborhoodChangeListeners);
    }
    for(NeighborhoodChangeListener l : copiedListeners) {
      l.notifyNeighborDead(node, reconfInfo);
    }
  }

  /**
   * Subclasses must implement this method to inform this abstract topology manager about the set of
   * subjects of data it is interested to receive from the transports connected.
   * 
   * @return the set of subjects to listen for (from transports connected to this topology manager).
   */
  protected abstract String[] getSubjectsToListenFor();
}
