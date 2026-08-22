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

import is.codion.common.db.database.Database;
import is.codion.common.rmi.client.Clients;
import is.codion.common.rmi.server.ServerConfiguration;
import is.codion.common.utilities.logging.MethodTrace;
import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.rmi.RemoteEntityConnection;
import is.codion.framework.server.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import java.util.List;

import static is.codion.framework.domain.entity.condition.Condition.all;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class ConnectionTraceTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	/**
	 * The connection round trip is traced in four parts, so that the cost of the pool and the cost of
	 * whatever the application does to the connection can be told apart in the trace log.
	 */
	@Test
	void pooledConnectionRoundTrip() throws Exception {
		EntityServer server = EntityServer.startServer(configure());
		try {
			EntityConnection connection = RemoteEntityConnection.builder()
							.hostname("localhost")
							.port(3723)
							.registryPort(3721)
							.domain(TestDomain.DOMAIN)
							.clientType("ConnectionTraceTest")
							.user(UNIT_TEST_USER)
							.build();
			EntityServerAdmin admin = server.admin(UNIT_TEST_USER);
			admin.tracingEnabled(connection.id(), true);

			connection.select(all(Employee.TYPE));

			List<MethodTrace> traces = admin.methodTraces(connection.id());
			MethodTrace select = traces.get(traces.size() - 1);
			assertEquals("select", select.method());

			List<String> connectionTrace = select.children().stream()
							.map(MethodTrace::method)
							.filter(method -> method.endsWith("Connection"))
							.collect(toList());

			//fetch and return bracket the pool, prepare and release bracket the session state
			assertEquals(4, connectionTrace.size());
			assertEquals("fetchConnection", connectionTrace.get(0));
			assertEquals("prepareConnection", connectionTrace.get(1));
			assertEquals("releaseConnection", connectionTrace.get(2));
			assertEquals("returnConnection", connectionTrace.get(3));

			connection.close();
		}
		finally {
			server.shutdown();
		}
	}

	private static EntityServerConfiguration configure() {
		Clients.SERVER_HOSTNAME.set("localhost");
		Clients.TRUSTSTORE.set("src/main/config/truststore.jks");
		Clients.resolveTrustStore();
		ServerConfiguration.RMI_SERVER_HOSTNAME.set("localhost");
		ServerConfiguration.KEYSTORE.set("src/main/config/keystore.jks");
		ServerConfiguration.KEYSTORE_PASSWORD.set("crappypass");

		return EntityServerConfiguration.builder()
						.port(3723)
						.registryPort(3721)
						.adminPort(3724)
						.adminUser(UNIT_TEST_USER)
						.database(Database.instance())
						.connectionPoolUsers(singletonList(UNIT_TEST_USER))
						.domainClasses(singletonList("is.codion.framework.server.TestDomain"))
						.methodTracing(true)
						.objectInputFilterFactoryRequired(false)
						.build();
	}
}
