/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.servlet;

import is.codion.common.rmi.server.AuxiliaryServerFactory;
import is.codion.common.rmi.server.Server;
import is.codion.common.rmi.server.ServerAdmin;
import is.codion.framework.db.rmi.RemoteEntityConnection;

/**
 * Provides a {@link EntityServletServer} auxiliary server instance.
 */
public final class EntityServletServerFactory implements AuxiliaryServerFactory<RemoteEntityConnection, ServerAdmin, EntityServletServer> {

  @Override
  public EntityServletServer createServer(Server<RemoteEntityConnection, ServerAdmin> server) {
    return new EntityServletServer(server);
  }
}
