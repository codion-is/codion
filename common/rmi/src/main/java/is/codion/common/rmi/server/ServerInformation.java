/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.server;

import is.codion.common.version.Version;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.UUID;

/**
 * Encapsulates static server information
 */
public interface ServerInformation {
  /**
   * @return the server name
   */
  String serverName();

  /**
   * @return a unique identifier for this server
   */
  UUID serverId();

  /**
   * @return the server framework Version
   */
  Version serverVersion();

  /**
   * @return the server port
   */
  int serverPort();

  /**
   * @return the time of server startup
   */
  ZonedDateTime startTime();

  /**
   * @return the server locale
   */
  Locale locale();

  /**
   * @return the server time zone
   */
  ZoneId timeZone();
}
