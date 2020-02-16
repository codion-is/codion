/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.server;

import org.jminor.common.User;
import org.jminor.common.db.Database;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.remote.RemoteClient;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.server.AbstractRemoteEntityConnection;
import org.jminor.framework.server.DefaultEntityConnectionServer;

import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.util.List;

import static org.jminor.framework.db.condition.Conditions.entitySelectCondition;

public final class EmployeeServer extends DefaultEntityConnectionServer {

  private static final Domain DOMAIN = new EmpDept().registerDomain();

  public EmployeeServer(final Database database, final int serverPort, final int serverAdminPort,
                        final int registryPort) throws RemoteException {
    super("Employee Server", serverPort, serverAdminPort, registryPort, database,
            false, -1, null, null,
            null, null, null, true,
            600000, null, User.parseUser("scott:tiger"));
  }

  @Override
  protected DefaultEmployeeService createRemoteConnection(final ConnectionPool connectionPool, final Database database,
                                                          final RemoteClient remoteClient, final int port,
                                                          final RMIClientSocketFactory clientSocketFactory,
                                                          final RMIServerSocketFactory serverSocketFactory)
          throws RemoteException, DatabaseException {
    return new DefaultEmployeeService(database, remoteClient, port);
  }

  static final class DefaultEmployeeService extends AbstractRemoteEntityConnection
          implements EmployeeService {

    private DefaultEmployeeService(final Database database, final RemoteClient remoteClient, final int port)
            throws DatabaseException, RemoteException {
      super(DOMAIN, null, database, remoteClient, port, null, null);
    }

    @Override
    public List<Entity> getEmployees() throws DatabaseException {
      synchronized (connectionProxy) {
        return connectionProxy.select(entitySelectCondition(EmpDept.T_EMPLOYEE));
      }
    }
  }
}
