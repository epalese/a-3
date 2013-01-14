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
