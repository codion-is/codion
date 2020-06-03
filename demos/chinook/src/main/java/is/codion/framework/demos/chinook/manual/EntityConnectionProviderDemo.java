/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.manual;

import is.codion.common.db.connection.DatabaseConnection;
import is.codion.common.db.database.Database;
import is.codion.common.db.database.Databases;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.Users;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.http.ClientHttps;
import is.codion.framework.db.http.HttpEntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.db.rmi.RemoteEntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook.Track;
import is.codion.framework.demos.chinook.domain.impl.ChinookImpl;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;

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

    Entity track = entityConnection.selectSingle(entities.key(Track.TYPE, 42L));

    connectionProvider.disconnect();
    // end::remote[]
  }

  static void httpConnectionProvider() throws DatabaseException {
    // tag::http[]
    HttpEntityConnectionProvider connectionProvider =
            new HttpEntityConnectionProvider("localhost", 8080, ClientHttps.FALSE);

    connectionProvider.setDomainClassName(ChinookImpl.class.getName());
    connectionProvider.setClientTypeId(EntityConnectionProviderDemo.class.getSimpleName());
    connectionProvider.setUser(Users.parseUser("scott:tiger"));

    EntityConnection entityConnection = connectionProvider.getConnection();

    Entities entities = entityConnection.getEntities();

    entityConnection.selectSingle(entities.key(Track.TYPE, 42L));

    connectionProvider.disconnect();
    // end::http[]
  }
}
