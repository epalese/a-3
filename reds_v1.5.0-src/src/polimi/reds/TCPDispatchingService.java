/*
 * Created on Jul 26, 2007
 *
 */

package polimi.reds;

import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Logger;

import polimi.reds.broker.overlay.DataListener;
import polimi.reds.broker.overlay.Link;
import polimi.reds.broker.overlay.NeighborhoodChangeListener;
import polimi.reds.broker.overlay.NotConnectedException;
import polimi.reds.broker.overlay.NotRunningException;
import polimi.reds.broker.overlay.SimpleTopologyManager;
import polimi.reds.broker.overlay.TCPTransport;
import polimi.reds.broker.routing.Router;

public class TCPDispatchingService implements DispatchingService, DataListener,
    NeighborhoodChangeListener {
  private class ClientReplyManager extends Thread {
    private static final int GARBAGE_COLLECTOR_TIMER = 5000;

    private class Row {
      LinkedList<Reply> replies;
      long timeout;
      boolean timeoutElapsed;
      boolean last;

      private Row() {
        replies = new LinkedList<Reply>();
        timeout = System.currentTimeMillis()+TIMEOUT;
        timeoutElapsed = false;
        last = false;
      }

      private synchronized void recordReply(Reply r) {
        replies.add(r);
        if(r.isLast()) last = true;
        notifyAll();
      }

      private int numReplies() {
        return replies.size();
      }

      private synchronized void checkIfElapsed(long time) {
        if(time>timeout) {
          timeoutElapsed = true;
          notifyAll();
        }
      }

      private synchronized Reply getNextReply() throws ReplyTimeoutException {
        while(replies.size()==0&&timeoutElapsed==false&&last==false)
          try {
            wait();
          } catch(InterruptedException e) {
            e.printStackTrace();
          }
        if(replies.size()>0) return replies.removeFirst();
        else { // (replies.size()==0)
          if(last) return null;
          else throw new ReplyTimeoutException();
        }
      }
    }

    HashMap<MessageID, Row> clientReplyTable = new HashMap<MessageID, Row>();

    public ClientReplyManager() {
      this.setDaemon(true);
    }

    private void recordReply(Reply reply) {
      MessageID repliableMessageID = reply.getRepliableMessageID();
      synchronized(clientReplyTable) {
        if(clientReplyTable.containsKey(repliableMessageID)) {
          clientReplyTable.get(repliableMessageID).recordReply(reply);
        } else TCPDispatchingService.this.l
            .warning("Reply arrived for unknown message (or timeout elapsed) : "+reply);
      }
    }

    private void recordMessage(Message msg) {
      synchronized(clientReplyTable) {
        clientReplyTable.put(msg.getID(), new Row());
      }
    }

    private Message getNextReply(MessageID repliableMessageID) throws ReplyTimeoutException {
      Row replyRow;
      synchronized(clientReplyTable) {
        if(!clientReplyTable.containsKey(repliableMessageID)) return null;
        replyRow = clientReplyTable.get(repliableMessageID);
      }
      Reply reply;
      try {
        reply = replyRow.getNextReply();
      } catch(ReplyTimeoutException e) {
        l.finer("Timeout for "+repliableMessageID+" elapsed, removing entry in clientReplyTable.");
        clientReplyTable.remove(repliableMessageID);
        throw e;
      }
      if(reply.isLast()) {
        synchronized(clientReplyTable) {
          if(clientReplyTable.containsKey(repliableMessageID))
            clientReplyTable.remove(repliableMessageID);
        }
      }
      return reply.getPayload();
    }

    private boolean hasMoreReplies(MessageID repliableMessageID) {
      synchronized(clientReplyTable) {
        return (clientReplyTable.containsKey(repliableMessageID)&&clientReplyTable.get(
            repliableMessageID).numReplies()>0);
      }
    }

    public void run() {
      while(true) {
        long time = System.currentTimeMillis();
        // TCPDispatchingService.this.l.finer("Checking timeouts");
        synchronized(clientReplyTable) {
          for(Row repliesToMessage : clientReplyTable.values()) {
            repliesToMessage.checkIfElapsed(time);
          }
        }
        try {
          Thread.sleep(GARBAGE_COLLECTOR_TIMER);
        } catch(InterruptedException e) {
          TCPDispatchingService.this.l.warning("Reply GarbageCollector interrupted, restarting");
        }
      }
    }
  }

  Logger l = Logger.getLogger("polimi.reds.client");
  private SimpleTopologyManager topologyMgr;
  private TCPTransport transport;
  String url;
  private NodeDescriptor myND;
  private NodeDescriptor brokerND;
  private Link link;
  private LinkedList<Message> messages = new LinkedList<Message>();
  private ClientReplyManager replyTable = new ClientReplyManager();
  public long TIMEOUT = 30000L;

  public TCPDispatchingService(String url) {
    this.url = url;
    // builds a transport that do not listen for
    // incoming connections
    try {
      this.transport = new TCPTransport();
    } catch(IOException e) {
      l.severe("Could not instantiate TCPTransport");
      e.printStackTrace();
    }
    this.topologyMgr = new SimpleTopologyManager();
    // TODO should urls for clients be empty?
    // TODO should we add a constructor with only the flag?
    myND = new NodeDescriptor("", false);
    this.topologyMgr.setNodeDescriptor(myND);
    replyTable.start();
  }

  public Message getNextReply(MessageID repliableMessageID) throws ReplyTimeoutException {
    return replyTable.getNextReply(repliableMessageID);
  }

  public boolean hasMoreReplies(MessageID repliableMessageID) {
    return replyTable.hasMoreReplies(repliableMessageID);
  }

  public Replies getAllReplies(MessageID repliableMessageID) {
    LinkedList<Message> replies = new LinkedList<Message>();
    try {
      Message nextReply;
      while((nextReply = getNextReply(repliableMessageID))!=null) {
        replies.add(nextReply);
      }
    } catch(ReplyTimeoutException e) {
      return new Replies(replies.toArray(new Message[replies.size()]), false);
    }
    return new Replies(replies.toArray(new Message[replies.size()]), true);
  }

  public Message getNextMessage() {
    return getNextMessage(0);
  }

  // FIXME might not wait up to timeout because of spurious notify
  public Message getNextMessage(long timeout) {
    synchronized(messages) {
      while(messages.size()==0)
        try {
          messages.wait(timeout);
          if(timeout>0) break;
        } catch(InterruptedException e) {
          l.warning("wait for incoming messages interrupted");
        }
      if(messages.size()==0) return null;
      else return messages.removeFirst();
    }
  }

  public Message getNextMessage(Filter f) {
    synchronized(messages) {
      while(true) {
        for(Message m : messages) {
          if(f.matches(m)) return m;
        }
        try {
          messages.wait();
        } catch(InterruptedException e) {
          l.warning("wait for incoming messages interrupted");
        }
      }
    }
  }

  public boolean hasMoreMessages() {
    return messages.size()!=0;
  }

  public boolean hasMoreMessages(Filter f) {
    synchronized(messages) {
      for(Message m : messages) {
        if(f.matches(m)) return true;
      }
      return false;
    }
  }

  public boolean isOpened() {
    return link!=null&&link.isConnected();
  }

  public void close() {
    if(link!=null) link.close();
  }

  public void open() throws ConnectException {
    transport.addDataListener(this, Router.PUBLISH);
    transport.addDataListener(this, Router.REPLY);
    topologyMgr.addNeighborhoodChangeListener(this);
    topologyMgr.addTransport(transport);
    topologyMgr.start();
    try {
      transport.start();
    } catch(IOException ex) {
      l.severe(ex+" starting "+transport);
      ex.printStackTrace();
    }
    try {
      brokerND = topologyMgr.addNeighbor(url);
      link = topologyMgr.getLinkFor(brokerND);
    } catch(MalformedURLException e) {
      l.severe("Malformed url: "+url);
      e.printStackTrace();
    } catch(NotRunningException e) {
      l.severe("Topology manager not running");
      e.printStackTrace();
    }
  }

  public void notifyNeighborAdded(NodeDescriptor addedNeighbor, Serializable reconfInfo) {
    brokerND = addedNeighbor;
    link = topologyMgr.getLinkFor(brokerND);
  }

  public void notifyNeighborDead(NodeDescriptor deadNeighbor, Serializable reconfInfo) {
    link = null;
  }

  public void notifyNeighborRemoved(NodeDescriptor removedNeighbor) {
    link = null;
  }

  public NodeDescriptor getNodeDescriptor() {
    return myND;
  }

  private void sendOnLink(String subject, Serializable data) {
    try {
      link.send(subject, data);
    } catch(NotConnectedException e) {
      l.warning("Disconnected from brokerND, could not send "+subject+" : "+data);
    } catch(IOException e) {
      l.warning("Error sending "+subject+" : "+data);
    } catch(NotRunningException e) {
      l.severe("Transport not running when sending "+subject+" : "+data);
      e.printStackTrace();
    }
  }

  public void publish(Message msg) {
    msg.createID();
    if(msg instanceof Repliable) {
      replyTable.recordMessage(msg);
    }
    sendOnLink(Router.PUBLISH, msg);
  }

  public void reply(Message reply, MessageID repliableMessageID) {
    reply.createID();
    Reply replyMsg = new Reply(repliableMessageID, true, reply);
    sendOnLink(Router.REPLY, replyMsg);
  }

  public void subscribe(Filter filter) {
    sendOnLink(Router.SUBSCRIBE, filter);
  }

  public void unsubscribe(Filter filter) {
    sendOnLink(Router.UNSUBSCRIBE, filter);
  }

  public void unsubscribeAll() {
    sendOnLink(Router.UNSUBSCRIBEALL, null);
  }

  public void notifyDataArrived(String subject, Link source, Serializable data) {
    if(subject.equals(Router.PUBLISH)) {
      synchronized(messages) {
        messages.add((Message) data);
        messages.notifyAll();
      }
    } else if(subject.equals(Router.REPLY)) {
      replyTable.recordReply((Reply) data);
    }
  }
}
