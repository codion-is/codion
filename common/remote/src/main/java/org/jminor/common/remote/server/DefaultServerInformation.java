/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.remote.server;

import org.jminor.common.version.Version;
import org.jminor.common.version.Versions;

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
  private final ZonedDateTime serverStartupTime;
  private final Locale locale = Locale.getDefault();
  private final Version serverVersion = Versions.getVersion();

  DefaultServerInformation(final UUID serverId, final String serverName, final int serverPort,
                           final ZonedDateTime serverStartupTime) {
    this.serverId = serverId;
    this.serverName = serverName;
    this.serverPort = serverPort;
    this.serverStartupTime = serverStartupTime;
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
    return serverStartupTime;
  }

  @Override
  public Locale getLocale() {
    return locale;
  }

  @Override
  public ZoneId getTimeZone() {
    return serverStartupTime.getZone();
  }
}
