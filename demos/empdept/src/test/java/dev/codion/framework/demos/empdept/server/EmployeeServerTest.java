/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.empdept.server;

import dev.codion.common.db.database.Databases;
import dev.codion.common.db.exception.DatabaseException;
import dev.codion.common.rmi.server.Server;
import dev.codion.common.rmi.server.ServerConfiguration;
import dev.codion.common.rmi.server.Servers;
import dev.codion.common.rmi.server.exception.ConnectionNotAvailableException;
import dev.codion.common.rmi.server.exception.ConnectionValidationException;
import dev.codion.common.rmi.server.exception.LoginException;
import dev.codion.common.user.Users;
import dev.codion.framework.demos.empdept.domain.Employee;
import dev.codion.framework.domain.entity.Entity;
import dev.codion.framework.server.EntityServerConfiguration;

import org.junit.jupiter.api.Test;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.UUID;

import static dev.codion.common.rmi.client.ConnectionRequest.connectionRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class EmployeeServerTest {

  public static final int REGISTRY_PORT = 2221;
  public static final int SERVER_PORT = 2223;
  public static final int SERVER_ADMIN_PORT = 2224;

  @Test
  public void test() throws RemoteException, NotBoundException, LoginException,
          ConnectionNotAvailableException, ConnectionValidationException, DatabaseException {
    ServerConfiguration.RMI_SERVER_HOSTNAME.set("localhost");

    final EntityServerConfiguration configuration = EntityServerConfiguration.configuration(SERVER_PORT, REGISTRY_PORT);
    configuration.setAdminPort(SERVER_ADMIN_PORT);
    configuration.setDatabase(Databases.getInstance());
    configuration.setConnectionTimeout(60_000);
    configuration.setAdminUser(Users.parseUser("scott:tiger"));
    configuration.setSslEnabled(false);
    configuration.setServerName("Employee Server");

    final EmployeeServer employeeServer = new EmployeeServer(configuration);

    final Server<EmployeeService, Remote> remoteServer = Servers.getServer("localhost",
            "Employee Server", REGISTRY_PORT, SERVER_PORT);

    final UUID clientId = UUID.randomUUID();
    final EmployeeService employeeService = remoteServer.connect(
            connectionRequest(Users.parseUser("scott:tiger"), clientId, "EmployeeServerTest"));

    final List<Entity> employees = employeeService.getEmployees();
    assertEquals(16, employees.size());

    final List<Employee> employeeBeans = employeeService.getEmployeeBeans();
    assertEquals(16, employeeBeans.size());

    employeeServer.disconnect(clientId);

    employeeServer.shutdown();
  }
}
