/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.empdept.server;

import dev.codion.common.db.database.Database;
import dev.codion.common.db.exception.DatabaseException;
import dev.codion.common.rmi.server.RemoteClient;
import dev.codion.framework.demos.empdept.domain.EmpDept;
import dev.codion.framework.demos.empdept.domain.Employee;
import dev.codion.framework.domain.Domain;
import dev.codion.framework.domain.entity.Entity;
import dev.codion.framework.server.AbstractRemoteEntityConnection;
import dev.codion.framework.server.EntityServer;
import dev.codion.framework.server.EntityServerConfiguration;

import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.util.List;

import static dev.codion.framework.db.condition.Conditions.selectCondition;

public final class EmployeeServer extends EntityServer {

  private final Domain domain = new EmpDept();

  public EmployeeServer(final EntityServerConfiguration configuration) throws RemoteException {
    super(configuration);
    domain.registerEntities();
  }

  @Override
  protected AbstractRemoteEntityConnection createRemoteConnection(final Database database,
                                                                  final RemoteClient remoteClient, final int port,
                                                                  final RMIClientSocketFactory clientSocketFactory,
                                                                  final RMIServerSocketFactory serverSocketFactory)
          throws RemoteException, DatabaseException {
    return new DefaultEmployeeService(domain, database, remoteClient, port);
  }

  static final class DefaultEmployeeService extends AbstractRemoteEntityConnection implements EmployeeService {

    private DefaultEmployeeService(final Domain domain, final Database database, final RemoteClient remoteClient, final int port)
            throws DatabaseException, RemoteException {
      super(domain, database, remoteClient, port, null, null);
    }

    @Override
    public List<Entity> getEmployees() throws DatabaseException {
      synchronized (connectionProxy) {
        return connectionProxy.select(selectCondition(EmpDept.T_EMPLOYEE));
      }
    }

    @Override
    public List<Employee> getEmployeeBeans() throws RemoteException, DatabaseException {
      synchronized (connectionProxy) {
        final List<Entity> employees = connectionProxy.select(selectCondition(EmpDept.T_EMPLOYEE).setForeignKeyFetchDepth(-1));

        return connectionProxy.getEntities().toBeans(employees);
      }
    }
  }
}
