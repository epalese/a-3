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
import java.io.Serializable;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.logging.Logger;
import polimi.reds.NodeDescriptor;
import polimi.util.BoundedQueue;

/**
 * This class implements the {@link Overlay} interface by delegating most of its operations to a
 * {@link TopologyManager} and to one or more {@link Transport}s. Its role is to decouple the
 * transport from the upper layers by enqueuing incoming packets and delivering them to the upper
 * layer asynchronously.
 */
public class GenericOverlay implements Overlay, Runnable, DataListener {
  private volatile boolean running;
  private Thread dispatchingThread;
  private NodeDescriptor myND;
  private TopologyManager topoMgr;
  private List<TrafficClass> trafficClasses; // the list of known traffic classes
  private Map<String, TrafficClass> sbjToTC; // maps subjects to traffic classes
  private Map<String, Set<PacketListener>> packetListeners; // maps subjects to PacketListeners
  private Logger logger;

  public GenericOverlay(TopologyManager tm, Transport tr) {
    logger = Logger.getLogger("polimi.reds.broker.overlay.TopologyManager");
    // builds my node descriptor. I am a broker.
    myND = new NodeDescriptor(true);
    // initialize other fields
    running = false;
    dispatchingThread = null;
    trafficClasses = new ArrayList<TrafficClass>();
    trafficClasses.add(new TrafficClass(DEFAULT_TRAFFIC_CLASS));
    sbjToTC = new HashMap<String, TrafficClass>();
    packetListeners = new HashMap<String, Set<PacketListener>>();
    topoMgr = tm;
    tm.setNodeDescriptor(myND);
    addTransport(tr);
  }

  /**
   * Associates a new transport to this overlay.
   * 
   * @param tr the transport to add.
   */
  public void addTransport(Transport tr) {
    if(topoMgr.getTransports().contains(tr)) return;
    logger.config("Adding transport "+tr+" to "+myND);
    topoMgr.addTransport(tr);
    for(String url : tr.getUrls())
      myND.addUrl(url);
    synchronized(packetListeners) {
      for(String sbj : packetListeners.keySet()) {
        tr.addDataListener(this, sbj);
      }
    }
    if(running) try {
      tr.start();
    } catch(IOException ex) {
      logger.severe(ex+" starting "+tr);
      ex.printStackTrace();
    }
  }

  /**
   * Removes the given transport from the collection of transports used by this overlay.
   * 
   * @param tr the transport to remove.
   */
  public void removeTransport(Transport tr) {
    if(!topoMgr.getTransports().contains(tr)) return;
    logger.config("Removing transport "+tr+" from "+myND);
    topoMgr.removeTransport(tr);
    for(String url : tr.getUrls())
      myND.removeUrl(url);
  }

  /**
   * Gets the collection of transports used by this overlay.
   * 
   * @return the collection of transports used by this overlay.
   */
  public Collection<Transport> getTransports() {
    return topoMgr.getTransports();
  }

  /**
   * Starts the overlay, also starting the topology manager and transports that builds this overlay.
   * 
   * @see #stop()
   */
  public void start() {
    if(running) return;
    logger.fine("Starting "+this);
    running = true;
    topoMgr.start();
    for(Transport tr : topoMgr.getTransports()) {
      try {
        tr.start();
      } catch(IOException ex) {
        logger.severe(ex+" starting "+tr);
        ex.printStackTrace();
      }
    }
    dispatchingThread = new Thread(this);
    dispatchingThread.start();
  }

  /**
   * Stops the overlay, also stopping the topology manager and transports that builds this overlay.
   * 
   * @see #start()
   */
  public void stop() {
    if(!running) return;
    logger.fine("Stopping "+this);
    for(Transport tr : topoMgr.getTransports()) {
      try {
        tr.stop();
      } catch(IOException ex) {
        logger.severe(ex+" stopping "+tr);
        ex.printStackTrace();
      }
    }
    topoMgr.stop();
    running = false;
    // wake up the dispatchingThread
    synchronized(trafficClasses) {
      trafficClasses.notifyAll();
    }
    // wait for the dispatchignThread to finish
    try {
      dispatchingThread.join();
    } catch(InterruptedException ex) {
      logger.warning(ex+" waiting for the dispatchingThread to finish");
    }
  }

