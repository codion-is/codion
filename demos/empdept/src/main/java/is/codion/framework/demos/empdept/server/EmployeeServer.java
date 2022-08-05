/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.server;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.rmi.server.RemoteClient;
import is.codion.framework.demos.empdept.domain.EmpDept;
import is.codion.framework.demos.empdept.domain.EmpDept.Employee;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.server.AbstractRemoteEntityConnection;
import is.codion.framework.server.EntityServer;
import is.codion.framework.server.EntityServerConfiguration;

import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.util.List;

import static is.codion.framework.db.condition.Conditions.condition;

public final class EmployeeServer extends EntityServer {

  private final Domain domain = new EmpDept();

  public EmployeeServer(EntityServerConfiguration configuration) throws RemoteException {
    super(configuration);
  }

  @Override
  protected AbstractRemoteEntityConnection createRemoteConnection(Database database,
                                                                  RemoteClient remoteClient, int port,
                                                                  RMIClientSocketFactory clientSocketFactory,
                                                                  RMIServerSocketFactory serverSocketFactory)
          throws RemoteException, DatabaseException {
    return new DefaultEmployeeService(domain, database, remoteClient, port);
  }

  static final class DefaultEmployeeService extends AbstractRemoteEntityConnection implements EmployeeService {

    private DefaultEmployeeService(Domain domain, Database database, RemoteClient remoteClient, int port)
            throws DatabaseException, RemoteException {
      super(domain, database, remoteClient, port, null, null);
    }

    @Override
    public List<Entity> getEmployees() throws RemoteException, DatabaseException {
      synchronized (connectionProxy) {
        return connectionProxy.select(condition(Employee.TYPE));
      }
    }

    @Override
    public List<Employee> getEmployeeBeans() throws RemoteException, DatabaseException {
      synchronized (connectionProxy) {
        List<Entity> employees = connectionProxy.select(condition(Employee.TYPE)
                .selectBuilder()
                .fetchDepth(-1)
                .build());

        return Entity.castTo(Employee.class, employees);
      }
    }
  }
}
