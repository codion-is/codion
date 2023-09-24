/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson.
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
  private final Version serverVersion = Version.version();

  DefaultServerInformation(UUID serverId, String serverName, int serverPort,
                           ZonedDateTime serverStartTime) {
    this.serverId = serverId;
    this.serverName = serverName;
    this.serverPort = serverPort;
    this.serverStartTime = serverStartTime;
  }

  @Override
  public String serverName() {
    return serverName;
  }

  @Override
  public UUID serverId() {
    return serverId;
  }

  @Override
  public int serverPort() {
    return serverPort;
  }

  @Override
  public Version serverVersion() {
    return serverVersion;
  }

  @Override
  public ZonedDateTime startTime() {
    return serverStartTime;
  }

  @Override
  public Locale locale() {
    return locale;
  }

  @Override
  public ZoneId timeZone() {
    return serverStartTime.getZone();
  }
}
