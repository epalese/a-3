
package polimi.reds.examples;

import java.io.Serializable;
import java.util.*;
import java.util.logging.*;
import polimi.reds.*;
import polimi.reds.broker.overlay.*;

public class OverlayTester {
  private static void err() {
    System.err
        .println("USAGE: java OverlayTester <accept port> [-conn <url> ...] [-send <numPackets>]");
    System.exit(-1);
  }

  public static void main(String[] args) throws Exception {
    int localPort;
    List<String> urls = new ArrayList<String>();
    int numPackets = 0;
    // parse the command line
    if(args.length<1) err();
    localPort = Integer.parseInt(args[0]);
    for(int i = 1; i<args.length;) {
      if(args[i].equals("-conn")) {
        i++;
        while(i<args.length && !args[i].startsWith("-")) {
          urls.add(args[i++]);
        }
      } else if(args[i].equals("-send")) {
        i++;
        numPackets = Integer.parseInt(args[i++]);
      } else err();
    }
    // configure the logger
    Logger logger = Logger.getLogger("polimi.reds");
    ConsoleHandler ch = new ConsoleHandler();
    ch.setLevel(Level.CONFIG);
    logger.addHandler(ch);
    logger.setLevel(Level.CONFIG);
    logger.setUseParentHandlers(false);
    // build the overlay
    Transport tr = new TCPTransport(localPort);
    TopologyManager tm = new SimpleTopologyManager();
    GenericOverlay ovr = new GenericOverlay(tm, tr);
    // adds the listeners
    NeighborhoodChangeLogger ncl = new NeighborhoodChangeLogger();
    PacketLogger pl = new PacketLogger();
    PacketForwarder pf = new PacketForwarder(ovr);
    ovr.addNeighborhoodChangeListener(ncl);
    ovr.addPacketListener(pl, "OverlayTester");
    ovr.addPacketListener(pf, "OverlayTester");
    ovr.start();
    for(String url : urls) {
      System.out.println("Press [enter] to connect to "+url);
      System.in.read();
      NodeDescriptor newNeighbor = ovr.addNeighbor(url);
      System.out.println(newNeighbor+" added!!!");
    }
    if(numPackets>0) {
      for(int i = 0; i<numPackets; i++) {
        System.out.println("Sending message "+(i+1));
        for(NodeDescriptor n : ovr.getNeighbors()) {
          ovr.send("OverlayTester", "Message "+i, n);
        }
        Thread.sleep(1000);
      }
    }
    System.out.println("Going to sleep");
    // wait forever
    synchronized(TopologyTester.class) {
      TopologyTester.class.wait();
    }
  }

  private static class NeighborhoodChangeLogger implements NeighborhoodChangeListener {
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

  private static class PacketLogger implements PacketListener {
    public void notifyPacketArrived(String subject, NodeDescriptor source, Serializable packet) {
      System.out.println("Received packet: "+packet+" from: "+source+" sbj: "+subject);
    }
  }
  private static class PacketForwarder implements PacketListener {
    private Overlay ovr;
    public PacketForwarder(Overlay ovr) {
      this.ovr = ovr;
    }
    public void notifyPacketArrived(String subject, NodeDescriptor source, Serializable packet) {
      System.out.print("Forwarding packet: "+packet+" from: "+source+" to: ");
      for(NodeDescriptor n : ovr.getAllNeighborsExcept(source)) {
        try {
          System.out.print(n+" ");
          ovr.send(subject, packet, n);
        } catch(Exception ex) {
          ex.printStackTrace();
        }
      }
      System.out.print("\n");
    }
  }
}
