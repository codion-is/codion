/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server;

import org.jminor.common.Version;

import java.util.UUID;

/**
 * Encapsulates static server information
 */
public interface ServerInfo {
  /**
   * @return the server name
   */
  String getServerName();

  /**
   * @return a unique identifier for this server
   */
  UUID getServerID();

  /**
   * @return the server Version
   */
  Version getServerVersion();

  /**
   * @return the server port
   */
  int getServerPort();

  /**
   * @return the time of server startup
   */
  long getStartTime();
}
