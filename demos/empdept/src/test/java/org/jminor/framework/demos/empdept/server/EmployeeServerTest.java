/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.server;

import org.jminor.common.db.database.Databases;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.remote.client.Clients;
import org.jminor.common.remote.server.Server;
import org.jminor.common.remote.server.Servers;
import org.jminor.common.remote.server.exception.ConnectionNotAvailableException;
import org.jminor.common.remote.server.exception.ConnectionValidationException;
import org.jminor.common.remote.server.exception.LoginException;
import org.jminor.common.user.Users;
import org.jminor.framework.domain.entity.Entity;
import org.jminor.framework.server.ServerConfiguration;

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
    Server.RMI_SERVER_HOSTNAME.set("localhost");

    final ServerConfiguration configuration = new ServerConfiguration()
            .port(SERVER_PORT).adminPort(SERVER_ADMIN_PORT).registryPort(REGISTRY_PORT)
            .sslEnabled(false).database(Databases.getInstance()).connectionTimeout(60_000)
            .adminUser(Users.parseUser("scott:tiger"));

    final EmployeeServer employeeServer = new EmployeeServer(configuration);

    final Server<EmployeeService, Remote> remoteServer = Servers.getServer("localhost",
            "Employee Server", REGISTRY_PORT, SERVER_PORT);

    final EmployeeService employeeService = remoteServer.connect(
            Clients.connectionRequest(Users.parseUser("scott:tiger"),
                    UUID.randomUUID(), "EmployeeServerTest"));

    final List<Entity> employees = employeeService.getEmployees();
    assertEquals(16, employees.size());

    employeeService.disconnect();

    employeeServer.shutdown();
  }
}
