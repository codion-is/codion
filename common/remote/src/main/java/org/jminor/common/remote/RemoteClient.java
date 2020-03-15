/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.remote;

import org.jminor.common.user.User;

/**
 * Contains basic information about a remote client
 */
public interface RemoteClient extends ConnectionRequest {

  /**
   * @return the connection request
   */
  ConnectionRequest getConnectionRequest();

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
  void setClientHost(String clientHost);
}