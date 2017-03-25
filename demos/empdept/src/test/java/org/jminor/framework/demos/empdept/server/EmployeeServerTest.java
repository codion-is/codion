/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.server;

import org.jminor.common.User;
import org.jminor.common.db.Database;
import org.jminor.common.db.Databases;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.server.ClientUtil;
import org.jminor.common.server.Server;
import org.jminor.common.server.ServerException;
import org.jminor.common.server.ServerUtil;
import org.jminor.framework.Configuration;
import org.jminor.framework.domain.Entity;

import org.junit.Test;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public final class EmployeeServerTest {

  public static final int REGISTRY_PORT = 2221;
  public static final int SERVER_PORT = 2223;

  @Test
  public void test() throws RemoteException, NotBoundException, ServerException.LoginException,
          ServerException.ServerFullException, ServerException.ConnectionValidationException, DatabaseException {
    Configuration.setValue("java.rmi.server.hostname", "localhost");

    final Database database = Databases.createInstance();
    final EmployeeServer employeeServer = new EmployeeServer(database, SERVER_PORT, SERVER_PORT, REGISTRY_PORT);

    final Server<EmployeeService, Remote> remoteServer = ServerUtil.getServer("localhost",
            "Employee Server", REGISTRY_PORT, SERVER_PORT);

    final EmployeeService employeeService = remoteServer.connect(
            ClientUtil.connectionInfo(new User("scott", "tiger"),
                    UUID.randomUUID(), "EmployeeServerTest"));

    final List<Entity> employees = employeeService.getEmployees();
    assertEquals(16, employees.size());

    employeeService.disconnect();

    employeeServer.shutdown();
  }
}
