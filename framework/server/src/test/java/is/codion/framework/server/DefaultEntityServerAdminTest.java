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
 * Copyright (c) 2022 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.server;

import is.codion.common.db.database.Database;
import is.codion.common.db.report.Report;
import is.codion.common.rmi.client.Clients;
import is.codion.common.rmi.client.ConnectionRequest;
import is.codion.common.rmi.server.ServerConfiguration;
import is.codion.common.rmi.server.exception.ConnectionNotAvailableException;
import is.codion.common.rmi.server.exception.LoginException;
import is.codion.common.user.User;
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

		EntityServerConfiguration configuration = EntityServerConfiguration.builder(3224, 3222)
						.adminPort(3225)
						.adminUser(ADMIN_USER)
						.database(Database.instance())
						.domainClasses(singletonList("is.codion.framework.server.TestDomain"))
						.connectionPoolUsers(singletonList(ADMIN_USER))
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
			admin.setTracingEnabled(connectionRequest.clientId(), true);
			assertTrue(admin.isTracingEnabled(connectionRequest.clientId()));
			admin.setLogLevel("TEST");//no op logger
			admin.getLogLevel();
			admin.resetConnectionPoolStatistics(SCOTT);
			admin.setCollectPoolSnapshotStatistics(SCOTT, true);
			assertTrue(admin.isCollectPoolSnapshotStatistics(SCOTT));
			admin.setCollectPoolCheckOutTimes(SCOTT, true);
			assertTrue(admin.isCollectPoolCheckOutTimes(SCOTT));
			assertNotNull(admin.connectionPoolStatistics(SCOTT, System.currentTimeMillis()));
			admin.setConnectionPoolCleanupInterval(SCOTT, 10);
			assertEquals(0, admin.getConnectionPoolCleanupInterval(SCOTT));//not configurable for hikari
			admin.setMinimumConnectionPoolSize(SCOTT, 2);
			assertEquals(2, admin.getMinimumConnectionPoolSize(SCOTT));
			admin.setMaximumConnectionPoolSize(SCOTT, 11);
			assertEquals(11, admin.getMaximumConnectionPoolSize(SCOTT));
			admin.setMaximumPoolCheckOutTime(SCOTT, 300);
			assertEquals(300, admin.getMaximumPoolCheckOutTime(SCOTT));
			admin.setPooledConnectionIdleTimeout(SCOTT, 1000);
			assertEquals(1000, admin.getPooledConnectionIdleTimeout(SCOTT));
			admin.methodTraces(connectionRequest.clientId());

			admin.setIdleConnectionTimeout(30);
			try {
				admin.setIdleConnectionTimeout(-1);
				fail();
			}
			catch (IllegalArgumentException ignored) {/*ignored*/}
			assertEquals(30, admin.getIdleConnectionTimeout());
			admin.databaseStatistics();
			admin.databaseUrl();
			admin.connectionPoolUsernames();
			admin.setMaintenanceInterval(500);
			admin.domainEntityDefinitions();
			assertEquals(1, admin.domainReports().get("TestDomain").size());
			assertEquals(2, admin.domainOperations().get("TestDomain").size());
			admin.clearReportCache();
			assertEquals(500, admin.getMaintenanceInterval());
			admin.serverInformation();
			admin.systemProperties();
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
