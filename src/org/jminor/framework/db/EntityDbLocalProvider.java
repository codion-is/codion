/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.db.AuthenticationException;
import org.jminor.common.db.Database;
import org.jminor.common.db.User;
import org.jminor.common.model.Event;
import org.jminor.common.model.UserException;
import org.jminor.common.model.Util;

import org.apache.log4j.Logger;

/**
 * A class responsible for managing local db connections
 */
public class EntityDbLocalProvider implements IEntityDbProvider {

  private static final Logger log = Util.getLogger(EntityDbLocalProvider.class);

  /**
   * Fired when a successful connection has been made
   */
  public final Event evtConnected = new Event("EntityDbLocalProvider.evtConnected");

  /**
   * The user used by this db provider when connecting to the database server
   */
  protected final User user;

  /**
   * The IEntityDb instance used by this db provider
   */
  protected IEntityDb entityDb;

  public EntityDbLocalProvider(final User user) {
    this.user = user;
    final String sid = System.getProperty(Database.DATABASE_SID_PROPERTY);
    if (!Database.isDerbyEmbedded() && (sid == null || sid.length() == 0))
      throw new RuntimeException("Required property value not found: " + Database.DATABASE_SID_PROPERTY);
    if (sid != null)
      user.setProperty(Database.DATABASE_SID_PROPERTY, sid);
  }

  public final IEntityDb getEntityDb() throws UserException {
    validateDbConnection();

    return entityDb;
  }

  /** {@inheritDoc} */
  public Event getConnectEvent() {
    return evtConnected;
  }

  private void validateDbConnection() throws UserException {
    try {
      if (entityDb == null)
        connect();

      if (!entityDb.isConnectionValid()) {
        //db unreachable
        //try to reconnect once in case db has become reachable
        entityDb = null;
        connect();
      }
    }
    catch (Exception e) {
      throw new UserException(e);
    }
  }

  private void connect() throws ClassNotFoundException, AuthenticationException {
    log.debug("Initializing connection for " + user);
    entityDb = new EntityDbConnection(user);
    evtConnected.fire();
  }
}