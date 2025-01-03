/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2004 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.manual.quickstart;

import is.codion.common.user.User;
import is.codion.framework.domain.test.DomainTest;
import is.codion.manual.quickstart.Store.Address;
import is.codion.manual.quickstart.Store.Customer;
import is.codion.manual.quickstart.Store.CustomerAddress;

import org.junit.jupiter.api.Test;

// tag::domainModelTest[]
public final class StoreTest extends DomainTest {

	public StoreTest() {
		super(new Store(), User.parse("scott:tiger"));
	}

	@Test
	void customer() {
		test(Customer.TYPE);
	}

	@Test
	void address() {
		test(Address.TYPE);
	}

	@Test
	void customerAddress() {
		test(CustomerAddress.TYPE);
	}
}
// end::domainModelTest[]