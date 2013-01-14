package discoveryService.demo;

import java.math.BigInteger;
import java.util.Random;

import discoveryService.demo.message.AbstractTask;
import discoveryService.demo.message.TaskResult;

public class SumTask extends AbstractTask {
	private static final long serialVersionUID = 1469530781347012310L;
	BigInteger num1, num2;
	
	public SumTask(BigInteger a, BigInteger b) {
		num1 = a;
		num2 = b;
		taskResult = new TaskResult();
	}
	
	@Override
	public void perform() {
		BigInteger result = num1.add(num2);
		
		try {
			java.util.concurrent.TimeUnit.MILLISECONDS.sleep(new Random().nextInt(4000));
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
		taskResult.setResult(result);
	}

}
