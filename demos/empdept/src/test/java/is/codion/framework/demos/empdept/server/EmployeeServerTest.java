/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.server;

import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.db.exception.DatabaseException;
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

import static is.codion.common.rmi.client.ConnectionRequest.connectionRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class EmployeeServerTest {

  public static final int REGISTRY_PORT = 3221;
  public static final int SERVER_PORT = 3223;
  public static final int SERVER_ADMIN_PORT = 3224;

  @Test
  public void test() throws RemoteException, NotBoundException, LoginException,
          ConnectionNotAvailableException, ConnectionValidationException, DatabaseException {
    ServerConfiguration.RMI_SERVER_HOSTNAME.set("localhost");

    final EntityServerConfiguration configuration = EntityServerConfiguration.configuration(SERVER_PORT, REGISTRY_PORT);
    configuration.setServerAdminPort(SERVER_ADMIN_PORT);
    configuration.setDatabase(DatabaseFactory.getDatabase());
    configuration.setConnectionTimeout(60_000);
    configuration.setAdminUser(User.parseUser("scott:tiger"));
    configuration.setSslEnabled(false);
    configuration.setServerName("Employee Server");

    final EmployeeServer employeeServer = new EmployeeServer(configuration);

    final Server<EmployeeService, ?> remoteServer = Server.Locator.locator().getServer("localhost",
            "Employee Server", REGISTRY_PORT, SERVER_PORT);

    final UUID clientId = UUID.randomUUID();
    final EmployeeService employeeService = remoteServer.connect(
            connectionRequest(User.parseUser("scott:tiger"), clientId, "EmployeeServerTest"));

    final List<Entity> employees = employeeService.getEmployees();
    assertEquals(16, employees.size());

    final List<Employee> employeeBeans = employeeService.getEmployeeBeans();
    assertEquals(16, employeeBeans.size());

    employeeServer.disconnect(clientId);

    employeeServer.shutdown();
  }
}
