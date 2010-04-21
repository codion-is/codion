/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.provider;

import org.jminor.framework.db.EntityDb;

/**
 * Interface for a class responsible for providing EntityDb objects.<br>
 * User: darri<br>
 * Date: 9.5.2005<br>
 * Time: 11:39:13<br>
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
   * Logs out, disconnects and performs cleanup if required
   */
  void disconnect();
}
