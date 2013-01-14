/***
 * * A-3 DiscoveryService
 * * <mailto: leleplx@gmail.com>
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
 */

package discoveryService.demo;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;

import discoveryService.core.DSConfiguration;
import discoveryService.core.DSCore;
import discoveryService.core.DSMessage;
import discoveryService.core.MessageListener;
import discoveryService.core.status.Status;
import discoveryService.demo.filter.SearchMessageFilter;
import discoveryService.demo.filter.TaskFilter;
import discoveryService.demo.message.AbstractTask;
import discoveryService.demo.message.SearchMessage;
import discoveryService.demo.message.ServiceDescriptor;
import discoveryService.demo.message.TaskResult;

public class Processor {
	private DSConfiguration dsc;
	private ServiceDescriptor sd;
	private DSCore dsCore;
	private MessageListener mListener;
	private Status status;
	
	public Processor(DSConfiguration dsc, ServiceDescriptor sd) throws UnknownHostException, ConnectException {
		this.dsc = dsc;
		this.sd = sd;
		//status = new  Status(dsc.getNodeName(), InetAddress.getLocalHost().getHostAddress(), new Integer(0));
		dsCore = new DSCore(dsc);
		mListener = new MessageListenerImpl();
		dsCore.registerMessageListener(mListener);
		dsCore.start();
	}
	
	public void start() throws ConnectException {
		
		status = dsCore.getStatus();
		status.setStatus(new Integer(0));
		
		// pubblica il suo descrittore di servizio
		dsCore.publish(sd, new SearchMessageFilter(sd));
		// si sottoscrive ai Task indirizzati a questo nodo
		dsCore.subscribe(new TaskFilter(dsc.getNodeName()));
	}
	
	private class MessageListenerImpl implements MessageListener {
		public void notify(DSMessage msg) {
			if (msg instanceof SearchMessage) {
				System.out.println("[" + dsc.getNodeName() + "]: received from " + msg.getNodeName() + " SearchMessage --> " + ((SearchMessage)msg).toString());
				dsCore.reply(sd, msg.getID());
			}
			else if (msg instanceof AbstractTask) {
				System.out.println("[" + dsc.getNodeName() + "]: received from " + msg.getNodeName() + " Task --> " + ((SumTask)msg).toString());
				System.out.println("[" + dsc.getNodeName() + "]: starting performer thread");
				new Thread(new Performer((AbstractTask)msg)).start();
			}
		}
	}
	
	private class Performer implements Runnable {
		private AbstractTask task;
		
		public Performer(AbstractTask task) {
			this.task = task;
		}
		
		public void run() {
			System.out.println("["+dsc.getNodeName()+"] Performing task "+ task + " received from " + task.getNodeName());
			synchronized(status) {
				Integer currentTaskProcessed = (Integer)status.getStatus();
				currentTaskProcessed++;
				status.setStatus(currentTaskProcessed);
				dsCore.setStatus(status);
				dsCore.notifyStatusUpdate();
			}
			
			task.perform();
			TaskResult tr = task.getTaskResult();
			System.out.println("["+dsc.getNodeName()+"] Task " + task + " performed");
			tr.setNodeName(task.getDestination());
			dsCore.reply(tr, task.getID());
			System.out.println("["+dsc.getNodeName()+"] Send result of " + task + " to " + task.getNodeName());
			
			synchronized(status) {
				Integer currentTaskProcessed = (Integer)status.getStatus();
				currentTaskProcessed--;
				status.setStatus(currentTaskProcessed);
				dsCore.setStatus(status);
				dsCore.notifyStatusUpdate();
			}
		}
	}
	
	// args: config_file nodeName serviceID Qos port
	public static void main(String[] args) throws IOException {
		DSConfiguration conf = new DSConfiguration(args[0]);
		conf.setNodeName(args[1]);
		conf.setBrokerPort(Integer.valueOf(args[4]));
		
		ServiceDescriptor desc = new ServiceDescriptor();
		desc.setNodeName(conf.getNodeName());
		desc.setServiceID(args[2]);
		desc.setQos(args[3]);
		
		Processor p = new Processor(conf, desc);
		p.start();
	}
}