  public boolean isRunning() {
    return running;
  }

  public NodeDescriptor getNodeDescriptor() {
    return myND;
  }

  public NodeDescriptor addNeighbor(String url) throws NotRunningException, MalformedURLException,
      AlreadyNeighborException, ConnectException {
    return topoMgr.addNeighbor(url);
  }

  public void tentativelyAddNeighbor(String url) throws NotRunningException, MalformedURLException {
    topoMgr.tentativelyAddNeighbor(url);
  }

  public void removeNeighbor(NodeDescriptor node) {
    topoMgr.removeNeighbor(node);
  }

  public void addNeighborhoodChangeListener(NeighborhoodChangeListener listener) {
    topoMgr.addNeighborhoodChangeListener(listener);
  }

  public void removeNeighborhoodChangeListener(NeighborhoodChangeListener listener) {
    topoMgr.removeNeighborhoodChangeListener(listener);
  }

  public Collection<NodeDescriptor> getNeighbors() {
    return topoMgr.getNeighbors();
  }

  public Collection<NodeDescriptor> getAllNeighborsExcept(NodeDescriptor excludedNeighbor) {
    return topoMgr.getAllNeighborsExcept(excludedNeighbor);
  }

  public boolean isNeighborOf(NodeDescriptor neighbor) {
    return topoMgr.isNeighborOf(neighbor);
  }

  public int getNumberOfNeighbors() {
    return topoMgr.getNumberOfNeighbors();
  }

  public int getNumberOfBrokers() {
    return topoMgr.getNumberOfBrokers();
  }

  public int getNumberOfClients() {
    return topoMgr.getNumberOfClients();
  }

  public Collection<String> getTrafficClasses() {
    Collection<String> res = new ArrayList<String>();
    synchronized(trafficClasses) {
      for(TrafficClass tc : trafficClasses) {
        res.add(tc.name);
      }
    }
    return res;
  }

  public void setTrafficClass(String subject, String trafficClass) {
    TrafficClass tcToAdd = null;
    // check if the given traffic class exists
    synchronized(trafficClasses) {
      for(TrafficClass tc : trafficClasses) {
        if(tc.name.equals(trafficClass)) {
          tcToAdd = tc;
          break;
        }
      }
      // create and add it to the list of traffic classes if necessary
      if(tcToAdd==null) {
        tcToAdd = new TrafficClass(trafficClass);
        trafficClasses.add(tcToAdd);
      }
    }
    // associate the given subject with the traffic class found/created
    synchronized(sbjToTC) {
      sbjToTC.put(subject, tcToAdd);
    }
  }

  public void addTrafficClass(String trafficClass, int queueSize) throws IllegalArgumentException {
    // check if the given traffic class exists
    synchronized(trafficClasses) {
      for(TrafficClass tc : trafficClasses) {
        if(tc.name.equals(trafficClass))
          throw new IllegalArgumentException("Traffic class "+trafficClass+" already exists");
      }
      trafficClasses.add(new TrafficClass(trafficClass, queueSize));
    }
  }

  public void send(String subject, Serializable packet, NodeDescriptor recipient)
      throws IOException, NotConnectedException, NotRunningException {
    if(!running) throw new NotRunningException();
    Link l = topoMgr.getLinkFor(recipient);
    if(l==null) throw new NotConnectedException();
    l.send(subject, packet);
  }

