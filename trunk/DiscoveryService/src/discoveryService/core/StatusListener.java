package discoveryService.core;

import discoveryService.core.status.Status;

public interface StatusListener {
	public void notify(Status status);
}
