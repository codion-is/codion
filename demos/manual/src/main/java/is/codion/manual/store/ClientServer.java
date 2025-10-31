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
package is.codion.manual.store;

import is.codion.common.db.database.Database;
import is.codion.dbms.h2.H2DatabaseFactory;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.http.HttpEntityConnectionProvider;
import is.codion.framework.db.rmi.RemoteEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.server.EntityServer;
import is.codion.framework.server.EntityServerConfiguration;
import is.codion.framework.servlet.EntityService;
import is.codion.framework.servlet.EntityServiceFactory;
import is.codion.manual.store.domain.Store;
import is.codion.manual.store.domain.Store.Customer;

import java.rmi.RemoteException;
import java.util.List;

import static is.codion.common.user.User.parse;
import static is.codion.framework.domain.entity.condition.Condition.all;

public final class ClientServer {

	private static final int SERVER_PORT = 2223;
	private static final int REGISTRY_PORT = 1099;
	private static final int HTTP_PORT = 8080;

	private static void runServer() throws RemoteException {
		// tag::runServer[]
		Database database = H2DatabaseFactory
						.createDatabase("jdbc:h2:mem:testdb",
										"src/main/sql/create_schema.sql");

		EntityServerConfiguration configuration =
						EntityServerConfiguration.builder(SERVER_PORT, REGISTRY_PORT)
										.domainClasses(List.of(Store.class.getName()))
										.database(database)
										.sslEnabled(false)
										.build();

		EntityServer server = EntityServer.startServer(configuration);

		RemoteEntityConnectionProvider connectionProvider =
						RemoteEntityConnectionProvider.builder()
										.port(SERVER_PORT)
										.registryPort(REGISTRY_PORT)
										.domain(Store.DOMAIN)
										.user(parse("scott:tiger"))
										.build();

		EntityConnection connection = connectionProvider.connection();

		List<Entity> customers = connection.select(all(Customer.TYPE));
		customers.forEach(System.out::println);

		connection.close();

		server.shutdown();
		// end::runServer[]
	}

	private static void runServerWithHttp() throws RemoteException {
		// tag::runServerWithHttp[]
		Database database = H2DatabaseFactory
						.createDatabase("jdbc:h2:mem:testdb",
										"src/main/sql/create_schema.sql");

		EntityService.PORT.set(HTTP_PORT);

		EntityServerConfiguration configuration =
						EntityServerConfiguration.builder(SERVER_PORT, REGISTRY_PORT)
										.domainClasses(List.of(Store.class.getName()))
										.database(database)
										.sslEnabled(false)
										.auxiliaryServerFactory(List.of(EntityServiceFactory.class.getName()))
										.build();

		EntityServer server = EntityServer.startServer(configuration);

		HttpEntityConnectionProvider connectionProvider =
						HttpEntityConnectionProvider.builder()
										.port(HTTP_PORT)
										.https(false)
										.domain(Store.DOMAIN)
										.user(parse("scott:tiger"))
										.build();

		EntityConnection connection = connectionProvider.connection();

		List<Entity> customers = connection.select(all(Customer.TYPE));
		customers.forEach(System.out::println);

		connection.close();

		server.shutdown();
		// end::runServerWithHttp[]
	}

	public static void main(String[] args) throws RemoteException {
		runServer();
		runServerWithHttp();
	}
}
