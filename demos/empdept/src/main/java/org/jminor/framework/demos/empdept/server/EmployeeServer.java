/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.server;

import org.jminor.common.db.database.Database;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.rmi.server.RemoteClient;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.demos.empdept.domain.Employee;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.entity.Entity;
import org.jminor.framework.server.AbstractRemoteEntityConnection;
import org.jminor.framework.server.EntityServer;
import org.jminor.framework.server.EntityServerConfiguration;

import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.util.List;

import static org.jminor.framework.db.condition.Conditions.selectCondition;

public final class EmployeeServer extends EntityServer {

  private final Domain domain = new EmpDept();

  public EmployeeServer(final EntityServerConfiguration configuration) throws RemoteException {
    super(configuration);
    domain.getEntities().register();
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
        final List<Entity> employees = connectionProxy.select(selectCondition(EmpDept.T_EMPLOYEE).setForeignKeyFetchDepthLimit(-1));

        return connectionProxy.getEntities().toBeans(employees);
      }
    }
  }
}
