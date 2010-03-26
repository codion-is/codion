/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.provider;

import org.jminor.framework.db.EntityDb;

/**
 * Interface for a class responsible for providing EntityDb objects
 * User: darri
 * Date: 9.5.2005
 * Time: 11:39:13
 */
public interface EntityDbProvider {

  /**
   * Provides a EntityDb object, is responsible for returning a healthy EntityDb object,
   * that is, it must reconnect an invalid connection whether remotely or locally
   * @return EntityDb
   */
  EntityDb getEntityDb();

  /**
   * Logs out, disconnects and performs cleanup if required
   */
  void disconnect();
}
