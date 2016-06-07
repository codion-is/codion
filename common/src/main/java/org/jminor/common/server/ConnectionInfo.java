/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server;

import org.jminor.common.User;
import org.jminor.common.Version;

import java.util.UUID;

/**
 * Encapsulates information about a client required by a server for establishing a connection
 */
public interface ConnectionInfo {

  /**
   * @return the user
   */
  User getUser();

  /**
   * @return the client id
   */
  UUID getClientID();

  /**
   * @return the client type id
   */
  String getClientTypeID();

  /**
   * @return the client version
   */
  Version getClientVersion();

  /**
   * @return the version of JMinor the client is using
   */
  Version getFrameworkVersion();
}
