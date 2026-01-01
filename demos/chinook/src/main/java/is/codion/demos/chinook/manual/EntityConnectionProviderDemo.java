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
 * Copyright (c) 2004 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.manual;

import is.codion.common.db.database.Database;
import is.codion.common.utilities.user.User;
import is.codion.demos.chinook.domain.ChinookImpl;
import is.codion.demos.chinook.domain.api.Chinook;
import is.codion.demos.chinook.domain.api.Chinook.Track;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.http.HttpEntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.db.rmi.RemoteEntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;

import java.sql.Connection;

public final class EntityConnectionProviderDemo {

	static void localConnectionProvider() {
		// tag::local[]
		Database.URL.set("jdbc:h2:mem:h2db");
		Database.INIT_SCRIPTS.set("src/main/sql/create_schema.sql");

		Database database = Database.instance();

		LocalEntityConnectionProvider connectionProvider =
						LocalEntityConnectionProvider.builder()
										.database(database)
										.domain(new ChinookImpl())
										.user(User.parse("scott:tiger"))
										.build();

		LocalEntityConnection entityConnection =
						connectionProvider.connection();

		// the underlying JDBC connection is available in a local connection
		Connection connection = entityConnection.connection();

		connectionProvider.close();
		// end::local[]
	}

	static void remoteConnectionProvider() {
		// tag::remote[]
		RemoteEntityConnectionProvider connectionProvider =
						RemoteEntityConnectionProvider.builder()
										.domain(Chinook.DOMAIN)
										.user(User.parse("scott:tiger"))
										.hostname("localhost")
										.registryPort(1099)
										.build();

		EntityConnection entityConnection =
						connectionProvider.connection();

		Entities entities = entityConnection.entities();

		Entity track = entityConnection.select(entities.primaryKey(Track.TYPE, 42L));

		connectionProvider.close();
		// end::remote[]
	}

	static void httpConnectionProvider() {
		// tag::http[]
		HttpEntityConnectionProvider connectionProvider =
						HttpEntityConnectionProvider.builder()
										.domain(Chinook.DOMAIN)
										.user(User.parse("scott:tiger"))
										.hostname("localhost")
										.port(8080)
										.https(false)
										.build();

		EntityConnection entityConnection = connectionProvider.connection();

		Entities entities = entityConnection.entities();

		entityConnection.select(entities.primaryKey(Track.TYPE, 42L));

		connectionProvider.close();
		// end::http[]
	}
}
