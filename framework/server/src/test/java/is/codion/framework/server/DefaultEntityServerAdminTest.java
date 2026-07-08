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
 * Copyright (c) 2022 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.server;

import is.codion.common.db.database.Database;
import is.codion.common.db.report.Report;
import is.codion.common.rmi.client.Clients;
import is.codion.common.rmi.client.ConnectionRequest;
import is.codion.common.rmi.server.ServerConfiguration;
import is.codion.common.rmi.server.exception.ConnectionNotAvailableException;
import is.codion.common.rmi.server.exception.LoginException;
import is.codion.common.utilities.user.User;
import is.codion.framework.db.rmi.RemoteEntityConnectionProvider;

import org.junit.jupiter.api.Test;

import java.rmi.RemoteException;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class DefaultEntityServerAdminTest {

	private static final User ADMIN_USER = User.parse("scott:tiger");
	private static final String SCOTT = "scott";

	@Test
	void test() throws RemoteException, ConnectionNotAvailableException, LoginException {
		Clients.SERVER_HOSTNAME.set("localhost");
		Clients.TRUSTSTORE.set("src/main/config/truststore.jks");
		Clients.resolveTrustStore();
		ServerConfiguration.RMI_SERVER_HOSTNAME.set("localhost");
		ServerConfiguration.KEYSTORE.set("src/main/config/keystore.jks");
		ServerConfiguration.KEYSTORE_PASSWORD.set("crappypass");
		Report.REPORT_PATH.set("a/report/path");

		EntityServerConfiguration configuration = EntityServerConfiguration.builder()
						.port(3224)
						.registryPort(3222)
						.adminPort(3225)
						.adminUser(ADMIN_USER)
						.database(Database.instance())
						.domainClasses(singletonList("is.codion.framework.server.TestDomain"))
						.connectionPoolUsers(singletonList(ADMIN_USER))
						.objectInputFilterFactoryRequired(false)
						.build();
		EntityServer server = EntityServer.startServer(configuration);
		try {
			ConnectionRequest connectionRequest = ConnectionRequest.builder()
							.user(ADMIN_USER)
							.clientType("DefaultEntityServerAdminTest")
							.parameter(RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_TYPE, "TestDomain")
							.build();
			server.connect(connectionRequest);
			EntityServerAdmin admin = new DefaultEntityServerAdmin(server, configuration);
			admin.tracingEnabled(connectionRequest.clientId(), true);
			assertTrue(admin.tracingEnabled(connectionRequest.clientId()));
			admin.resetConnectionPoolStatistics(SCOTT);
			admin.collectPoolSnapshotStatistics(SCOTT, true);
			assertTrue(admin.collectPoolSnapshotStatistics(SCOTT));
			admin.collectPoolCheckOutTimes(SCOTT, true);
			assertTrue(admin.collectPoolCheckOutTimes(SCOTT));
			assertNotNull(admin.connectionPoolStatistics(SCOTT, System.currentTimeMillis()));
			admin.connectionPoolCleanupInterval(SCOTT, 10);
			assertEquals(0, admin.connectionPoolCleanupInterval(SCOTT));//not configurable for hikari
			admin.minimumConnectionPoolSize(SCOTT, 2);
			assertEquals(2, admin.minimumConnectionPoolSize(SCOTT));
			admin.maximumConnectionPoolSize(SCOTT, 11);
			assertEquals(11, admin.maximumConnectionPoolSize(SCOTT));
			admin.maximumPoolCheckOutTime(SCOTT, 300);
			assertEquals(300, admin.maximumPoolCheckOutTime(SCOTT));
			admin.pooledConnectionIdleTimeout(SCOTT, 1000);
			assertEquals(1000, admin.pooledConnectionIdleTimeout(SCOTT));
			admin.methodTraces(connectionRequest.clientId());

			admin.idleConnectionTimeout(30);
			try {
				admin.idleConnectionTimeout(-1);
				fail();
			}
			catch (IllegalArgumentException ignored) {/*ignored*/}
			assertEquals(30, admin.idleConnectionTimeout());
			admin.databaseStatistics();
			admin.databaseUrl();
			admin.connectionPoolUsernames();
			admin.maintenanceInterval(500);
			admin.domainEntityDefinitions();
			assertEquals(1, admin.domainReports().get("TestDomain").size());
			assertEquals(2, admin.domainOperations().get("TestDomain").size());
			admin.clearReportCache();
			assertEquals(500, admin.maintenanceInterval());
			admin.serverInformation();
			admin.systemProperties();
			admin.serializationFilterPatterns();
			admin.users();
			admin.statistics(System.currentTimeMillis());
			admin.disconnectTimedOutClients();
			admin.disconnectAllClients();
		}
		finally {
			server.shutdown();
			ServerConfiguration.RMI_SERVER_HOSTNAME.set(null);
		}
	}
}
