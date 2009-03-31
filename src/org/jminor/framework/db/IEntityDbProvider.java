/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.model.Event;
import org.jminor.common.model.UserException;

/**
 * Interface for a class responsible for providing IEntityDb objects
 * User: darri
 * Date: 9.5.2005
 * Time: 11:39:13
 */
public interface IEntityDbProvider {

  /**
   * Provides a IEntityDb object, is responsible for returning a
   * healthy IEntityDb object, that is, it must perform all
   * reconnections whether remotely or locally
   * @return IEntityDb
   * @throws UserException in case of a problem
   */
  public IEntityDb getEntityDb() throws UserException;

  /**
   * @return an Event fired when a successful connection is made
   */
  public Event getConnectEvent();
}
