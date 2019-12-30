/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.manual;

import org.jminor.common.User;
import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseConnection;
import org.jminor.common.db.Databases;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.remote.Server;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.http.HttpEntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnection;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.db.remote.RemoteEntityConnectionProvider;
import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.framework.demos.chinook.domain.impl.ChinookImpl;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;

import java.sql.Connection;

public class EntityConnectionProviderDemo {

  // tag::local[]
  static void localConnectionProvider() {
    Database.DATABASE_TYPE.set(Database.Type.H2.toString());
    Database.DATABASE_EMBEDDED_IN_MEMORY.set(true);
    Database.DATABASE_INIT_SCRIPT.set("src/main/sql/create_schema.sql");

    LocalEntityConnectionProvider connectionProvider =
            new LocalEntityConnectionProvider(Databases.getInstance());

    connectionProvider.setDomainClassName(ChinookImpl.class.getName());
    connectionProvider.setUser(User.parseUser("scott:tiger"));

    LocalEntityConnection entityConnection =
            (LocalEntityConnection) connectionProvider.getConnection();

    DatabaseConnection databaseConnection =
            entityConnection.getDatabaseConnection();

    //the underlying JDBC connection is available in a local connection
    Connection connection = databaseConnection.getConnection();

    connectionProvider.disconnect();
  }
  // end::local[]

  // tag::remote[]
  static void remoteConnectionProvider() throws DatabaseException {
    Server.SERVER_HOST_NAME.set("localhost");

    RemoteEntityConnectionProvider connectionProvider =
            new RemoteEntityConnectionProvider();

    connectionProvider.setDomainClassName(ChinookImpl.class.getName());
    connectionProvider.setUser(User.parseUser("scott:tiger"));
    connectionProvider.setClientTypeId(EntityConnectionProviderDemo.class.getSimpleName());

    EntityConnection entityConnection =
            connectionProvider.getConnection();

    Domain domain = entityConnection.getDomain();

    Entity track = entityConnection.selectSingle(domain.key(Chinook.T_TRACK, 42L));

    connectionProvider.disconnect();
  }
  // end::remote[]

  // tag::http[]
  static void httpConnectionProvider() throws DatabaseException {
    HttpEntityConnectionProvider.HTTP_CLIENT_HOST_NAME.set("localhost");

    HttpEntityConnectionProvider connectionProvider =
            new HttpEntityConnectionProvider();

    connectionProvider.setDomainClassName(ChinookImpl.class.getName());
    connectionProvider.setClientTypeId(EntityConnectionProviderDemo.class.getSimpleName());
    connectionProvider.setUser(User.parseUser("scott:tiger"));

    EntityConnection entityConnection = connectionProvider.getConnection();

    Domain domain = entityConnection.getDomain();

    entityConnection.selectSingle(domain.key(Chinook.T_TRACK, 42L));

    connectionProvider.disconnect();
  }
  // end::http[]
}
