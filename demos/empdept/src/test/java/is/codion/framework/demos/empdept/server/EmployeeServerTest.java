/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.server;

import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.rmi.client.ConnectionRequest;
import is.codion.common.rmi.server.Server;
import is.codion.common.rmi.server.ServerConfiguration;
import is.codion.common.rmi.server.exception.ConnectionNotAvailableException;
import is.codion.common.rmi.server.exception.ConnectionValidationException;
import is.codion.common.rmi.server.exception.LoginException;
import is.codion.common.user.User;
import is.codion.framework.demos.empdept.domain.EmpDept.Employee;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.server.EntityServerConfiguration;

import org.junit.jupiter.api.Test;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class EmployeeServerTest {

  public static final int REGISTRY_PORT = 3221;
  public static final int SERVER_PORT = 3223;
  public static final int SERVER_ADMIN_PORT = 3224;

  @Test
  void test() throws RemoteException, NotBoundException, LoginException,
          ConnectionNotAvailableException, ConnectionValidationException, DatabaseException {
    ServerConfiguration.RMI_SERVER_HOSTNAME.set("localhost");

    EntityServerConfiguration configuration = EntityServerConfiguration.builder(SERVER_PORT, REGISTRY_PORT)
            .adminPort(SERVER_ADMIN_PORT)
            .database(DatabaseFactory.database())
            .idleConnectionTimeout(60_000)
            .adminUser(User.parse("scott:tiger"))
            .sslEnabled(false)
            .serverName("Employee Server")
            .build();

    EmployeeServer employeeServer = new EmployeeServer(configuration);

    Server<EmployeeService, ?> remoteServer = Server.Locator.locator().getServer("localhost",
            "Employee Server", REGISTRY_PORT, SERVER_PORT);

    UUID clientId = UUID.randomUUID();
    EmployeeService employeeService = remoteServer.connect(ConnectionRequest.builder()
            .user(User.parse("scott:tiger"))
            .clientId(clientId)
            .clientTypeId("EmployeeServerTest")
            .build());

    List<Entity> employees = employeeService.employees();
    assertEquals(16, employees.size());

    List<Employee> employeeBeans = employeeService.employeeBeans();
    assertEquals(16, employeeBeans.size());

    employeeServer.disconnect(clientId);

    employeeServer.shutdown();
  }
}
