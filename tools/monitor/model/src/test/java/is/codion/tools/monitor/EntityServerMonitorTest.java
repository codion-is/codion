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
 * Copyright (c) 2010 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.tools.monitor;

import is.codion.common.db.database.Database;
import is.codion.common.rmi.client.Clients;
import is.codion.common.rmi.server.RemoteClient;
import is.codion.common.rmi.server.Server;
import is.codion.common.rmi.server.ServerConfiguration;
import is.codion.common.user.User;
import is.codion.framework.db.rmi.RemoteEntityConnectionProvider;
import is.codion.framework.server.EntityServer;
import is.codion.framework.server.EntityServerAdmin;
import is.codion.framework.server.EntityServerConfiguration;
import is.codion.tools.monitor.model.ClientMonitor;
import is.codion.tools.monitor.model.ClientUserMonitor;
import is.codion.tools.monitor.model.EntityServerMonitor;
import is.codion.tools.monitor.model.HostMonitor;
import is.codion.tools.monitor.model.ServerMonitor;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.rmi.registry.LocateRegistry;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class EntityServerMonitorTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final User ADMIN_USER = User.parse("scott:tiger");
	private static Server<?, EntityServerAdmin> server;
	private static EntityServerAdmin admin;

	public static final EntityServerConfiguration CONFIGURATION = configure();

	@BeforeAll
	public static synchronized void setUp() throws Exception {
		EntityServer.startServer(CONFIGURATION);
		server = (Server<?, EntityServerAdmin>) LocateRegistry.getRegistry(Clients.SERVER_HOSTNAME.get(),
						CONFIGURATION.registryPort()).lookup(CONFIGURATION.serverName());
		admin = server.admin(ADMIN_USER);
	}

	@AfterAll
	public static synchronized void tearDown() throws Exception {
		admin.shutdown();
		server = null;
	}

	@Test
	void test() throws Exception {
		String clientType = EntityServerMonitorTest.class.getName();
		RemoteEntityConnectionProvider connectionProvider =
						RemoteEntityConnectionProvider.builder()
										.hostname("localhost")
										.port(CONFIGURATION.port())
										.registryPort(CONFIGURATION.registryPort())
										.domain(TestDomain.DOMAIN)
										.clientType(clientType)
										.user(UNIT_TEST_USER)
										.build();
		connectionProvider.connection();
		EntityServerMonitor model = new EntityServerMonitor("localhost", CONFIGURATION.registryPort(), CONFIGURATION.adminUser());
		model.refresh();
		HostMonitor hostMonitor = model.hostMonitors().iterator().next();
		assertEquals("localhost", hostMonitor.hostname());
		hostMonitor.refresh();
		ServerMonitor serverMonitor = hostMonitor.serverMonitors().iterator().next();
		assertNotNull(serverMonitor);
		ClientUserMonitor clientUserMonitor = serverMonitor.clientMonitor();
		ClientMonitor clientMonitor = clientUserMonitor.clientMonitor();
		clientMonitor.clientInstanceTableModel().items().refresh();
		assertEquals(1, clientMonitor.clientInstanceTableModel().items().included().size());
		RemoteClient remoteClient = clientMonitor.clientInstanceTableModel().items().included().get(0);
		assertEquals(connectionProvider.clientId(), remoteClient.clientId());
		assertEquals(UNIT_TEST_USER, remoteClient.user());

		clientMonitor.server().disconnect(remoteClient.clientId());//disconnects the client

		clientMonitor.refresh();
		assertEquals(0, clientMonitor.clientInstanceTableModel().items().included().size());

		serverMonitor.shutdown();
	}

	private static EntityServerConfiguration configure() {
		Clients.SERVER_HOSTNAME.set("localhost");
		ServerConfiguration.RMI_SERVER_HOSTNAME.set("localhost");

		return EntityServerConfiguration.builder(3223, 3221)
						.adminPort(3223)
						.adminUser(User.parse("scott:tiger"))
						.connectionPoolUsers(Collections.singletonList(UNIT_TEST_USER))
						.domainClasses(Collections.singletonList(TestDomain.class.getName()))
						.objectInputFilterFactoryRequired(false)
						.database(Database.instance())
						.sslEnabled(false)
						.build();
	}
}
