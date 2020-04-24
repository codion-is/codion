/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.server;

import org.jminor.common.db.database.Database;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.rmi.server.RemoteClient;
import org.jminor.framework.demos.empdept.domain.EmpDept;
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

  private static final Domain DOMAIN = new EmpDept().registerDomain();

  public EmployeeServer(final EntityServerConfiguration configuration) throws RemoteException {
    super(configuration);
  }

  @Override
  protected DefaultEmployeeService createRemoteConnection(final ConnectionPool connectionPool, final Database database,
                                                          final RemoteClient remoteClient, final int port,
                                                          final RMIClientSocketFactory clientSocketFactory,
                                                          final RMIServerSocketFactory serverSocketFactory)
          throws RemoteException, DatabaseException {
    return new DefaultEmployeeService(database, remoteClient, port);
  }

  static final class DefaultEmployeeService extends AbstractRemoteEntityConnection implements EmployeeService {

    private DefaultEmployeeService(final Database database, final RemoteClient remoteClient, final int port)
            throws DatabaseException, RemoteException {
      super(DOMAIN, null, database, remoteClient, port, null, null);
    }

    @Override
    public List<Entity> getEmployees() throws DatabaseException {
      synchronized (connectionProxy) {
        return connectionProxy.select(selectCondition(EmpDept.T_EMPLOYEE));
      }
    }
  }
}
