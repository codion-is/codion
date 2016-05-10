/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.StateObserver;
import org.jminor.common.User;

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
   * Returns the Type this connection provider represents
   * @return the Type
   */
  EntityConnection.Type getConnectionType();

  /**
   * @return a short description of the database provider
   */
  String getDescription();

  /**
   * @return the name of the host providing the connection
   */
  String getServerHostName();

  /**
   * @return true if a connection has been established, note that this does not check if the actual
   * connection is healthy, only that one has been established.
   */
  boolean isConnected();

  /**
   * Returns a state which is active when this provider is connected, note that this state is only updated
   * if {@link org.jminor.framework.Configuration#CONNECTION_SCHEDULE_VALIDATION} is set to true
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
