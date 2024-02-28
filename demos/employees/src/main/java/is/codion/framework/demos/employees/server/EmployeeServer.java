/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.employees.server;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.rmi.server.RemoteClient;
import is.codion.framework.demos.employees.domain.Employees;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.server.AbstractRemoteEntityConnection;
import is.codion.framework.server.EntityServer;
import is.codion.framework.server.EntityServerConfiguration;

import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.util.Collection;

import static is.codion.framework.demos.employees.domain.Employees.Employee;
import static is.codion.framework.domain.entity.condition.Condition.all;

public final class EmployeeServer extends EntityServer {

  private final Domain domain = new Employees();

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
    public Collection<Entity> employees() throws RemoteException, DatabaseException {
      synchronized (connectionProxy) {
        return connectionProxy.select(all(Employee.TYPE));
      }
    }
  }
}
