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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.server;

import is.codion.common.rmi.server.ServerConfiguration;
import is.codion.common.utilities.user.User;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class DefaultEntityServerConfigurationTest {

	@AfterEach
	void tearDown() {
		EntityServerConfiguration.DOMAIN_CLASSES.remove();
		EntityServerConfiguration.CONNECTION_POOL_USERS.remove();
		EntityServerConfiguration.CLIENT_CONNECTION_TIMEOUT.remove();
		ServerConfiguration.AUXILIARY_SERVER_FACTORIES.remove();
		ServerConfiguration.AUTHENTICATOR_EXEMPTIONS.remove();
	}

	@Test
	void addToProperties() {
		properties();
		//the singular methods adding to those configured via the properties
		EntityServerConfiguration configuration = EntityServerConfiguration.builder()
						.port(1234)
						.domainClass("c.Domain")
						.connectionPoolUser(User.parse("john:doe"))
						.clientTypeIdleConnectionTimeout("Chinook", 3000)
						.auxiliaryServerFactory("c.Factory")
						.authenticatorExemption("Petstore")
						.build();
		assertEquals(new HashSet<>(asList("a.Domain", "b.Domain", "c.Domain")), configuration.domainClasses());
		assertEquals(new HashSet<>(asList(User.parse("scott:tiger"), User.parse("john:doe"))), configuration.connectionPoolUsers());
		Map<String, Integer> timeouts = new HashMap<>();
		timeouts.put("Chinook", 3000);
		timeouts.put("World", 2000);
		assertEquals(timeouts, configuration.clientTypeIdleConnectionTimeouts());
		assertEquals(new HashSet<>(asList("a.Factory", "c.Factory")), configuration.auxiliaryServerFactories());
		assertEquals(new HashSet<>(asList("Employees", "Petstore")), configuration.authenticatorExemptions());
	}

	@Test
	void replaceProperties() {
		properties();
		//the plural methods replacing those configured via the properties
		EntityServerConfiguration configuration = EntityServerConfiguration.builder()
						.port(1234)
						.domainClasses(singletonList("c.Domain"))
						.connectionPoolUsers(singletonList(User.parse("john:doe")))
						.clientTypeIdleConnectionTimeouts(singletonMap("Petstore", 3000))
						.auxiliaryServerFactories(singletonList("c.Factory"))
						.authenticatorExemptions(singletonList("Petstore"))
						.build();
		assertEquals(new HashSet<>(singletonList("c.Domain")), configuration.domainClasses());
		assertEquals(new HashSet<>(singletonList(User.parse("john:doe"))), configuration.connectionPoolUsers());
		assertEquals(singletonMap("Petstore", 3000), configuration.clientTypeIdleConnectionTimeouts());
		assertEquals(new HashSet<>(singletonList("c.Factory")), configuration.auxiliaryServerFactories());
		assertEquals(new HashSet<>(singletonList("Petstore")), configuration.authenticatorExemptions());
	}

	private static void properties() {
		EntityServerConfiguration.DOMAIN_CLASSES.set("a.Domain, b.Domain");
		EntityServerConfiguration.CONNECTION_POOL_USERS.set("scott:tiger");
		EntityServerConfiguration.CLIENT_CONNECTION_TIMEOUT.set("Chinook:1000, World:2000");
		ServerConfiguration.AUXILIARY_SERVER_FACTORIES.set("a.Factory");
		ServerConfiguration.AUTHENTICATOR_EXEMPTIONS.set("Employees");
	}
}
