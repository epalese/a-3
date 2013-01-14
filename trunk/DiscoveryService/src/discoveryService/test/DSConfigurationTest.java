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
