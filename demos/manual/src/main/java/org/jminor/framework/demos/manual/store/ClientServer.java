/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.manual.store;

import org.jminor.common.db.database.Database;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.http.server.HttpServerConfiguration;
import org.jminor.dbms.h2database.H2DatabaseProvider;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.http.HttpEntityConnectionProvider;
import org.jminor.framework.db.rmi.RemoteEntityConnectionProvider;
import org.jminor.framework.demos.manual.store.domain.Store;
import org.jminor.framework.domain.entity.Entity;
import org.jminor.framework.server.EntityServer;
import org.jminor.framework.server.EntityServerConfiguration;
import org.jminor.framework.servlet.EntityServletServerProvider;

import java.rmi.RemoteException;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.jminor.common.user.Users.parseUser;
import static org.jminor.framework.db.condition.Conditions.selectCondition;

public final class ClientServer {

  private static final int SERVER_PORT = 2223;
  private static final int REGISTRY_PORT = 1099;
  private static final int HTTP_PORT = 8080;

  private static void runServer() throws RemoteException, DatabaseException {
    // tag::runServer[]
    Database database = new H2DatabaseProvider().createDatabase("jdbc:h2:mem:testdb", "src/main/sql/create_schema.sql");

    EntityServerConfiguration configuration = EntityServerConfiguration.configuration(SERVER_PORT, REGISTRY_PORT);
    configuration.setDomainModelClassNames(singletonList(Store.class.getName()));
    configuration.setDatabase(database);
    configuration.setSslEnabled(false);

    EntityServer server = EntityServer.startServer(configuration);

    RemoteEntityConnectionProvider connectionProvider =
            new RemoteEntityConnectionProvider("localhost", SERVER_PORT, REGISTRY_PORT);
    connectionProvider.setDomainClassName(Store.class.getName());
    connectionProvider.setUser(parseUser("scott:tiger"));
    connectionProvider.setClientTypeId("ClientServer");

    EntityConnection connection = connectionProvider.getConnection();

    List<Entity> customers = connection.select(selectCondition(Store.T_CUSTOMER));
    customers.forEach(System.out::println);

    connection.disconnect();

    server.shutdown();
    // end::runServer[]
  }

  private static void runServerWithHttp() throws RemoteException, DatabaseException {
    // tag::runServerWithHttp[]
    Database database = new H2DatabaseProvider().createDatabase("jdbc:h2:mem:testdb", "src/main/sql/create_schema.sql");

    EntityServerConfiguration configuration = EntityServerConfiguration.configuration(SERVER_PORT, REGISTRY_PORT);
    configuration.setDomainModelClassNames(singletonList(Store.class.getName()));
    configuration.setDatabase(database);
    configuration.setSslEnabled(false);
    configuration.setAuxiliaryServerProviderClassNames(singletonList(EntityServletServerProvider.class.getName()));

    HttpServerConfiguration.HTTP_SERVER_PORT.set(HTTP_PORT);
    HttpServerConfiguration.HTTP_SERVER_SECURE.set(false);

    EntityServer server = EntityServer.startServer(configuration);

    HttpEntityConnectionProvider connectionProvider =
            new HttpEntityConnectionProvider("localhost", HTTP_PORT, /*https*/false);
    connectionProvider.setDomainClassName(Store.class.getName());
    connectionProvider.setUser(parseUser("scott:tiger"));
    connectionProvider.setClientTypeId("ClientServer");

    EntityConnection connection = connectionProvider.getConnection();

    List<Entity> customers = connection.select(selectCondition(Store.T_CUSTOMER));
    customers.forEach(System.out::println);

    connection.disconnect();

    server.shutdown();
    // end::runServerWithHttp[]
  }

  public static void main(String[] args) throws RemoteException, DatabaseException {
    runServer();
    runServerWithHttp();
  }
}
