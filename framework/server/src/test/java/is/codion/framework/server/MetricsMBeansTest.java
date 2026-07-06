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
import is.codion.common.rmi.client.ConnectionRequest;
import is.codion.common.rmi.server.ServerConfiguration;
import is.codion.common.utilities.user.User;
import is.codion.framework.db.rmi.RemoteEntityConnection;
import is.codion.framework.db.rmi.RemoteEntityConnectionProvider;

import org.junit.jupiter.api.Test;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

import static is.codion.framework.domain.entity.condition.Condition.all;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class MetricsMBeansTest {

	private static final User ADMIN_USER = User.parse("scott:tiger");

	@Test
	void test() throws Exception {
		Clients.SERVER_HOSTNAME.set("localhost");
		Clients.TRUSTSTORE.set("src/main/config/truststore.jks");
		Clients.resolveTrustStore();
		ServerConfiguration.RMI_SERVER_HOSTNAME.set("localhost");
		ServerConfiguration.KEYSTORE.set("src/main/config/keystore.jks");
		ServerConfiguration.KEYSTORE_PASSWORD.set("crappypass");
		EntityServerConfiguration.JMX.set(true);

		EntityServerConfiguration configuration = EntityServerConfiguration.builder(3334, 3332)
						.adminPort(3335)
						.adminUser(ADMIN_USER)
						.database(Database.instance())
						.domainClasses(singletonList("is.codion.framework.server.TestDomain"))
						.connectionPoolUsers(singletonList(ADMIN_USER))
						.objectInputFilterFactoryRequired(false)
						.build();
		EntityServer server = EntityServer.startServer(configuration);
		MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
		ObjectName serverName = new ObjectName("is.codion:type=EntityServer");
		ObjectName poolName = new ObjectName("is.codion:type=ConnectionPool,username=scott");
		try {
			assertTrue(mBeanServer.isRegistered(serverName));
			assertTrue(mBeanServer.isRegistered(poolName));
			assertEquals(-1, number(mBeanServer, serverName, "ConnectionLimit"));

			ConnectionRequest connectionRequest = ConnectionRequest.builder()
							.user(ADMIN_USER)
							.clientType("MetricsMBeansTest")
							.parameter(RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_TYPE, "TestDomain")
							.build();
			RemoteEntityConnection connection = (RemoteEntityConnection) server.connect(connectionRequest);
			connection.select(all(TestDomain.Department.TYPE));

			assertEquals(1, number(mBeanServer, serverName, "ConnectionCount"));
			assertTrue(number(mBeanServer, serverName, "RequestCount") > 0);
			assertTrue(number(mBeanServer, poolName, "Requests") > 0);
			assertTrue(number(mBeanServer, poolName, "Size") >= 0);

			ObjectName selectName = new ObjectName("is.codion:type=OperationLatency,operation=select");
			assertTrue(registered(mBeanServer, selectName));
			assertTrue(number(mBeanServer, selectName, "Count") > 0);
			assertNotNull(mBeanServer.getAttribute(selectName, "Sum"));
			javax.management.openmbean.TabularData buckets =
							(javax.management.openmbean.TabularData) mBeanServer.getAttribute(selectName, "Buckets");
			assertTrue(buckets.containsKey(new Object[] {"+Inf"}));

			connection.close();
		}
		finally {
			server.shutdown();
			assertFalse(mBeanServer.isRegistered(serverName));
			assertFalse(mBeanServer.isRegistered(poolName));
			EntityServerConfiguration.JMX.set(false);
			ServerConfiguration.RMI_SERVER_HOSTNAME.set(null);
		}
	}

	private static long number(MBeanServer mBeanServer, ObjectName objectName, String attribute) throws Exception {
		return ((Number) mBeanServer.getAttribute(objectName, attribute)).longValue();
	}

	private static boolean registered(MBeanServer mBeanServer, ObjectName objectName) throws InterruptedException {
		for (int i = 0; i < 50 && !mBeanServer.isRegistered(objectName); i++) {
			Thread.sleep(100);
		}

		return mBeanServer.isRegistered(objectName);
	}
}
