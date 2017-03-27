/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.server;

import org.jminor.common.User;
import org.jminor.common.db.Database;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.server.ClientInfo;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.server.AbstractEntityConnectionServer;
import org.jminor.framework.server.AbstractRemoteEntityConnection;

import java.rmi.RemoteException;
import java.util.List;

public final class EmployeeServer extends AbstractEntityConnectionServer {

  public EmployeeServer(final Database database, final int serverPort, final int serverAdminPort,
                        final int registryPort) throws RemoteException {
    super("Employee Server", serverPort, serverAdminPort, registryPort, database,
            false, -1, null, null,
            null, null, null,
            null, true, 600000,
            null, new User("scott", "tiger"));
  }

  @Override
  protected DefaultEmployeeService createRemoteConnection(final ConnectionPool connectionPool,
                                                          final Database database, final ClientInfo clientInfo,
                                                          final int port, final boolean clientLoggingEnabled,
                                                          final boolean sslEnabled)
          throws RemoteException, DatabaseException {
    return new DefaultEmployeeService(database, clientInfo, port, clientLoggingEnabled, sslEnabled);
  }

  static final class DefaultEmployeeService extends AbstractRemoteEntityConnection
          implements EmployeeService {

    static {
      EmpDept.init();
    }

    private DefaultEmployeeService(final Database database, final ClientInfo clientInfo, final int port,
                                  final boolean loggingEnabled, final boolean sslEnabled)
            throws DatabaseException, RemoteException {
      super(null, database, clientInfo, port, loggingEnabled, sslEnabled);
    }

    @Override
    public List<Entity> getEmployees() throws DatabaseException {
      synchronized (connectionProxy) {
        return connectionProxy.selectMany(EntityConditions.selectCondition(EmpDept.T_EMPLOYEE));
      }
    }
  }
}
