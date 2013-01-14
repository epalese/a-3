
package polimi.reds.examples;

import polimi.reds.NodeDescriptor;
import polimi.reds.broker.overlay.*;

import java.io.Serializable;
import java.util.logging.*;

public class TopologyTester {
  public static void main(String args[]) throws Exception {
    if(args.length!=3) {
      System.err.println("USAGE: java TopologyTester <accept port> <host> <port>");
      System.exit(-1);
    }
    int localPort = Integer.parseInt(args[0]);
    String host = null;
    int port = -1;
    host = args[1];
    port = Integer.parseInt(args[2]);
    // configure the logger
    Logger logger = Logger.getLogger("polimi.reds");
    ConsoleHandler ch = new ConsoleHandler();
    ch.setLevel(Level.ALL);
    logger.addHandler(ch);
    logger.setLevel(Level.ALL);
    logger.setUseParentHandlers(false);
    // start
    Transport tr = new TCPTransport(localPort);
    TopologyManager tm = new SimpleTopologyManager();
    tm.addTransport(tr);
    GenericOverlay ovr = new GenericOverlay(tm,tr);
    MyTMListener l = new MyTMListener();
    tm.addNeighborhoodChangeListener(l);
    tm.start();
    String neighborURL = "reds-tcp:"+host+":"+port;
    System.out.println("Press a key to connect to "+neighborURL);
    System.in.read();
    try {
      NodeDescriptor newNeighbor = tm.addNeighbor(neighborURL);
      if(newNeighbor!=null) System.out.println(newNeighbor+" added!!!");
    } catch(Exception ex) {
      System.out.println("Exception adding new neighbor "+neighborURL);
      ex.printStackTrace();
    }
    System.out.println("Going to sleep");
    // wait forever
    synchronized(TopologyTester.class) {
      TopologyTester.class.wait();
    }
  }
}

class MyTMListener implements NeighborhoodChangeListener {
  public void notifyNeighborAdded(NodeDescriptor addedNeighbor, Serializable reconfInfo) {
    System.out.println(addedNeighbor+" added");
  }

  public void notifyNeighborRemoved(NodeDescriptor removedNeighbor) {
    System.out.println(removedNeighbor+" removed");
  }

  public void notifyNeighborDead(NodeDescriptor deadNeighbor, Serializable reconfInfo) {
    System.out.println(deadNeighbor+" dead");
  }
}