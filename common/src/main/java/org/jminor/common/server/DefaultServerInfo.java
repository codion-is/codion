/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server;

import org.jminor.common.Util;
import org.jminor.common.Version;

import java.io.Serializable;
import java.util.UUID;

public final class DefaultServerInfo implements ServerInfo, Serializable {
  private static final long serialVersionUID = 1;

  private final UUID serverID;
  private final String serverName;
  private final int serverPort;
  private final long serverStartupTime;
  private final Version serverVersion = Util.getVersion();

  DefaultServerInfo(final UUID serverID, final String serverName, final int serverPort, final long serverStartupTime) {
    this.serverID = serverID;
    this.serverName = serverName;
    this.serverPort = serverPort;
    this.serverStartupTime = serverStartupTime;
  }

  @Override
  public String getServerName() {
    return serverName;
  }

  @Override
  public UUID getServerID() {
    return serverID;
  }

  @Override
  public int getServerPort() {
    return serverPort;
  }

  @Override
  public Version getServerVersion() {
    return serverVersion;
  }

  @Override
  public long getStartTime() {
    return serverStartupTime;
  }
}
