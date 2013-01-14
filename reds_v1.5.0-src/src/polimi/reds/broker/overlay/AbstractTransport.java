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

import java.util.*;
import java.util.logging.Logger;
import java.io.Serializable;

public abstract class AbstractTransport implements Transport {
  private Map<String, Set<DataListener>> dataListeners;
  private Set<ConnectivityChangeListener> connectivityChangeListeners;
  private List<Link> openLinks;
  private Set<String> urls;
  private volatile boolean beaconing;
  /**
   * The <code>Logger</code> used by this transport.
   * 
   * Subclasses are supposed to use this logger to log messages (e.g., warning and errors).
   */
  protected Logger logger;

  /**
   * An abstract transport providing the methods common to all transports.
   */
  protected AbstractTransport() {
    logger = Logger.getLogger("polimi.reds.broker.overlay.Transport");
    dataListeners = new HashMap<String, Set<DataListener>>();
    connectivityChangeListeners = new LinkedHashSet<ConnectivityChangeListener>();
    openLinks = new ArrayList<Link>();
    urls = new HashSet<String>();
  }

  public boolean isBeaconing() {
    return beaconing;
  }

  public void setBeaconing(boolean beaconing) {
    this.beaconing = beaconing;
  }

  public synchronized String[] getUrls() {
    return urls.toArray(new String[urls.size()]);
  }

  public Collection<Link> getOpenLinks() {
    Collection<Link> res;
    synchronized(openLinks) {
      res = new ArrayList<Link>(openLinks);
    }
    return res;
  }

  public void addDataListener(DataListener listener, String subject) {
    synchronized(dataListeners) {
      Set<DataListener> l = dataListeners.get(subject);
      if(l==null) {
        l = new LinkedHashSet<DataListener>();
        dataListeners.put(subject, l);
      }
      l.add(listener);
    }
  }

  public void addConnectivityChangeListener(ConnectivityChangeListener listener) {
    synchronized(connectivityChangeListeners) {
      connectivityChangeListeners.add(listener);
    }
  }

  public void removeDataListener(DataListener listener, String subject) {
    synchronized(dataListeners) {
      Set<DataListener> l = dataListeners.get(subject);
      if(l!=null) {
        l.remove(listener);
        if(l.isEmpty()) dataListeners.remove(subject);
      }
    }
  }

  public void removeConnectivityChangeListener(ConnectivityChangeListener listener) {
    synchronized(connectivityChangeListeners) {
      connectivityChangeListeners.remove(listener);
    }
  }

  /**
   * Add a new URL to those associated with this transport.
   * 
   * @param url the new URL associated with this transport.
   */
  protected synchronized void addUrl(String url) {
    urls.add(url);
  }

  /**
   * Adds the specified <code>Link</code> to the collection of opened links.
   * 
   * @param l the link to be added.
   */
  protected void addOpenLink(Link l) {
    synchronized(openLinks) {
      openLinks.add(l);
    }
  }

  /**
   * Removes the specified <code>Link</code> from the collection of opened links.
   * 
   * @param l the link to be removed.
   */
  protected void removeOpenLink(Link l) {
    synchronized(openLinks) {
      openLinks.remove(l);
    }
  }

  /**
   * Signals the arrival of new data to the registered listeners.
   * 
   * @param subject the subject the new data was addressed to.
   * @param source the link the new data was received from.
   * @param data the new data.
   */
  protected void notifyDataArrived(String subject, Link source, Serializable data) {
    logger.fine("Dispatching received msg \""+data+"\" addressed to sbj \""+subject+"\" through "
        +source);
    List<DataListener> copiedListeners = null;
    synchronized(dataListeners) {
      Set<DataListener> listeners = dataListeners.get(subject);
      if(listeners!=null) {
        copiedListeners = new ArrayList<DataListener>(listeners);
      }
    }
    if(copiedListeners!=null) {
      for(DataListener dl : copiedListeners) {
        dl.notifyDataArrived(subject, source, data);
      }
    }
  }

  /**
   * Signals the opening of a new link to the registered listeners.
   * 
   * @param link the new link.
   */
  protected void notifyLinkOpened(Link link) {
    List<ConnectivityChangeListener> copiedListeners = null;
    synchronized(connectivityChangeListeners) {
      copiedListeners = new ArrayList<ConnectivityChangeListener>(connectivityChangeListeners);
    }
    for(ConnectivityChangeListener l : copiedListeners) {
      l.notifyLinkOpened(link);
    }
  }

  /**
   * Signals the closing of a link to the registered listeners.
   * 
   * @param link the closed link.
   */
  protected void notifyLinkClosed(Link link) {
    List<ConnectivityChangeListener> copiedListeners = null;
    synchronized(connectivityChangeListeners) {
      copiedListeners = new ArrayList<ConnectivityChangeListener>(connectivityChangeListeners);
    }
    for(ConnectivityChangeListener l : copiedListeners) {
      l.notifyLinkClosed(link);
    }
  }

  /**
   * Signals the crash of a link to the registered listeners.
   * 
   * @param link the crashed link.
   */
  protected void notifyLinkCrashed(Link link) {
    List<ConnectivityChangeListener> copiedListeners = null;
    synchronized(connectivityChangeListeners) {
      copiedListeners = new ArrayList<ConnectivityChangeListener>(connectivityChangeListeners);
    }
    for(ConnectivityChangeListener l : copiedListeners) {
      l.notifyLinkCrashed(link);
    }
  }

  public String toString() {
    return "Transport@"+urls.toString();
  }
}
