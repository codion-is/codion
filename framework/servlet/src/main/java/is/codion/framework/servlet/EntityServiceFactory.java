/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.servlet;

import is.codion.common.rmi.server.AuxiliaryServerFactory;
import is.codion.common.rmi.server.Server;
import is.codion.common.rmi.server.ServerAdmin;
import is.codion.framework.db.rmi.RemoteEntityConnection;

/**
 * Provides a {@link EntityService} auxiliary server instance.
 */
public final class EntityServiceFactory implements AuxiliaryServerFactory<RemoteEntityConnection, ServerAdmin, EntityService> {

  @Override
  public EntityService createServer(Server<RemoteEntityConnection, ServerAdmin> server) {
    return new EntityService(server);
  }
}
