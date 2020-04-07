/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.remote.Server;

public final class TestWebServer implements Server.AuxiliaryServer {

  public TestWebServer() {}

  @Override
  public void setServer(final Server server) {}

  @Override
  public void startServer() throws Exception {}

  @Override
  public void stopServer() throws Exception {}
}
