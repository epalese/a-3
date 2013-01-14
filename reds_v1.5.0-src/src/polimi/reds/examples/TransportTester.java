
package polimi.reds.examples;

import java.io.Serializable;
import polimi.reds.broker.overlay.*;
import java.util.logging.*;

public class TransportTester {
  public static void main(String[] args) throws Exception {
    if(args.length==0) {
      System.err.println("USAGE: java TransportTester <accept port> [<host> <port>]");
      System.exit(-1);
    }
    int localPort = Integer.parseInt(args[0]);
    String host = null;
    int port = -1;
    if(args.length>1) {
      host = args[1];
      port = Integer.parseInt(args[2]);
    }
    // configure the logger
    Logger logger = Logger.getLogger("polimi.reds");
    ConsoleHandler ch = new ConsoleHandler();
    ch.setLevel(Level.ALL);
    logger.addHandler(ch);
    logger.setLevel(Level.ALL);
    logger.setUseParentHandlers(false);
    // start
    System.out.println("Creating the TCPTransport");
    Transport tr = new TCPTransport(localPort);
    tr.setBeaconing(false);
    System.out.println("Adding listeners");
    tr.addDataListener(new MyDataListener(), "sbj1");
    MyLinkListener linkListener = new MyLinkListener();
    tr.addConnectivityChangeListener(linkListener);
    System.out.println("Starting the TCPTransport");
    tr.start();
    if(host!=null) {
      System.out.println("Opening new link");
      Link l = tr.openLink("reds-tcp:"+host+":"+port);
      System.out.println("Sending data");
      for(int i = 0; i<10; i++) {
        if(i%2==0) l.send("sbj1", i);
        else l.send("sbj2", i);
        // Thread.sleep(400);
      }
      System.out.println("Sleeping for five seconds");
      Thread.sleep(5000);
      System.out.println("Sending more data");
      for(int i = 0; i<10; i++) {
        if(i%2==0) l.send("sbj1", i);
        else l.send("sbj2", i);
        // Thread.sleep(400);
      }
      System.out.println("Closing link");
      l.close();
      System.out.println("Waiting forever");
      synchronized(TransportTester.class) {
        TransportTester.class.wait();
      }
    } else {
      System.out.println("Sleeping for three second");
      Thread.sleep(3000);
      System.out.println("Sending data");
      Link l = tr.getOpenLinks().iterator().next();
      for(int i = 0; i<10; i++) {
        if(i%2==0) l.send("sbj1", i);
        else l.send("sbj2", i);
        // Thread.sleep(400);
      }
      System.out.println("Closing link");
      l.close();
      System.out.println("Waiting forever");
      
      synchronized(TransportTester.class) {
        TransportTester.class.wait();
      }
    }
  }
}

class MyDataListener implements DataListener {
  public void notifyDataArrived(String subject, Link source, Serializable data) {
    System.out.println("Data "+data+" arrived for sbj "+subject+" through link "+source);
  }
}

class MyLinkListener implements ConnectivityChangeListener {
  public void notifyLinkOpened(Link link) {
    System.out.println("Link "+link+" opened");
  }

  public void notifyLinkClosed(Link link) {
    System.out.println("Link "+link+" closed");
  }

  public void notifyLinkCrashed(Link link) {
    System.out.println("Link "+link+" dead");
  }
}