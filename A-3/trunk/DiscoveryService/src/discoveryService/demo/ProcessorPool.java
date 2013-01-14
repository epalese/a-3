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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.Random;

import discoveryService.core.DSConfiguration;
import discoveryService.demo.message.ServiceDescriptor;

public class ProcessorPool {
	public final static int NUMBER_OF_PROCESSEORS = 10;

	public static void main(String[] args) {
		String[] qos = new String[] {"HIGH", "MEDIUM", "LOW"};
		DSConfiguration conf = null;
		
		try {
			conf = new DSConfiguration(args[0]);
		} catch (FileNotFoundException e) {
			System.err.println("Error in parsing configuration file!");
			System.exit(-1);
		} catch (IOException e) {
			System.err.println("Error in parsing configuration file!");
			System.exit(-1);
		}
		
		Random rand = new Random();
		
		try {
			for (int i = 0; i < NUMBER_OF_PROCESSEORS; i++) {
				DSConfiguration confTmp = new DSConfiguration();
				confTmp.setProtocol(conf.getProtocol());
				confTmp.setMaxNumOfNeighbors(conf.getMaxNumOfNeighbors());
				confTmp.setBrokerAddress(conf.getBrokerAddress());
				confTmp.setBrokerPort(conf.getBrokerPort() + i);
				confTmp.setNodeName("processor"+i);
				ServiceDescriptor sd = new ServiceDescriptor();
				sd.setNodeName(conf.getNodeName());
				sd.setServiceID("processor service");
				sd.setQos(qos[rand.nextInt(3)]);
				Processor p = new Processor(confTmp, sd);
				p.start();
			}
							
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (ConnectException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		System.out.println("ProcessorPool started succesfully!");		
		
	}
}
