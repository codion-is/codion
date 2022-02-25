/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.server;

import is.codion.common.version.Version;

import java.io.Serializable;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.UUID;

final class DefaultServerInformation implements ServerInformation, Serializable {

  private static final long serialVersionUID = 1;

  private final UUID serverId;
  private final String serverName;
  private final int serverPort;
  private final ZonedDateTime serverStartTime;
  private final Locale locale = Locale.getDefault();
  private final Version serverVersion = Version.getVersion();

  DefaultServerInformation(UUID serverId, String serverName, int serverPort,
                           ZonedDateTime serverStartTime) {
    this.serverId = serverId;
    this.serverName = serverName;
    this.serverPort = serverPort;
    this.serverStartTime = serverStartTime;
  }

  @Override
  public String getServerName() {
    return serverName;
  }

  @Override
  public UUID getServerId() {
    return serverId;
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
  public ZonedDateTime getStartTime() {
    return serverStartTime;
  }

  @Override
  public Locale getLocale() {
    return locale;
  }

  @Override
  public ZoneId getTimeZone() {
    return serverStartTime.getZone();
  }
}
