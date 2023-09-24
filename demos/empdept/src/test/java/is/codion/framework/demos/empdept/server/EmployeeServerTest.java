/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.empdept.server;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.rmi.client.ConnectionRequest;
import is.codion.common.rmi.server.Server;
import is.codion.common.rmi.server.ServerConfiguration;
import is.codion.common.rmi.server.exception.ConnectionNotAvailableException;
import is.codion.common.rmi.server.exception.LoginException;
import is.codion.common.user.User;
import is.codion.framework.demos.empdept.domain.EmpDept.Employee;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.server.EntityServerConfiguration;

import org.junit.jupiter.api.Test;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class EmployeeServerTest {

  public static final int REGISTRY_PORT = 3221;
  public static final int SERVER_PORT = 3223;
  public static final int SERVER_ADMIN_PORT = 3224;

  @Test
  void test() throws RemoteException, NotBoundException, LoginException,
          ConnectionNotAvailableException, DatabaseException {
    ServerConfiguration.RMI_SERVER_HOSTNAME.set("localhost");

    EntityServerConfiguration configuration = EntityServerConfiguration.builder(SERVER_PORT, REGISTRY_PORT)
            .adminPort(SERVER_ADMIN_PORT)
            .database(Database.instance())
            .idleConnectionTimeout(60_000)
            .adminUser(User.parse("scott:tiger"))
            .sslEnabled(false)
            .serverName("Employee Server")
            .build();

    EmployeeServer employeeServer = new EmployeeServer(configuration);

    Server<EmployeeService, ?> remoteServer = Server.Locator.builder()
            .hostName("localhost")
            .namePrefix("Employee Server")
            .registryPort(REGISTRY_PORT)
            .port(SERVER_PORT)
            .build()
            .locateServer();

    UUID clientId = UUID.randomUUID();
    EmployeeService employeeService = remoteServer.connect(ConnectionRequest.builder()
            .user(User.parse("scott:tiger"))
            .clientId(clientId)
            .clientTypeId("EmployeeServerTest")
            .build());

    Collection<Entity> employees = employeeService.employees();
    assertEquals(16, employees.size());

    Collection<Employee> employeeBeans = employeeService.employeeBeans();
    assertEquals(16, employeeBeans.size());

    employeeServer.disconnect(clientId);

    employeeServer.shutdown();
  }
}
