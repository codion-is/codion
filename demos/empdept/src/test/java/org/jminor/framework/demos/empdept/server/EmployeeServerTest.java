/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.server;

import org.jminor.common.db.database.Databases;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.remote.client.ConnectionRequest;
import org.jminor.common.remote.server.Server;
import org.jminor.common.remote.server.ServerConfiguration;
import org.jminor.common.remote.server.Servers;
import org.jminor.common.remote.server.exception.ConnectionNotAvailableException;
import org.jminor.common.remote.server.exception.ConnectionValidationException;
import org.jminor.common.remote.server.exception.LoginException;
import org.jminor.common.user.Users;
import org.jminor.framework.domain.entity.Entity;
import org.jminor.framework.server.EntityConnectionServerConfiguration;

import org.junit.jupiter.api.Test;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class EmployeeServerTest {

  public static final int REGISTRY_PORT = 2221;
  public static final int SERVER_PORT = 2223;
  public static final int SERVER_ADMIN_PORT = 2224;

  @Test
  public void test() throws RemoteException, NotBoundException, LoginException,
          ConnectionNotAvailableException, ConnectionValidationException, DatabaseException {
    ServerConfiguration.RMI_SERVER_HOSTNAME.set("localhost");

    final ServerConfiguration serverConfiguration = ServerConfiguration.configuration(SERVER_PORT).setServerName("Employee Server");
    serverConfiguration.setSslEnabled(false);
    final EntityConnectionServerConfiguration configuration = EntityConnectionServerConfiguration.configuration(serverConfiguration, REGISTRY_PORT)
            .setAdminPort(SERVER_ADMIN_PORT).setDatabase(Databases.getInstance()).setConnectionTimeout(60_000)
            .setAdminUser(Users.parseUser("scott:tiger"));

    final EmployeeServer employeeServer = new EmployeeServer(configuration);

    final Server<EmployeeService, Remote> remoteServer = Servers.getServer("localhost",
            "Employee Server", REGISTRY_PORT, SERVER_PORT);

    final EmployeeService employeeService = remoteServer.connect(
            ConnectionRequest.connectionRequest(Users.parseUser("scott:tiger"),
                    UUID.randomUUID(), "EmployeeServerTest"));

    final List<Entity> employees = employeeService.getEmployees();
    assertEquals(16, employees.size());

    employeeService.disconnect();

    employeeServer.shutdown();
  }
}
