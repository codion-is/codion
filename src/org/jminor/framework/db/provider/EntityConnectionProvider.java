/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.provider;

import org.jminor.common.model.StateObserver;
import org.jminor.common.model.User;
import org.jminor.framework.db.EntityConnection;

/**
 * Interface for a class responsible for providing EntityConnection objects.
 */
public interface EntityConnectionProvider {

  /**
   * Provides a EntityConnection object, is responsible for returning a healthy EntityConnection object,
   * that is, it must reconnect an invalid connection whether remotely or locally
   * @return a EntityConnection instance
   */
  EntityConnection getConnection();

  /**
   * @return a short description of the database provider
   */
  String getDescription();

  /**
   * @return the name of the host providing the connection
   */
  String getHostName();

  /**
   * @return true if a connection has been established
   */
  boolean isConnected();

  /**
   * @return a state active when this provider is connected
   */
  StateObserver getConnectedObserver();

  /**
   * Logs out, disconnects and performs cleanup if required
   */
  void disconnect();

  /**
   * Sets the user for this connection provider, this invalidates and disconnects the previous connection if any.
   * @param user the user
   */
  void setUser(final User user);

  /**
   * @return the user used by this connection provider
   */
  User getUser();
}
