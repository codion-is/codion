/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.provider;

import org.jminor.common.model.User;
import org.jminor.common.model.StateObserver;
import org.jminor.framework.db.EntityDb;

/**
 * Interface for a class responsible for providing EntityDb objects.
 */
public interface EntityDbProvider {

  /**
   * Provides a EntityDb object, is responsible for returning a healthy EntityDb object,
   * that is, it must reconnect an invalid connection whether remotely or locally
   * @return a EntityDb instance
   */
  EntityDb getEntityDb();

  /**
   * @return a short description of the database provider
   */
  String getDescription();

  /**
   * @return true if a connection has been established
   */
  boolean isConnected();

  /**
   * @return a state active when this provider is connected
   */
  StateObserver getConnectedState();

  /**
   * Logs out, disconnects and performs cleanup if required
   */
  void disconnect();

  /**
   * Sets the user for this db provider, this invalidates and disconnects the previous
   * connection if any.
   * @param user the user
   */
  void setUser(final User user);

  /**
   * @return the user used by this db provider
   */
  User getUser();
}
