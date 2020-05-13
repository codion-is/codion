/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.servlet;

import dev.codion.common.rmi.server.AuxiliaryServerProvider;
import dev.codion.common.rmi.server.Server;

/**
 * Provides a {@link EntityServletServer} auxiliary server instance.
 */
public final class EntityServletServerProvider implements AuxiliaryServerProvider<EntityServletServer> {

  @Override
  public EntityServletServer createServer(final Server server) {
    return new EntityServletServer(server);
  }
}
