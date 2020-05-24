/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.servlet;

import is.codion.common.rmi.server.AuxiliaryServerFactory;
import is.codion.common.rmi.server.Server;

/**
 * Provides a {@link EntityServletServer} auxiliary server instance.
 */
public final class EntityServletServerFactory implements AuxiliaryServerFactory<EntityServletServer> {

  @Override
  public EntityServletServer createServer(final Server server) {
    return new EntityServletServer(server);
  }
}
