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
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.util.*;
import polimi.reds.NodeDescriptor;

/**
 * A topology manager that does not restrict the overlay to assume any specific topology and does
 * not try to keep the overlay connected when links/brokers die. It only prohibits opening more than
 * a single link with the same neighbor.
 * 
 * @see #addNeighbor(String)
 */
public class SimpleTopologyManager extends AbstractTopologyManager {
  private static final String OPEN_SBJ = "__SIMPLETM_OPEN";
  private static final String CONFIRM_OPEN_SBJ = "__SIMPLETM_CONFIRM_OPEN";
  private static final String ALREADY_NEIGHBOR_SBJ = "__SIMPLETM_ALREADY_NEIGHBOR";
  private static final String[] subjectsToListenFor = {OPEN_SBJ, CONFIRM_OPEN_SBJ,
      ALREADY_NEIGHBOR_SBJ};
  private Map<NodeDescriptor, Link> neighbors;
  private List<PendingOpen> pendingOpens;

  public SimpleTopologyManager() {
    super();
    // the following two maps are one the inverse of the other. We keep both for performance reasons
    neighbors = new HashMap<NodeDescriptor, Link>();
    pendingOpens = new LinkedList<PendingOpen>();
  }

  /**
   * Connects to the node located at the specified URL. If everything goes right the specified node
   * becomes a new neighbor and the <code>NeighborAddedListener</code>s registered are notified
   * of the new neighbor, otherwise an exception is thrown.
   * 
   * @see #tentativelyAddNeighbor(String)
   * 
   * @param url the URL of the node to connect.
   * @throws NotRunningException if the topology manager is not running.
   * @throws MalformedURLException if the given URL has a wrong format.
   * @throws AlreadyNeighborException if the broker was already a neighbor.
   * @throws ConnectException if the connection fails.
   * @return the <code>NodeDescriptor</code> of the new neighbor.
   */
  public NodeDescriptor addNeighbor(String url) throws NotRunningException, MalformedURLException,
      AlreadyNeighborException, ConnectException {
    if(!isRunning()) throw new NotRunningException();
    Transport tr = findTransportKnowing(url);
    if(tr==null) throw new MalformedURLException("Unknown protocol");
    logger.fine("Adding neighbor "+url);
    PendingOpen pend = new PendingOpen();
    synchronized(pendingOpens) {
      pendingOpens.add(pend);
    }
    Link l = tr.openLink(url);
    pend.setLink(l);
    try {
      if(pend.getResult()==PendingOpen.ALREADY_NEIGHBOR) throw new AlreadyNeighborException(pend
          .getNewNeighbor());
      else if(pend.getResult()==PendingOpen.CONNECTION_ERROR) throw new ConnectException();
      else return pend.getNewNeighbor();
    } catch(InterruptedException ex) {
      logger.warning(ex+" waiting for node "+url+" to be added as a neighbor");
      throw new ConnectException();
    } finally {
      synchronized(pendingOpens) {
        pendingOpens.remove(pend);
      }
    }
    // the notifyLinkOpened is called by the tr.openLink and it adds the node to the neighbors map
    // and notifies the listeners
  }

  public void tentativelyAddNeighbor(String url) throws NotRunningException, MalformedURLException {
    if(!isRunning()) throw new NotRunningException();
    Transport tr = findTransportKnowing(url);
    if(tr==null) throw new MalformedURLException("Unknown protocol");
    logger.fine("Tentatively adding neighbor "+url);
    try {
      tr.openLink(url);
    } catch(ConnectException ex) {
      logger.warning(ex+" tentatively adding neighbor "+url);
    }
    // the notifyLinkOpened is called by the tr.openLink and it adds the node to the neighbors map
    // and notifies the listeners
  }

  // Utility method to find the transport that knows the url's protocol
  private Transport findTransportKnowing(String url) {
    Transport transport = null;
    String protocol = url.split(":")[0];
    for(Transport tr : getTransports()) {
      if(tr.knowsProtocol(protocol)) {
        transport = tr;
        break;
      }
    }
    return transport;
  }

  public void removeNeighbor(NodeDescriptor node) {
    Link l;
    synchronized(neighbors) {
      l = neighbors.get(node);
    }
    if(l==null) return; // TODO return an ad-hoc exception?
    logger.info("Removing neighbor "+node);
    l.close();
    // the notifyLinkClosed is called by the l.close and it removes the node from the neighbors map
    // and notifies the listeners
  }

