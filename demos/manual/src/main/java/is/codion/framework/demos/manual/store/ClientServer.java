/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.http.server.HttpServerConfiguration;
import is.codion.dbms.h2database.H2DatabaseFactory;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.http.HttpEntityConnectionProvider;
import is.codion.framework.db.rmi.RemoteEntityConnectionProvider;
import is.codion.framework.demos.manual.store.domain.Store;
import is.codion.framework.demos.manual.store.domain.Store.Customer;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.server.EntityServer;
import is.codion.framework.server.EntityServerConfiguration;
import is.codion.framework.servlet.EntityServletServerFactory;

import java.rmi.RemoteException;
import java.util.List;

import static is.codion.common.user.User.parse;
import static is.codion.framework.db.condition.Condition.condition;
import static java.util.Collections.singletonList;

public final class ClientServer {

  private static final int SERVER_PORT = 2223;
  private static final int REGISTRY_PORT = 1099;
  private static final int HTTP_PORT = 8080;

  private static void runServer() throws RemoteException, DatabaseException {
    // tag::runServer[]
    Database database = new H2DatabaseFactory()
            .createDatabase("jdbc:h2:mem:testdb",
                    "src/main/sql/create_schema.sql");

    EntityServerConfiguration configuration = EntityServerConfiguration.builder(SERVER_PORT, REGISTRY_PORT)
            .domainModelClassNames(singletonList(Store.class.getName()))
            .database(database)
            .sslEnabled(false)
            .build();

    EntityServer server = EntityServer.startServer(configuration);

    RemoteEntityConnectionProvider connectionProvider =
            RemoteEntityConnectionProvider.builder()
                    .serverHostName("localhost")
                    .serverPort(SERVER_PORT)
                    .registryPort(REGISTRY_PORT)
                    .domainClassName(Store.class.getName())
                    .user(parse("scott:tiger"))
                    .clientTypeId("ClientServer")
                    .build();

    EntityConnection connection = connectionProvider.connection();

    List<Entity> customers = connection.select(condition(Customer.TYPE));
    customers.forEach(System.out::println);

    connection.close();

    server.shutdown();
    // end::runServer[]
  }

  private static void runServerWithHttp() throws RemoteException, DatabaseException {
    // tag::runServerWithHttp[]
    Database database = new H2DatabaseFactory()
            .createDatabase("jdbc:h2:mem:testdb",
                    "src/main/sql/create_schema.sql");

    HttpServerConfiguration.HTTP_SERVER_PORT.set(HTTP_PORT);
    HttpServerConfiguration.HTTP_SERVER_SECURE.set(false);

    EntityServerConfiguration configuration = EntityServerConfiguration.builder(SERVER_PORT, REGISTRY_PORT)
            .domainModelClassNames(singletonList(Store.class.getName()))
            .database(database)
            .sslEnabled(false)
            .auxiliaryServerFactoryClassNames(singletonList(EntityServletServerFactory.class.getName()))
            .build();

    EntityServer server = EntityServer.startServer(configuration);

    HttpEntityConnectionProvider connectionProvider =
            HttpEntityConnectionProvider.builder()
                    .serverHostName("localhost")
                    .serverPort(HTTP_PORT)
                    .https(false)
                    .domainClassName(Store.class.getName())
                    .user(parse("scott:tiger"))
                    .clientTypeId("ClientServer")
                    .build();

    EntityConnection connection = connectionProvider.connection();

    List<Entity> customers = connection.select(condition(Customer.TYPE));
    customers.forEach(System.out::println);

    connection.close();

    server.shutdown();
    // end::runServerWithHttp[]
  }

  public static void main(String[] args) throws RemoteException, DatabaseException {
    runServer();
    runServerWithHttp();
  }
}
