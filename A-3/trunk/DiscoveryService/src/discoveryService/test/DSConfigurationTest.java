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

package discoveryService.test;

import java.io.FileNotFoundException;
import java.io.IOException;

import polimi.reds.DispatchingService;
import discoveryService.broker.Broker;
import discoveryService.core.DSConfiguration;
import discoveryService.core.DSFactory;

public class DSConfigurationTest {
	public static void main(String[] args) throws FileNotFoundException, IOException {
		DSConfiguration dsc = new DSConfiguration(args[0]);
		System.out.println(dsc.toString());
		
		Broker broker = DSFactory.createBroker(dsc);
		System.out.println(broker.getURL());
		
		DispatchingService ds = DSFactory.createDispatchingService(dsc);
		System.out.println(ds.isOpened());
		ds.open();
		System.out.println(ds.isOpened());
	}
}
