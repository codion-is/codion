/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.server;

import org.jminor.common.User;
import org.jminor.common.db.Database;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.server.RemoteClient;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.server.AbstractRemoteEntityConnection;
import org.jminor.framework.server.DefaultEntityConnectionServer;

import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.util.List;

public final class EmployeeServer extends DefaultEntityConnectionServer {

  private static final Entities ENTITIES = new EmpDept().registerDomain();

  public EmployeeServer(final Database database, final int serverPort, final int serverAdminPort,
                        final int registryPort) throws RemoteException {
    super("Employee Server", serverPort, serverAdminPort, registryPort, database,
            false, -1, null, null,
            null, null, null, true,
            600000,null, new User("scott", "tiger".toCharArray()));
  }

  @Override
  protected DefaultEmployeeService createRemoteConnection(final ConnectionPool connectionPool,
                                                          final Database database, final RemoteClient remoteClient,
                                                          final int port, final boolean clientLoggingEnabled,
                                                          final RMIClientSocketFactory clientSocketFactory,
                                                          final RMIServerSocketFactory serverSocketFactory)
          throws RemoteException, DatabaseException {
    return new DefaultEmployeeService(database, remoteClient, port, clientLoggingEnabled);
  }

  static final class DefaultEmployeeService extends AbstractRemoteEntityConnection
          implements EmployeeService {

    private DefaultEmployeeService(final Database database, final RemoteClient remoteClient, final int port,
                                   final boolean loggingEnabled)
            throws DatabaseException, RemoteException {
      super(ENTITIES,null, database, remoteClient, port, loggingEnabled, null, null);
    }

    @Override
    public List<Entity> getEmployees() throws DatabaseException {
      synchronized (connectionProxy) {
        return connectionProxy.selectMany(new EntityConditions(ENTITIES).selectCondition(EmpDept.T_EMPLOYEE));
      }
    }
  }
}
