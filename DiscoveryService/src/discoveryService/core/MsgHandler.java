package discoveryService.core;

public abstract class MsgHandler extends Thread {
	abstract public void process(DSMessage msg);
	abstract public void run();
	abstract public void stopProcess();
}
