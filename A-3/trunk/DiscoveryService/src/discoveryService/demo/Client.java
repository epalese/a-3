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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.math.BigInteger;
import java.net.ConnectException;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;

import polimi.reds.TimeoutException;
import discoveryService.core.DSConfiguration;
import discoveryService.core.DSCore;
import discoveryService.core.DSMessage;
import discoveryService.core.MessageListener;
import discoveryService.core.StatusListener;
import discoveryService.core.status.Status;
import discoveryService.core.status.StatusFilter;
import discoveryService.demo.filter.ServiceDescriptorFilter;
import discoveryService.demo.message.AbstractTask;
import discoveryService.demo.message.SearchMessage;
import discoveryService.demo.message.ServiceDescriptor;
import discoveryService.demo.message.TaskResult;

public class Client {
	private DSCore dsCore;
	private String nodeName;
	private MessageListener mListener;
	private StatusListener sListener;
	private final ClientGui gui = new ClientGui(this);
	
	public Client(DSConfiguration dsc) {
		
		gui.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.out.println("Stopping");
				if (Client.this.dsCore != null)
					Client.this.stop();
			}
		});
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				gui.initGUI();
				
				gui.setLocationRelativeTo(null);
				gui.setVisible(true);
			}
		});
		
		nodeName = dsc.getNodeName();
		dsCore = new DSCore(dsc);
		mListener = new MessageListenerImpl();
		sListener = new StatusListenerImpl();
		dsCore.registerMessageListener(mListener);
		dsCore.registerStatusListener(sListener);
	}
	
	public void start() throws ConnectException, InterruptedException {
		dsCore.start();
		java.util.concurrent.TimeUnit.MILLISECONDS.sleep(3000);
	}
	
	public String getNodeName() {
		return nodeName;
	}
	
	public ArrayList<ServiceDescriptor> search(SearchMessage template) {
		ArrayList<ServiceDescriptor> results = new ArrayList<ServiceDescriptor>();
		
		dsCore.search(template);
		
		try {
			// Attende qualche secondo prima di richiedere le repliche
			java.util.concurrent.TimeUnit.MILLISECONDS.sleep(3000);
		} catch(InterruptedException e) {			
		}
		
		while(dsCore.hasMoreReplies()) {
			try {
				results.add((ServiceDescriptor)dsCore.getNextReply(template.getID()));
			} catch (TimeoutException e) {
				System.err.println("Search in time out!");
			}
		}
		System.out.println("Search finished");
		return results;
	}
	
	public ArrayList<TaskResult> getResult(SumTask task) {
		ArrayList<TaskResult> results = new ArrayList<TaskResult>();
		
		System.out.println("in getresult");
		while (results.size() < 1) {
			try {
				java.util.concurrent.TimeUnit.MILLISECONDS.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			while(dsCore.hasMoreReplies(task.getID())) {
				try {
					results.add((TaskResult)dsCore.getNextReply(task.getID()));
				} catch (TimeoutException e) {
					gui.jTextAreaNotify.append("Search in time out!\n");
					System.out.println("Search in time out!");
				}
			}
			System.out.println("results: " + results);
		}
		return results;
	}
	
	public StatusFilter engage(String nodeName) {
		gui.jTextAreaNotify.append("Engaging with " + nodeName + "...");
		StatusFilter sf = dsCore.engage(nodeName);
		gui.jTextAreaNotify.append("success!\n");
		return sf;
	}
	
	public void sendTask(AbstractTask task) {
		task.setNodeName(nodeName);
		dsCore.getDispatchingService().publish(task);
		//dsCore.publish(task, null);
	}
	
	public void stop() {
		System.out.println("Shutdown...");
		dsCore.stop();	
	}
	
	private class MessageListenerImpl implements MessageListener {
		public void notify(DSMessage msg) {
			if (msg instanceof TaskResult) {
				
			}
		}
	}
	
	private class StatusListenerImpl implements StatusListener {
		public void notify(Status status) {
			System.out.println(status);
			gui.jTextAreaNotify.append(status + "\n");
		}
	}
	
	public static void main(String[] args) throws IOException, InterruptedException {
		DSConfiguration conf = new DSConfiguration(args[0]);
		System.out.println("Searching for other brokers...");
		Client client = new Client(conf);
		client.start();
	}
	
	private class ClientGui extends javax.swing.JFrame {
		private static final long serialVersionUID = 4994411812854880505L;
		JLabel jLabel1;
		JLabel jLabel2;
		JLabel jLabel3;
		JTextField jTextFieldNodeName;
		JLabel jLabel5;
		JScrollPane jScrollPane2;
		JScrollPane jScrollPane1;
		JList jListSearchResult;
		JTextArea jTextAreaNotify;
		JButton jButtonSubmit;
		JButton jButtonEngage;
		JLabel jLabel4;
		JButton jButtonSearch;
		JTextField jTextFieldQoS;
		JTextField jTextFieldService;
		
		private Client client;
		private ArrayList<ServiceDescriptor> nodes = new ArrayList<ServiceDescriptor>();

		
		public ClientGui(Client client) {
			super();
			this.client = client;
		}
		
		private void initGUI() {
			try {
				setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
				getContentPane().setLayout(null);
				this.setResizable(false);
				{
					jLabel1 = new JLabel();
					getContentPane().add(jLabel1);
					jLabel1.setText("Node Name");
					jLabel1.setBounds(26, 20, 91, 21);
				}
				{
					jLabel2 = new JLabel();
					getContentPane().add(jLabel2);
					jLabel2.setText("Service");
					jLabel2.setBounds(47, 44, 91, 21);
				}
				{
					jLabel3 = new JLabel();
					getContentPane().add(jLabel3);
					jLabel3.setText("QoS");
					jLabel3.setBounds(48, 69, 91, 21);
				}
				{
					jTextFieldNodeName = new JTextField();
					getContentPane().add(jTextFieldNodeName);
					jTextFieldNodeName.setBounds(108, 20, 128, 19);
				}
				{
					jTextFieldService = new JTextField();
					getContentPane().add(jTextFieldService);
					jTextFieldService.setBounds(108, 45, 128, 19);
				}
				{
					jTextFieldQoS = new JTextField();
					getContentPane().add(jTextFieldQoS);
					jTextFieldQoS.setBounds(108, 70, 128, 19);
				}
				{
					jButtonSearch = new JButton();
					getContentPane().add(jButtonSearch);
					jButtonSearch.setText("Search");
					jButtonSearch.setBounds(120, 111, 88, 28);
					jButtonSearch.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							ServiceDescriptor template = new ServiceDescriptor();
							if (jTextFieldNodeName.getText() == "")
								template.setNodeName(null);
							else
								template.setNodeName(jTextFieldNodeName.getText());
							
							if (jTextFieldService.getText() == "")
								template.setServiceID(null);
							else
								template.setServiceID(jTextFieldService.getText());
							
							if (jTextFieldQoS.getText() == "")
								template.setQos(null);
							else							
								template.setQos(jTextFieldQoS.getText());
							
							SwingUtilities.invokeLater(new UpdateSearchData(template));
							
							ServiceDescriptorFilter sdf = new ServiceDescriptorFilter(template);
							SearchMessage sm = new SearchMessage(sdf);
							ArrayList<ServiceDescriptor> results = client.search(sm);
							synchronized(nodes) {
								nodes = new ArrayList<ServiceDescriptor>(results);
							}
							
							SwingUtilities.invokeLater(new UpdateSearchResult(results));
						}
					});
				}
				{
					jLabel4 = new JLabel();
					getContentPane().add(jLabel4);
					jLabel4.setText("Search Result");
					jLabel4.setBounds(563, 3, 125, 21);
					jLabel4.setHorizontalAlignment(SwingConstants.CENTER);
					jLabel4.setHorizontalTextPosition(SwingConstants.CENTER);
				}
				{
					jButtonEngage = new JButton();
					getContentPane().add(jButtonEngage);
					jButtonEngage.setText("Engage");
					jButtonEngage.setBounds(435, 219, 92, 31);
					jButtonEngage.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							ServiceDescriptor node;
							
							if (jListSearchResult.getSelectedIndex() >= 0) {
								node = nodes.get(jListSearchResult.getSelectedIndex());
								client.engage(node.getNodeName());
							}
							else 
								JOptionPane.showMessageDialog(ClientGui.this, "You must select a node", "!!!", JOptionPane.ERROR_MESSAGE);
						}
					});
				}
				{
					jButtonSubmit = new JButton();
					getContentPane().add(jButtonSubmit);
					jButtonSubmit.setText("Submit Task");
					jButtonSubmit.setBounds(729, 220, 126, 30);
					jButtonSubmit.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							ServiceDescriptor node;
							if (jListSearchResult.getSelectedIndex() >= 0) {
								node = nodes.get(jListSearchResult.getSelectedIndex());
								SumTask task = new SumTask(new BigInteger("1000000000000"), new BigInteger("1000000000000"));
								task.setDestination(node.getNodeName());
								client.sendTask(task);
								Thread t = new Thread(new ResultFetcher(task));
								t.start();

							}
							else 
								JOptionPane.showMessageDialog(ClientGui.this, "You must select a node", "!!!", JOptionPane.ERROR_MESSAGE);

						}
					});
				}
				{
					jScrollPane1 = new JScrollPane();
					getContentPane().add(jScrollPane1);
					jScrollPane1.setBounds(12, 292, 864, 259);
					{
						jTextAreaNotify = new JTextArea();
						jScrollPane1.setViewportView(jTextAreaNotify);
						jTextAreaNotify.setBounds(12, 292, 864, 259);
						jTextAreaNotify.setBorder(BorderFactory.createEtchedBorder(BevelBorder.LOWERED));
						jTextAreaNotify.setEditable(false);
					}
				}
				{
					jLabel5 = new JLabel();
					getContentPane().add(jLabel5);
					jLabel5.setText("Event Notify");
					jLabel5.setBounds(12, 266, 84, 21);
				}
				{
					jScrollPane2 = new JScrollPane();
					getContentPane().add(jScrollPane2);
					jScrollPane2.setBounds(392, 36, 496, 164);
					{
						ListModel jListSearchResultModel = 
							new DefaultComboBoxModel(
									new String[] { });
						jListSearchResult = new JList();
						jScrollPane2.setViewportView(jListSearchResult);
						jListSearchResult.setModel(jListSearchResultModel);
						jListSearchResult.setBounds(392, 36, 496, 164);
						jListSearchResult.setBorder(BorderFactory.createEtchedBorder(BevelBorder.LOWERED));
					}
				}
				pack();
				this.setSize(910, 580);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		class UpdateSearchData implements Runnable {
			ServiceDescriptor template;
			public UpdateSearchData(ServiceDescriptor template) {
				this.template = template;
			}
			
			public void run() {
				jTextAreaNotify.append("Start search. Template: " + template + "\n");
				jTextFieldNodeName.setText("");
				jTextFieldService.setText("");
				jTextFieldQoS.setText("");
			}
		}
		
		class UpdateSearchResult implements Runnable {
			ArrayList<ServiceDescriptor> results;
			
			public UpdateSearchResult(ArrayList<ServiceDescriptor> results) {
				this.results = results;
			}
			
			public void run() {
				ArrayList<String> resultsAsString = new ArrayList<String>();
				for (ServiceDescriptor item : results) {
					if (item != null)
						resultsAsString.add(item.toString());
				}
				Object[] resultList = resultsAsString.toArray();
				ListModel newNodeListModel = new DefaultComboBoxModel(resultList);
				jListSearchResult.setModel(newNodeListModel);
			}
		}
		
		class ResultFetcher implements Runnable {
			SumTask task;
			
			public ResultFetcher(SumTask task) {
				this.task = task;
			}
			
			public void run() {
				System.out.println("Getting results");
				
				for (TaskResult r: client.getResult(task)) {
					jTextAreaNotify.append("TaskResult from " + r.getNodeName() + ": " + r.getResult() + "\n");
					System.out.println("TaskResult from " + r.getNodeName() + ": " + r.getResult());
				}
			}
			
		}

	}

}
