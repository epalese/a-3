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

package discoveryService.core;

/**
 * Thrown when {@link DSCore} finds an illegal configuration parameter.
 * 
 * @author leleplx@gmail.com (emanuele)
 *
 */
public class IllegalConfigurationException extends IllegalArgumentException {
	private static final long serialVersionUID = -8283189470624318525L;
	private String error;
	
	public IllegalConfigurationException(String msg) {
		super();
		error = msg;
	}
	
	public String getError() {
		return error;
	}

}