  public void addPacketListener(PacketListener listener, String subject) {
    synchronized(packetListeners) {
      Set<PacketListener> l = packetListeners.get(subject);
      if(l==null) { // this is the first PacketListener registering for this subject
        // create a new list of PacketListeners for this subject
        l = new LinkedHashSet<PacketListener>();
        packetListeners.put(subject, l);
        // register myself as a DataListener for this subject to every transport
        for(Transport tr : topoMgr.getTransports()) {
          tr.addDataListener(this, subject);
        }
        if(sbjToTC.get(subject)==null) {
          // This subject has not been associated with a trafficClass before
          setTrafficClass(subject, DEFAULT_TRAFFIC_CLASS);
        }
      }
      l.add(listener);
    }
  }

  public void removePacketListener(PacketListener listener, String subject) {
    synchronized(packetListeners) {
      Set<PacketListener> l = packetListeners.get(subject);
      if(l!=null) {
        l.remove(listener);
        if(l.isEmpty()) {
          packetListeners.remove(subject);
          for(Transport tr : topoMgr.getTransports()) {
            tr.removeDataListener(this, subject);
          }
        }
      }
    }
  }

  public void notifyDataArrived(String subject, Link source, Serializable data) {
    TrafficClass tc = null;
    synchronized(sbjToTC) {
      tc = sbjToTC.get(subject);
    }
    if(tc==null) return; // skip packets addressed to unknown subjects
    NodeDescriptor nd = topoMgr.getNeighborFor(source);
    if(nd==null) return; // skip packets coming from neighbors who disconnected in the meanwhile
    synchronized(trafficClasses) {
      if(tc.queue.offer(new ReceivedPacket(subject, nd, data))) trafficClasses.notifyAll();
      // else the queue was full and the packet was discarded
    }
  }

  public void run() {
    List<ReceivedPacket> lp = new ArrayList<ReceivedPacket>();
    while(running) {
      lp.clear();
      synchronized(trafficClasses) {
        for(TrafficClass tc : trafficClasses) {
          if(tc.queue.isEmpty()) continue;
          else {
            lp.add(tc.queue.remove());
          }
        }
        if(lp.isEmpty()) try {
          trafficClasses.wait();
          continue;
        } catch(InterruptedException ex) {
          logger.warning(ex+" waiting for new packet to arrive");
        }
      }
      // Must release the trafficClasses lock before notifying. Indeed, the notified components
      // could need to wait for new data arriving, which cannot arrive if the trafficClasses lock is
      // not available.
      for(ReceivedPacket p : lp) {
        notifyPacketArrived(p.subject, p.source, p.data);
      }
    }
  }

  private void notifyPacketArrived(String subject, NodeDescriptor source, Serializable packet) {
    List<PacketListener> copiedListeners = null;
    synchronized(packetListeners) {
      Set<PacketListener> l = packetListeners.get(subject);
      if(l!=null) copiedListeners = new ArrayList<PacketListener>(l);
    }
    if(copiedListeners!=null) {
      for(PacketListener l : copiedListeners) {
        l.notifyPacketArrived(subject, source, packet);
      }
    }
  }

  private static class TrafficClass {
    String name;
    Queue<ReceivedPacket> queue;

    /**
     * Creates a new traffic class with the given name and a unbounded queue.
     * 
     * @param name the name of the traffic class.
     */
    TrafficClass(String name) {
      this(name, -1);
    }

    /**
     * Creates a new traffic class with the given name and queue size.
     * 
     * @param name the name of the traffic class.
     * @param queueSize the size of the queue.
     */
    TrafficClass(String name, int queueSize) {
      this.name = name;
      if(queueSize<0) queue = new LinkedList<ReceivedPacket>();
      else queue = new BoundedQueue<ReceivedPacket>(queueSize);
    }
  }

  private static class ReceivedPacket {
    String subject;
    NodeDescriptor source;
    Serializable data;

    ReceivedPacket(String subject, NodeDescriptor source, Serializable data) {
      this.subject = subject;
      this.source = source;
      this.data = data;
    }
  }
}
