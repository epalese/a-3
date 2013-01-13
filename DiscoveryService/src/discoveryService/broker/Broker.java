package discoveryService.broker;

import polimi.reds.broker.overlay.Overlay;

public interface Broker {
	public void start();
	public void stop();
	public void addNeighbor(String url);
	public void locateNeighbors(int num);
	public Overlay getOverlay();
	public String getURL();
}