  public Collection<NodeDescriptor> getNeighbors() {
    List<NodeDescriptor> res = null;
    synchronized(neighbors) {
      res = new ArrayList<NodeDescriptor>(neighbors.keySet());
    }
    return res;
  }

  public Collection<NodeDescriptor> getAllNeighborsExcept(NodeDescriptor excludedNeighbor) {
    Collection<NodeDescriptor> res = getNeighbors();
    res.remove(excludedNeighbor);
    return res;
  }

  public boolean isNeighborOf(NodeDescriptor neighbor) {
    boolean res;
    synchronized(neighbors) {
      res = neighbors.keySet().contains(neighbor);
    }
    return res;
  }

  public Link getLinkFor(NodeDescriptor neighbor) {
    Link res;
    synchronized(neighbors) {
      res = neighbors.get(neighbor);
    }
    return res;
  }

  public NodeDescriptor getNeighborFor(Link link) {
    NodeDescriptor node = null;
    synchronized(neighbors) {
      for(Map.Entry<NodeDescriptor, Link> e : neighbors.entrySet()) {
        if(e.getValue().equals(link)) {
          node = e.getKey();
          break;
        }
      }
    }
    return node;
  }

  public int getNumberOfNeighbors() {
    int res = 0;
    synchronized(neighbors) {
      res = neighbors.size();
    }
    return res;
  }

  public int getNumberOfBrokers() {
    int res = 0;
    synchronized(neighbors) {
      for(NodeDescriptor n : neighbors.keySet()) {
        if(n.isBroker()) res++;
      }
    }
    return res;
  }

  public int getNumberOfClients() {
    int res = 0;
    synchronized(neighbors) {
      for(NodeDescriptor n : neighbors.keySet()) {
        if(n.isClient()) res++;
      }
    }
    return res;
  }

  public void notifyLinkOpened(Link link) {
    // send my node descriptor
    try {
      link.send(OPEN_SBJ, getNodeDescriptor());
    } catch(Exception ex) {
      logger.warning(ex+" while sending my node descriptor to the new neighbor. Closing");
      link.close();
    }
  }

  public void notifyLinkClosed(Link link) {
    NodeDescriptor node = null;
    synchronized(neighbors) {
      for(Map.Entry<NodeDescriptor, Link> e : neighbors.entrySet()) {
        if(e.getValue().equals(link)) {
          node = e.getKey();
          break;
        }
      }
      if(node==null) return; // a link not yet accepted was closed
      logger.info("Removing disconnected neighbor "+node);
      neighbors.remove(node);
    }
    notifyNeighborRemoved(node);
  }

  public void notifyLinkCrashed(Link link) {
    NodeDescriptor node = null;
    synchronized(neighbors) {
      for(Map.Entry<NodeDescriptor, Link> e : neighbors.entrySet()) {
        if(e.getValue().equals(link)) {
          node = e.getKey();
          break;
        }
      }
      if(node==null) return; // a link not yet accepted died
      logger.info("Removing crashed neighbor "+node+" ");
      neighbors.remove(node);
    }
    notifyNeighborDead(node, null);
  }

