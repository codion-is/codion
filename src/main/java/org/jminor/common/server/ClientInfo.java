/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server;

import org.jminor.common.model.User;

/**
 * Contains basic information about a remote client required by a server
 */
public interface ClientInfo extends ConnectionInfo {

  /**
   * @return the connection info
   */
  ConnectionInfo getConnectionInfo();

  /**
   * @return the user used when connecting to the underlying database
   */
  User getDatabaseUser();

  /**
   * @return the client hostname
   */
  String getClientHost();

  /**
   * @param clientHost the client hostname
   */
  void setClientHost(final String clientHost);
}