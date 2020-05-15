/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.chinook.manual;

import dev.codion.common.db.connection.DatabaseConnection;
import dev.codion.common.db.database.Database;
import dev.codion.common.db.database.Databases;
import dev.codion.common.db.exception.DatabaseException;
import dev.codion.common.user.Users;
import dev.codion.framework.db.EntityConnection;
import dev.codion.framework.db.http.HttpEntityConnectionProvider;
import dev.codion.framework.db.local.LocalEntityConnection;
import dev.codion.framework.db.local.LocalEntityConnectionProvider;
import dev.codion.framework.db.rmi.RemoteEntityConnectionProvider;
import dev.codion.framework.demos.chinook.domain.Chinook;
import dev.codion.framework.demos.chinook.domain.impl.ChinookImpl;
import dev.codion.framework.domain.entity.Entities;
import dev.codion.framework.domain.entity.Entity;

import java.sql.Connection;

public class EntityConnectionProviderDemo {

  static void localConnectionProvider() {
    // tag::local[]
    Database.DATABASE_URL.set("jdbc:h2:mem:h2db");
    Database.DATABASE_INIT_SCRIPT.set("src/main/sql/create_schema.sql");

    LocalEntityConnectionProvider connectionProvider =
            new LocalEntityConnectionProvider(Databases.getInstance());

    connectionProvider.setDomainClassName(ChinookImpl.class.getName());
    connectionProvider.setUser(Users.parseUser("scott:tiger"));

    LocalEntityConnection entityConnection =
            (LocalEntityConnection) connectionProvider.getConnection();

    DatabaseConnection databaseConnection =
            entityConnection.getDatabaseConnection();

    //the underlying JDBC connection is available in a local connection
    Connection connection = databaseConnection.getConnection();

    connectionProvider.disconnect();
    // end::local[]
  }

  static void remoteConnectionProvider() throws DatabaseException {
    // tag::remote[]
    RemoteEntityConnectionProvider connectionProvider =
            new RemoteEntityConnectionProvider("localhost", -1, 1099);

    connectionProvider.setDomainClassName(ChinookImpl.class.getName());
    connectionProvider.setUser(Users.parseUser("scott:tiger"));
    connectionProvider.setClientTypeId(EntityConnectionProviderDemo.class.getSimpleName());

    EntityConnection entityConnection =
            connectionProvider.getConnection();

    Entities entities = entityConnection.getEntities();

    Entity track = entityConnection.selectSingle(entities.key(Chinook.T_TRACK, 42L));

    connectionProvider.disconnect();
    // end::remote[]
  }

  static void httpConnectionProvider() throws DatabaseException {
    // tag::http[]
    HttpEntityConnectionProvider connectionProvider =
            new HttpEntityConnectionProvider("localhost", 8080, false);

    connectionProvider.setDomainClassName(ChinookImpl.class.getName());
    connectionProvider.setClientTypeId(EntityConnectionProviderDemo.class.getSimpleName());
    connectionProvider.setUser(Users.parseUser("scott:tiger"));

    EntityConnection entityConnection = connectionProvider.getConnection();

    Entities entities = entityConnection.getEntities();

    entityConnection.selectSingle(entities.key(Chinook.T_TRACK, 42L));

    connectionProvider.disconnect();
    // end::http[]
  }
}