  public void notifyDataArrived(String subject, Link source, Serializable data) {
    NodeDescriptor self = getNodeDescriptor();
    NodeDescriptor newNode = (NodeDescriptor) data;
    // see if there is a pending open request associated with this link
    PendingOpen pendingOpen = null;
    for(PendingOpen p : pendingOpens) {
      try {
        if(p.getLink()==source) {
          pendingOpen = p;
          break;
        }
      } catch(InterruptedException ex) {
        logger.warning(ex+" checking if pending open for "+source+" exists");
        if(p!=null) {
          p.setResult(PendingOpen.CONNECTION_ERROR);
          synchronized(pendingOpens) {
            pendingOpens.remove(p); // remove potential garbage
          }
        }
      }
    }
    if(subject.equals(OPEN_SBJ)) {
      // check if a link was already opened with the new node
      Link oldLink = neighbors.get(newNode);
      if(oldLink!=null) {
        // A link with the new node was already active: first update URLs...
        synchronized(neighbors) {
          neighbors.remove(newNode);
          neighbors.put(newNode, oldLink);
        }
      }
      // ...then check if you are the node with the higher id
      if(self.compareTo(newNode)>0) {
        // I am the node with the higher id. I choose!
        if(oldLink!=null) {
          // A link with the same node was already active: send appropriate msg, close the link, and
          // inform waiting thread
          try {
            source.send(ALREADY_NEIGHBOR_SBJ, self);
          } catch(Exception ex) {
            logger.warning(ex+" informing other partner we are already neighbor");
          }
          source.close();
          if(pendingOpen!=null) {
            pendingOpen.setNewNeighbor(newNode);
            pendingOpen.setResult(PendingOpen.ALREADY_NEIGHBOR);
          }
        } else {
          // This is the first time I hear this node: confirm, add the new neighbor, and notify
          // interested listeners
          try {
            source.send(CONFIRM_OPEN_SBJ, self);
          } catch(Exception ex) {
            logger.warning(ex+" confirming open request. Closing");
            source.close();
            if(pendingOpen!=null) pendingOpen.setResult(PendingOpen.CONNECTION_ERROR);
            return;
          }
          logger.info("Adding neighbor "+newNode);
          synchronized(neighbors) {
            neighbors.put(newNode, source);
          }
          notifyNeighborAdded(newNode, null);
          if(pendingOpen!=null) {
            pendingOpen.setNewNeighbor(newNode);
            pendingOpen.setResult(PendingOpen.CONNECTION_ACCEPTED);
          }
        }
      } else {
        // I am the node with the lower id: nothing to do
      }
    } else if(subject.equals(CONFIRM_OPEN_SBJ)) {
      logger.info("Adding neighbor "+newNode);
      synchronized(neighbors) {
        neighbors.put(newNode, source);
      }
      notifyNeighborAdded(newNode, null);
      if(pendingOpen!=null) {
        pendingOpen.setNewNeighbor(newNode);
        pendingOpen.setResult(PendingOpen.CONNECTION_ACCEPTED);
      }
    } else if(subject.equals(ALREADY_NEIGHBOR_SBJ)) {
      // close link and inform waiting thread
      source.close();
      if(pendingOpen!=null) {
        pendingOpen.setNewNeighbor(newNode);
        pendingOpen.setResult(PendingOpen.ALREADY_NEIGHBOR);
      }
    }
  }

  @Override
  protected String[] getSubjectsToListenFor() {
    return subjectsToListenFor;
  }

  /**
   * An object used to synchronize the caller of the
   * {@link SimpleTopologyManager#addNeighbor(String)} method and the thread managing the protocol
   * between the local node and the new neighbor to decide if the new connection can be opened.
   * 
   * <p>
   * The caller sets the new link and wait for the status to be set. The thread managing the
   * protocol to open a new connection uses the link to identify the <code>PendingOpen</code>
   * request and sets the status (and the node descriptor of the new neighbor if the protocol ends
   * succesfully) when the protocol ends.
   * 
   * @see SimpleTopologyManager#addNeighbor(String)
   * @see SimpleTopologyManager#tentativelyAddNeighbor(String)
   */
  private static class PendingOpen {
    static final int WAITING = 0;
    static final int CONNECTION_ACCEPTED = 1;
    static final int CONNECTION_ERROR = 2;
    static final int ALREADY_NEIGHBOR = 3;
    private Link newLink;
    private NodeDescriptor newNeighbor;
    private int status;

    PendingOpen() {
      newLink = null;
      newNeighbor = null;
      status = WAITING;
    }

    synchronized int getResult() throws InterruptedException {
      while(status==WAITING)
        wait();
      return status;
    }

    synchronized void setResult(int status) {
      this.status = status;
      notifyAll();
    }

    synchronized void setNewNeighbor(NodeDescriptor node) {
      newNeighbor = node;
      notifyAll();
    }

    synchronized NodeDescriptor getNewNeighbor() throws InterruptedException {
      while(newNeighbor==null)
        wait();
      return newNeighbor;
    }

    synchronized void setLink(Link l) {
      newLink = l;
      notifyAll();
    }

    synchronized Link getLink() throws InterruptedException {
      while(newLink==null)
        wait();
      return newLink;
    }
  }
}
