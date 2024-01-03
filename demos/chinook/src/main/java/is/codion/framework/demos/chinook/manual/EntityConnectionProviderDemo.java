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
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.chinook.manual;

import is.codion.common.db.connection.DatabaseConnection;
import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.http.HttpEntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.db.rmi.RemoteEntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook;
import is.codion.framework.demos.chinook.domain.Chinook.Track;
import is.codion.framework.demos.chinook.domain.impl.ChinookImpl;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;

import java.sql.Connection;

public final class EntityConnectionProviderDemo {

  static void localConnectionProvider() {
    // tag::local[]
    Database.DATABASE_URL.set("jdbc:h2:mem:h2db");
    Database.DATABASE_INIT_SCRIPTS.set("src/main/sql/create_schema.sql");

    LocalEntityConnectionProvider connectionProvider =
            LocalEntityConnectionProvider.builder()
                    .domain(new ChinookImpl())
                    .user(User.parse("scott:tiger"))
                    .build();

    LocalEntityConnection entityConnection =
            connectionProvider.connection();

    DatabaseConnection databaseConnection =
            entityConnection.databaseConnection();

    // the underlying JDBC connection is available in a local connection
    Connection connection = databaseConnection.getConnection();

    connectionProvider.close();
    // end::local[]
  }

  static void remoteConnectionProvider() throws DatabaseException {
    // tag::remote[]
    RemoteEntityConnectionProvider connectionProvider =
            RemoteEntityConnectionProvider.builder()
                    .domainType(Chinook.DOMAIN)
                    .user(User.parse("scott:tiger"))
                    .clientTypeId(EntityConnectionProviderDemo.class.getSimpleName())
                    .hostName("localhost")
                    .registryPort(1099)
                    .build();

    EntityConnection entityConnection =
            connectionProvider.connection();

    Entities entities = entityConnection.entities();

    Entity track = entityConnection.select(entities.primaryKey(Track.TYPE, 42L));

    connectionProvider.close();
    // end::remote[]
  }

  static void httpConnectionProvider() throws DatabaseException {
    // tag::http[]
    HttpEntityConnectionProvider connectionProvider =
            HttpEntityConnectionProvider.builder()
                    .domainType(Chinook.DOMAIN)
                    .clientTypeId(EntityConnectionProviderDemo.class.getSimpleName())
                    .user(User.parse("scott:tiger"))
                    .hostName("localhost")
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
