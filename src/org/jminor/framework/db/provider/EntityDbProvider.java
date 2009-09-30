/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.provider;

import org.jminor.common.model.UserException;
import org.jminor.framework.db.EntityDb;

/**
 * Interface for a class responsible for providing EntityDb objects
 * User: darri
 * Date: 9.5.2005
 * Time: 11:39:13
 */
public interface EntityDbProvider {

  /**
   * Provides a EntityDb object, is responsible for returning a
   * healthy EntityDb object, that is, it must perform all
   * reconnections whether remotely or locally
   * @return EntityDb
   * @throws UserException in case of a problem
   */
  public EntityDb getEntityDb() throws UserException;

  /**
   * Logs out, disconnects and performs cleanup if required
   * @throws UserException in case of an exception
   */
  public void disconnect() throws UserException;
}
