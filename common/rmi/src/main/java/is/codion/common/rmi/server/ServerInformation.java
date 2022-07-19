/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
  String getServerName();

  /**
   * @return a unique identifier for this server
   */
  UUID getServerId();

  /**
   * @return the server framework Version
   */
  Version getServerVersion();

  /**
   * @return the server port
   */
  int getServerPort();

  /**
   * @return the time of server startup
   */
  ZonedDateTime getStartTime();

  /**
   * @return the server locale
   */
  Locale getLocale();

  /**
   * @return the server time zone
   */
  ZoneId getTimeZone();
}
