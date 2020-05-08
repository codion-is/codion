/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.manual;

import org.jminor.common.db.connection.DatabaseConnection;
import org.jminor.common.db.database.Database;
import org.jminor.common.db.database.Databases;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.user.Users;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.http.HttpEntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnection;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.db.rmi.RemoteEntityConnectionProvider;
import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.framework.demos.chinook.domain.impl.ChinookImpl;
import org.jminor.framework.domain.entity.Entities;
import org.jminor.framework.domain.entity.Entity;

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

    Entities domain = entityConnection.getDomain();

    Entity track = entityConnection.selectSingle(domain.key(Chinook.T_TRACK, 42L));

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

    Entities domain = entityConnection.getDomain();

    entityConnection.selectSingle(domain.key(Chinook.T_TRACK, 42L));

    connectionProvider.disconnect();
    // end::http[]
  }
}
